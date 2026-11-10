package com.smartfarm.mobile

import com.smartfarm.mobile.data.repository.NetworkErrorMessages
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NetworkErrorMessagesTest {
    private fun http(code: Int): HttpException = HttpException(
        Response.error<Any>(code, "Sensitive server details".toResponseBody("text/plain".toMediaType())))

    @Test fun permissionSessionConflictAndRateLimitHaveActionableMessages() {
        assertTrue(NetworkErrorMessages.forRequest(http(401), "fallback").contains("session"))
        assertTrue(NetworkErrorMessages.forRequest(http(403), "fallback").contains("permission"))
        assertTrue(NetworkErrorMessages.forRequest(http(409), "fallback").contains("Refresh"))
        assertTrue(NetworkErrorMessages.forRequest(http(429), "fallback").contains("wait"))
    }

    @Test fun errorsDoNotExposePrivateServerResponses() {
        val messages = (400..599).map { code ->
            NetworkErrorMessages.forRequest(http(code), "Could not complete the request.")
        }
        assertTrue(messages.none { it.contains("Sensitive server details") })
        assertEquals("Incorrect email or password.", NetworkErrorMessages.forLogin(http(401)))
        assertEquals("Check your account details and password requirements.",
            NetworkErrorMessages.forRegistration(http(400)))
    }

    @Test fun unconfirmedWritesMustNotRecommendBlindRetry() {
        val network = NetworkErrorMessages.forMutation(IOException("Socket timed out"), "fallback")
        val server = NetworkErrorMessages.forMutation(http(503), "fallback")
        assertTrue(network.contains("Refresh the records"))
        assertTrue(server.contains("Refresh the records"))
        assertFalse(network.contains("Try again later"))
    }
}
