package com.commit451.fourohone

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Re-authentication via an Authenticator
 */
class FourOhOneAuthenticator private constructor(builder: Builder) : Authenticator {

    companion object {

        private val DEFAULT_RETRY_COUNT = 3

        /**
         * Add this header on any requests that you want to be ignored by the [FourOhOneAuthenticator]. Value of the header is ignored
         */
        val HEADER_IGNORE = "FourOhOneAuthenticatorIgnore"
    }

    private var ignoreHeader: String = builder.ignoreHeader
    private var callback: Callback = builder.callback
    private var retryCount: Int = builder.retryCount

    override fun authenticate(route: Route, response: Response): Request? {
        if (response.request().header(ignoreHeader) != null) {
            //ignore this call (probably login)
            return null
        }

        if (FourOhOne.responseCount(response) >= retryCount) {
            callback.onUnableToAuthenticate(route, response)
            return null
        }

        val request =  callback.onReauthenticate(route, response)
        if (request == null) {
            callback.onUnableToAuthenticate(route, response)
            return null
        } else {
            return request
        }
    }

    /**
     * Builder for [FourOhOneAuthenticator]
     */
    class Builder
    /**
     * New builder for [FourOhOneAuthenticator] with the required [Callback]
     * @param callback callback
     */
    (internal val callback: Callback) {
        internal var retryCount = DEFAULT_RETRY_COUNT
        internal var ignoreHeader = HEADER_IGNORE

        /**
         * The number of times to retry the call before calling [Callback.onUnableToAuthenticate]
         * @param retryCount number of retries. Defaults to 3
         * @return builder
         */
        fun retryCount(retryCount: Int): Builder {
            this.retryCount = retryCount
            return this
        }

        /**
         * Calls that contain this header will be ignored by [FourOhOneAuthenticator]. This is useful for ignoring expected 401 calls, such as login with incorrect credentials. Value of the header is ignored
         * @param ignoreHeader the name of the header
         * @return builder
         */
        fun ignoreHeader(ignoreHeader: String): Builder {
            this.ignoreHeader = ignoreHeader
            return this
        }

        /**
         * Build the [FourOhOneAuthenticator]
         * @return the newly built authenticator
         */
        fun build(): FourOhOneAuthenticator {
            return FourOhOneAuthenticator(this)
        }
    }

    /**
     * Callbacks that [FourOhOneAuthenticator] calls to coordinate reauth and failed auth
     */
    interface Callback {

        /**
         * A 401 error has occurred, and an attempt should be made to reauthenticate with the server.
         * Note that this is called on a background thread
         * @param route the route of the 401 failure
         * @param response the response of the 401 failure
         * @return a built request that has the correct authentication. Otherwise, null if re-authentication is not possible
         */
        fun onReauthenticate(route: Route, response: Response): Request?

        /**
         * Authentication has been attempted the max number of times, and seems to be impossible. This is a good time to clear credentials, and prompt the user to sign in again.
         * Note that this is called on a background thread
         * @param route the route of the 401 failure
         * @param response the response of the 401 failure
         */
        fun onUnableToAuthenticate(route: Route, response: Response)
    }
}
