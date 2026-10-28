package com.smartfarm.mobile

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import com.smartfarm.mobile.data.remote.api.FarmApi
import retrofit2.HttpException
import org.junit.Assert.*
import org.junit.Test

class EnrollmentApiContractTest {
    @Test fun registrationSendsOnlyNameEmailAndPassword() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val auth = AuthRepository { ApiClientFactory.create(server.url("/").toString(), true).create(AuthApi::class.java) }
            server.enqueue(MockResponse().setResponseCode(204))
            auth.register(" Sam ", " sam@example.com ", "Password2026")
            val sent = server.takeRequest()
            assertEquals("/api/auth/register", sent.path)
            val json = JsonParser.parseString(sent.body.readUtf8()).asJsonObject
            assertEquals(setOf("fullName", "email", "password"), json.keySet())
            assertEquals("Sam", json.get("fullName").asString)
            assertEquals("sam@example.com", json.get("email").asString)
            assertEquals("Password2026", json.get("password").asString)
        } finally { server.shutdown() }
    }

    @Test fun registrationFailureIsNotAutomaticallyRetried() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val auth = AuthRepository { ApiClientFactory.create(server.url("/").toString(), true).create(AuthApi::class.java) }
            server.enqueue(MockResponse().setResponseCode(429))
            try { auth.register("Sam", "sam@example.com", "Password2026"); fail("Registration must fail") }
            catch (error: HttpException) { assertEquals(429, error.code()) }
            assertEquals(1, server.requestCount)
        } finally { server.shutdown() }
    }

    @Test fun enrollmentUsesStoredIdentityForDecisionsCancellationAndCodeRegeneration() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = ApiClientFactory.create(server.url("/").toString(), true).create(FarmApi::class.java)
            val request = """{"id":"req","farmId":"farm","farmName":"North farm","userId":"farmer","fullName":"Sam","email":"sam@example.com","status":"Pending","requestedAtUtc":"2026-10-27T01:00:00Z"}"""
            server.enqueue(MockResponse().setBody(request))
            assertEquals("Pending", api.requestToJoin("Bearer test", FarmJoinInputDto("ABCD12345678")).status)
            server.enqueue(MockResponse().setBody("[$request]"))
            assertEquals("North farm", api.myJoinRequests("Bearer test").single().farmName)
            server.enqueue(MockResponse().setBody("""{"joinCode":"ABCD12345678","requests":[$request],"history":[${request.replace("Pending", "Canceled") }]}"""))
            val overview = api.enrollment("Bearer test", "farm")
            assertEquals("sam@example.com", overview.requests.single().email)
            assertEquals("Canceled", overview.history.single().status)
            server.enqueue(MockResponse().setResponseCode(204))
            api.decideJoinRequest("Bearer test", "farm", "req", FarmJoinDecisionDto(true))
            server.enqueue(MockResponse().setResponseCode(204))
            api.cancelJoinRequest("Bearer test", "farm", "req")
            server.enqueue(MockResponse().setBody("""{"joinCode":"NEWCODE12345"}"""))
            assertEquals("NEWCODE12345", api.regenerateJoinCode("Bearer test", "farm", RegenerateJoinCodeDto("ABCD12345678")).joinCode)
            val requests = (1..6).map { server.takeRequest() }
            assertEquals(listOf("POST", "GET", "GET", "POST", "POST", "POST"), requests.map { it.method })
            assertEquals(listOf("/api/farm-join-requests", "/api/farm-join-requests", "/api/farms/farm/join-requests",
                "/api/farms/farm/join-requests/req/decision", "/api/farms/farm/join-requests/req/cancel",
                "/api/farms/farm/join-code/regenerate"), requests.map { it.path })
            assertEquals("""{"farmCode":"ABCD12345678"}""", requests[0].body.readUtf8())
            assertEquals("""{"approve":true}""", requests[3].body.readUtf8())
            assertEquals(0L, requests[4].bodySize)
            assertEquals("""{"currentCode":"ABCD12345678"}""", requests[5].body.readUtf8())
            requests.forEach { assertEquals("Bearer test", it.getHeader("Authorization")) }
        } finally { server.shutdown() }
    }

    @Test fun olderEnrollmentOverviewDefaultsToEmptyHistory() {
        val result = Gson().fromJson("""{"joinCode":"CODE","requests":[]}""", FarmEnrollmentOverviewDto::class.java)
        assertTrue(result.history.isEmpty())
    }

    @Test fun throttledAndConflictingEnrollmentMutationsAreNotRetried() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            server.enqueue(MockResponse().setBody("""{"accessToken":"test","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}"""))
            server.enqueue(MockResponse().setBody("""{"id":"user","fullName":"Sam","email":"sam@example.com"}"""))
            auth.login("sam@example.com", "Password2026")
            val farms = FarmRepository(auth) { retrofit.create(FarmApi::class.java) }
            server.enqueue(MockResponse().setResponseCode(429))
            try { farms.cancelJoinRequest("farm", "req"); fail("Cancel must fail") }
            catch (error: HttpException) { assertEquals(429, error.code()) }
            server.enqueue(MockResponse().setResponseCode(409))
            try { farms.regenerateJoinCode("farm", "OLD"); fail("Stale code must fail") }
            catch (error: HttpException) { assertEquals(409, error.code()) }
            assertEquals(4, server.requestCount)
        } finally { server.shutdown() }
    }
}
