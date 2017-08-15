package com.commit451.fourohone

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests for configuration of [FourOhOneAuthenticator]
 */
class ConfigurationTest {

    @Test
    fun ignore() {

        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        val fourOhOneAuthenticator = FourOhOneAuthenticator.Builder(object : FourOhOneAuthenticator.Callback {

            override fun onReauthenticate(route: Route, response: Response): Request? {
                throw RuntimeException("This is a failure")
            }

            override fun onUnableToAuthenticate(route: Route, response: Response) {
                throw RuntimeException("This is a failure")
            }
        })
                .build()

        val client = OkHttpClient.Builder()
                .authenticator(fourOhOneAuthenticator)
                .build()

        val request = Request.Builder()
                .url(server.url(""))
                .header(FourOhOneAuthenticator.HEADER_IGNORE, "true")
                .build()

        client.newCall(request).execute()

        server.shutdown()
    }

    @Test
    fun unableToAuth() {

        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        server.start()

        val countDownLatch = CountDownLatch(0)

        val fourOhOneAuthenticator = FourOhOneAuthenticator.Builder(object : FourOhOneAuthenticator.Callback {

            override fun onReauthenticate(route: Route, response: Response): Request? {
                //just keep retrying with same stuff
                return response.request()
            }

            override fun onUnableToAuthenticate(route: Route, response: Response) {
                countDownLatch.countDown()
            }
        })
                .build()
        val client = OkHttpClient.Builder()
                .authenticator(fourOhOneAuthenticator)
                .build()

        val request = Request.Builder()
                .url(server.url(""))
                .build()

        client.newCall(request).execute()

        countDownLatch.await(5, TimeUnit.SECONDS)

        server.shutdown()
    }
}