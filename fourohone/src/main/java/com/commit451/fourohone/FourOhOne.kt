package com.commit451.fourohone

import okhttp3.Response

internal object FourOhOne {

    fun responseCount(response: Response): Int {
        var theResponse = response
        var result = 1
        while (true) {
            val priorResponse = theResponse.priorResponse() ?: break
            theResponse = priorResponse
            result++
        }
        return result
    }
}