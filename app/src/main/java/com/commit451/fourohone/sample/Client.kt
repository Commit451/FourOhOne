package com.commit451.fourohone.sample

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.commit451.fourohone.FourOhOneAuthenticator
import io.reactivex.Single
import okhttp3.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.*


/**
 * Some simple client. Use something like retrofit to get a similar effect. Also mocks the server
 */
class Client(context: Context) {

    companion object {

        val PATH_LOGIN = "login"
        val PATH_RESOURCE = "resource"
        val PATH_OTHER_RESOURCE = "other_resource"

        val HEADER_AUTHENTICATION = "Authentication"
    }

    private val client: OkHttpClient
    private var token: String? = null
    private val server: MockWebServer = MockWebServer()

    init {

        val dispatcher = object : okhttp3.mockwebserver.Dispatcher() {

            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == PATH_LOGIN) {
                    return MockResponse().setBody(UUID.randomUUID().toString()).setResponseCode(200)
                } else if (request.path == PATH_RESOURCE) {
                    if (request.getHeader(HEADER_AUTHENTICATION) != null) {
                        return MockResponse().setResponseCode(200)
                    } else {
                        return MockResponse().setResponseCode(401)
                    }
                } else if (request.path == PATH_OTHER_RESOURCE) {
                    return MockResponse().setResponseCode(401)
                }
                return MockResponse().setResponseCode(404)
            }
        }
        server.setDispatcher(dispatcher)

        val fourOhOneAuthenticator = FourOhOneAuthenticator.Builder(object : FourOhOneAuthenticator.Callback {
            override fun onReauthenticate(route: Route, response: Response): Request? {
                Log.e("FourOhOneAuthenticator", "Got a 401, retrying after logging in")
                //don't worry, already on a background thread
                val token = login().blockingGet()
                //don't forget to set it on the client, so that future calls do not fail
                this@Client.token = token
                val requestBuilder = response.request().newBuilder()
                addAuthentication(requestBuilder, token)
                return requestBuilder.build()
            }

            override fun onUnableToAuthenticate(route: Route, response: Response) {
                //gotta post this on the main thread
                Log.e("FourOhOneAuthenticator", "Unable to satisfy login. Relaunching user to login")
                postOnMainThread(Runnable {
                    Toast.makeText(context, "You have been signed out", Toast.LENGTH_SHORT)
                            .show()

                    //relaunch login screen, clearing the stack
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                })

            }
        })
                .retryCount(4)
                .build()
        client = OkHttpClient.Builder()
                .authenticator(fourOhOneAuthenticator)
                .addInterceptor(Interceptor { chain ->
                    val token = token
                    if (token != null) {
                        val requestBuilder = chain.request().newBuilder()
                        addAuthentication(requestBuilder, token)
                        return@Interceptor chain.proceed(requestBuilder.build())
                    }
                    chain.proceed(chain.request())
                })
                .build()
    }

    /**
     * Simulate logging in

     * @return an token token
     */
    fun login(): Single<String> {
        return Single.defer {
            val request = Request.Builder()
                    .url(server.url(PATH_LOGIN))
                    .build()
            val response = client.newCall(request).execute().body()!!.string()
            Single.just(response)
        }
    }

    /**
     * Simulate fetching some authenticated resource

     * @return some random string
     */
    fun getResource(): Single<String> {
        return Single.defer {
            val request = Request.Builder()
                    .url(server.url(PATH_RESOURCE))
                    .build()
            val response = client.newCall(request).execute().body()!!.string()
            Single.just(response)
        }
    }

    /**
     * Simulate fetching some authenticated resource but never succeed

     * @return some random string
     */
    fun getResourceAndFail(): Single<String> {
        return Single.defer {
            val request = Request.Builder()
                    .url(server.url(PATH_OTHER_RESOURCE))
                    .build()
            val response = client.newCall(request).execute().body()!!.string()
            Single.just(response)
        }
    }

    fun setToken(token: String?) {
        this.token = token
    }

    private fun addAuthentication(requestBuilder: Request.Builder, token: String) {
        requestBuilder.header(HEADER_AUTHENTICATION, token)
    }

    private fun postOnMainThread(runnable: Runnable) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(runnable)
    }
}
