package com.commit451.fourohone.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.commit451.fourohone.FourOhOneAuthenticator;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;


/**
 * Some simple client. Use something like retrofit to get a similar effect. Also mocks the server
 */
public class Client {

    public static final String PATH_LOGIN = "login";
    public static final String PATH_RESOURCE = "resource";
    public static final String PATH_OTHER_RESOURCE = "other_resource";

    public static final String HEADER_AUTHENTICATION = "Authentication";

    private OkHttpClient client;
    private String token;
    private MockWebServer server;

    public Client(final Context context) {

        server = new MockWebServer();
        final okhttp3.mockwebserver.Dispatcher dispatcher = new okhttp3.mockwebserver.Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getPath().equals(PATH_LOGIN)) {
                    return new MockResponse().setBody(UUID.randomUUID().toString()).setResponseCode(200);
                } else if (request.getPath().equals(PATH_RESOURCE)) {
                    if (request.getHeader(HEADER_AUTHENTICATION) != null) {
                        return new MockResponse().setResponseCode(200);
                    } else {
                        return new MockResponse().setResponseCode(401);
                    }
                } else if (request.getPath().equals(PATH_OTHER_RESOURCE)) {
                    return new MockResponse().setResponseCode(401);
                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);

        FourOhOneAuthenticator fourOhOneAuthenticator = new FourOhOneAuthenticator.Builder(new FourOhOneAuthenticator.Callback() {
            @Nullable
            @Override
            public Request onReauthenticate(Route route, Response response) {
                Log.e("FourOhOneAuthenticator", "Got a 401, retrying after logging in");
                //don't worry, already on a background thread
                String token = login().blockingGet();
                //don't forget to set it on the client, so that future calls do not fail
                Client.this.token = token;
                Request.Builder requestBuilder = response.request().newBuilder();
                addAuthentication(requestBuilder, token);
                return requestBuilder.build();
            }

            @Override
            public void onUnableToAuthenticate(Route route, Response response) {
                //gotta post this on the main thread
                Log.e("FourOhOneAuthenticator", "Unable to satisfy login. Relaunching user to login");
                postOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "You have been signed out", Toast.LENGTH_SHORT)
                                .show();

                        //relaunch login screen, clearing the stack
                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                    }
                });

            }
        })
                .retryCount(4)
                .build();
        client = new OkHttpClient.Builder()
                .authenticator(fourOhOneAuthenticator)
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        if (token != null) {
                            Request.Builder requestBuilder = chain.request().newBuilder();
                            addAuthentication(requestBuilder, token);
                            return chain.proceed(requestBuilder.build());
                        }
                        return chain.proceed(chain.request());
                    }
                })
                .build();
    }

    /**
     * Simulate logging in
     *
     * @return an token token
     */
    public Single<String> login() {
        return Single.defer(new Callable<SingleSource<? extends String>>() {
            @Override
            public SingleSource<? extends String> call() throws Exception {
                Request request = new Request.Builder()
                        .url(server.url(PATH_LOGIN))
                        .build();
                String response = client.newCall(request).execute().body().string();
                return Single.just(response);
            }
        });
    }

    /**
     * Simulate fetching some authenticated resource
     *
     * @return some random string
     */
    public Single<String> getResource() {
        return Single.defer(new Callable<SingleSource<? extends String>>() {
            @Override
            public SingleSource<? extends String> call() throws Exception {
                Request request = new Request.Builder()
                        .url(server.url(PATH_RESOURCE))
                        .build();
                String response = client.newCall(request).execute().body().string();
                return Single.just(response);
            }
        });
    }

    /**
     * Simulate fetching some authenticated resource but never succeed
     *
     * @return some random string
     */
    public Single<String> getResourceAndFail() {
        return Single.defer(new Callable<SingleSource<? extends String>>() {
            @Override
            public SingleSource<? extends String> call() throws Exception {
                Request request = new Request.Builder()
                        .url(server.url(PATH_OTHER_RESOURCE))
                        .build();
                String response = client.newCall(request).execute().body().string();
                return Single.just(response);
            }
        });
    }

    public void setToken(String token) {
        this.token = token;
    }

    private void addAuthentication(Request.Builder requestBuilder, String token) {
        requestBuilder.header(HEADER_AUTHENTICATION, token);
    }

    private void postOnMainThread(Runnable runnable) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(runnable);
    }
}
