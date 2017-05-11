package com.commit451.fourohone;

import android.support.annotation.Nullable;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * Re-authentication
 */
public class FourOhOneAuthenticator implements Authenticator {

    private static final int DEFAULT_RETRY_COUNT = 3;

    /**
     * Add this header on any requests that you want to be ignored by the {@link FourOhOneAuthenticator}. Value of the header is ignored
     */
    public static final String HEADER_IGNORE = "FourOhOneAuthenticatorIgnore";

    private String ignoreHeader;
    private Callback callback;
    private int retryCount;

    private FourOhOneAuthenticator() {
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (response.request().header(ignoreHeader) != null) {
            //ignore this call (probably login)
            return null;
        }

        if (responseCount(response) >= retryCount) {
            callback.onUnableToAuthenticate(route, response);
            return null;
        }

        Request request = callback.onReauthenticate(route, response);
        if (request == null) {
            return null;
        } else {
            return request;
        }
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    /**
     * Builder for {@link FourOhOneAuthenticator}
     */
    public static class Builder {

        private Callback callback;
        private int retryCount = DEFAULT_RETRY_COUNT;
        private String ignoreHeader = HEADER_IGNORE;

        /**
         * New builder for {@link FourOhOneAuthenticator} with the required {@link Callback}
         * @param callback callback
         */
        public Builder(Callback callback) {
            this.callback = callback;
        }

        /**
         * The number of times to retry the call before calling {@link Callback#onUnableToAuthenticate(Route, Response)}
         * @param retryCount number of retries. Defaults to 3
         * @return builder
         */
        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        /**
         * Calls that contain this header will be ignored by {@link FourOhOneAuthenticator}. This is useful for ignoring expected 401 calls, such as login with incorrect credentials. Value of the header is ignored
         * @param ignoreHeader the name of the header
         * @return builder
         */
        public Builder ignoreHeader(String ignoreHeader) {
            this.ignoreHeader = ignoreHeader;
            return this;
        }

        /**
         * Build the {@link FourOhOneAuthenticator}
         * @return the newly built authenticator
         */
        public FourOhOneAuthenticator build() {
            FourOhOneAuthenticator authenticator = new FourOhOneAuthenticator();
            authenticator.callback = callback;
            authenticator.retryCount = retryCount;
            authenticator.ignoreHeader = ignoreHeader;
            return authenticator;
        }
    }

    /**
     * Callbacks that {@link FourOhOneAuthenticator} calls to coordinate reauth and failed auth
     */
    public interface Callback {

        /**
         * A 401 error has occurred, and an attempt should be made to reauthenticate with the server.
         * Note that this is called on a background thread
         * @param route the route of the 401 failure
         * @param response the response of the 401 failure
         * @return a built request that has the correct authentication. Otherwise, null if reauthentication is not possible
         */
        @Nullable
        Request onReauthenticate(Route route, Response response);

        /**
         * Authentication has been attempted the max number of times, and seems to be impossible. This is a good time to clear credentials, and prompt the user to sign in again.
         * Note that this is called on a background thread
         * @param route the route of the 401 failure
         * @param response the response of the 401 failure
         */
        void onUnableToAuthenticate(Route route, Response response);
    }
}
