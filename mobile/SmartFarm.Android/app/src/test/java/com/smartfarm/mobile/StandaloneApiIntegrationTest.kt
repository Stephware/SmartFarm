package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.api.StatusApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests the same routes, JSON responses, and bearer headers used by SmartFarm.Api.
class StandaloneApiIntegrationTest {
    @Test fun checksStatusAndLoadsFarmsWithBearerToken() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(jsonResponse("""{"app":"SmartFarm","status":"ok"}"""))
            server.enqueue(jsonResponse(tokens("first-access", "first-refresh")))
            server.enqueue(jsonResponse(account()))
            server.enqueue(jsonResponse("[]"))

            val retrofit = ApiClientFactory.create(
                server.url("/").toString(), allowLocalHttpForDebug = true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            val farms = FarmRepository(auth) { retrofit.create(FarmApi::class.java) }

            assertTrue(retrofit.create(StatusApi::class.java).check().status == "ok")
            auth.login("farmer@example.test", "ExamplePassword1")
            assertTrue(farms.list().isEmpty())

            val statusCall = server.takeRequest()
            val loginCall = server.takeRequest()
            val accountCall = server.takeRequest()
            val farmsCall = server.takeRequest()
            assertEquals("/api/status", statusCall.path)
            assertEquals("/api/auth/login", loginCall.path)
            assertEquals("/api/auth/me", accountCall.path)
            assertEquals("/api/farms", farmsCall.path)
            assertEquals("Bearer first-access", accountCall.getHeader("Authorization"))
            assertEquals("Bearer first-access", farmsCall.getHeader("Authorization"))
            assertEquals("POST", loginCall.method)
            assertFalse(loginCall.body.readUtf8().contains("first-access"))
        } finally {
            server.shutdown()
        }
    }

    @Test fun refreshesExpiredTokenAndRetriesFarmRequest() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(jsonResponse(tokens("old-access", "old-refresh")))
            server.enqueue(jsonResponse(account()))
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(jsonResponse(tokens("new-access", "new-refresh")))
            server.enqueue(jsonResponse("[]"))

            val retrofit = ApiClientFactory.create(
                server.url("/").toString(), allowLocalHttpForDebug = true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            val farms = FarmRepository(auth) { retrofit.create(FarmApi::class.java) }

            auth.login("farmer@example.test", "ExamplePassword1")
            assertTrue(farms.list().isEmpty())

            server.takeRequest() // login
            server.takeRequest() // account
            val expired = server.takeRequest()
            val refresh = server.takeRequest()
            val retry = server.takeRequest()
            assertEquals("/api/farms", expired.path)
            assertEquals("Bearer old-access", expired.getHeader("Authorization"))
            assertEquals("/api/auth/refresh", refresh.path)
            assertEquals("POST", refresh.method)
            assertTrue(refresh.body.readUtf8().contains("old-refresh"))
            assertEquals("/api/farms", retry.path)
            assertEquals("Bearer new-access", retry.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }

    private fun jsonResponse(body: String) =
        MockResponse().setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody(body)

    private fun tokens(access: String, refresh: String) =
        """{"tokenType":"Bearer","accessToken":"$access","expiresIn":1800,"refreshToken":"$refresh"}"""

    private fun account() =
        """{"id":"d56b3ae2-08ac-4662-9d7f-9b9582bd76be","fullName":"Test Farmer","email":"farmer@example.test"}"""
}
