package com.commit451.fourohone;

import android.support.annotation.Nullable;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Tests for configuration of {@link FourOhOneAuthenticator}
 */
public class ConfigurationTest {

    @Test
    public void ignore() throws Exception {

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(401));
        server.start();

        FourOhOneAuthenticator fourOhOneAuthenticator = new FourOhOneAuthenticator.Builder(new FourOhOneAuthenticator.Callback() {
            @Nullable
            @Override
            public Request onReauthenticate(Route route, Response response) {
                throw new RuntimeException("This is a failure");
            }

            @Override
            public void onUnableToAuthenticate(Route route, Response response) {
                throw new RuntimeException("This is a failure");
            }
        })
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator(fourOhOneAuthenticator)
                .build();

        Request request = new Request.Builder()
                .url(server.url(""))
                .header(FourOhOneAuthenticator.HEADER_IGNORE, "true")
                .build();

        client.newCall(request).execute();

        server.shutdown();
    }

    @Test
    public void unableToAuth() throws Exception {

        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(new MockResponse().setResponseCode(401));
        server.start();

        final CountDownLatch countDownLatch = new CountDownLatch(0);

        FourOhOneAuthenticator fourOhOneAuthenticator = new FourOhOneAuthenticator.Builder(new FourOhOneAuthenticator.Callback() {
            @Nullable
            @Override
            public Request onReauthenticate(Route route, Response response) {
                //just keep retrying with same stuff
                return response.request();
            }

            @Override
            public void onUnableToAuthenticate(Route route, Response response) {
                countDownLatch.countDown();
            }
        })
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .authenticator(fourOhOneAuthenticator)
                .build();

        Request request = new Request.Builder()
                .url(server.url(""))
                .build();

        client.newCall(request).execute();

        countDownLatch.await(5, TimeUnit.SECONDS);

        server.shutdown();
    }
}