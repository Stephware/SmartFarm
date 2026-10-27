package com.smartfarm.mobile

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.MockResponse
import com.smartfarm.mobile.data.remote.api.FarmApi
import retrofit2.http.GET
import org.junit.Assert.*
import org.junit.Test

class EnrollmentApiContractTest {
    @Test fun invalidCodeRegistrationKeepsFieldErrorFromTheApi() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            server.enqueue(MockResponse().setResponseCode(400).setBody(
                """{"errors":["Farm code is invalid."],"farmCodeError":"Farm code is invalid."}"""))
            try {
                auth.register("Sam", "sam@example.com", "Password2026", "Password2026", true, "BADCODE")
                fail("Registration should fail")
            } catch (error: Exception) {
                assertEquals("Farm code is invalid.", error.message)
            }
        } finally { server.shutdown() }
    }

    @Test fun joiningRegistrationSendsCodeAndOwnerChoiceDropsIt() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            server.enqueue(MockResponse().setResponseCode(204))
            auth.register(" Sam ", " sam@example.com ", "Password2026", "Password2026", true, " abcd-1234-5678 ")
            val joining = server.takeRequest()
            assertEquals("/api/auth/register", joining.path)
            val json = JsonParser.parseString(joining.body.readUtf8()).asJsonObject
            assertTrue(json.get("joinFarm").asBoolean)
            assertEquals("abcd-1234-5678", json.get("farmCode").asString)
            server.enqueue(MockResponse().setResponseCode(204))
            auth.register("Sam", "sam@example.com", "Password2026", "Password2026", false, "stale-code")
            val owner = JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
            assertFalse(owner.get("joinFarm").asBoolean)
            assertFalse(owner.has("farmCode"))
        } finally { server.shutdown() }
    }

    @Test fun joiningRequiresCodeBeforeRegistrationIsSent() = runBlocking {
        val auth = AuthRepository { error("Blank code must never reach the API") }
        try {
            auth.register("Sam", "sam@example.com", "Password2026", "Password2026", true, " ")
            fail("A code is required")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun joinRequestAndOwnerDecisionUseStoredRequestIdentityAndNoRole() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = ApiClientFactory.create(server.url("/").toString(), true).create(FarmApi::class.java)
            val request = """{"id":"req","farmId":"farm","farmName":"North farm","userId":"farmer","fullName":"Sam","email":"sam@example.com","status":"Pending","requestedAtUtc":"2026-10-27T01:00:00Z","reviewedAtUtc":null}"""
            server.enqueue(MockResponse().setBody(request))
            assertEquals("Pending", api.requestToJoin("Bearer test", FarmJoinInputDto("ABCD12345678")).status)
            server.enqueue(MockResponse().setBody("[$request]"))
            assertEquals("North farm", api.myJoinRequests("Bearer test").single().farmName)
            server.enqueue(MockResponse().setBody("""{"joinCode":"ABCD12345678","requests":[$request]}"""))
            assertEquals("sam@example.com", api.enrollment("Bearer test", "farm").requests.single().email)
            server.enqueue(MockResponse().setResponseCode(204))
            api.decideJoinRequest("Bearer test", "farm", "req", FarmJoinDecisionDto(true))
            val submit = server.takeRequest()
            assertEquals("POST", submit.method)
            assertEquals("/api/farm-join-requests", submit.path)
            assertEquals("""{"farmCode":"ABCD12345678"}""", submit.body.readUtf8())
            val mine = server.takeRequest()
            assertEquals("GET", mine.method)
            assertEquals("/api/farm-join-requests", mine.path)
            val owner = server.takeRequest()
            assertEquals("/api/farms/farm/join-requests", owner.path)
            val decision = server.takeRequest()
            assertEquals("POST", decision.method)
            assertEquals("/api/farms/farm/join-requests/req/decision", decision.path)
            assertEquals("""{"approve":true}""", decision.body.readUtf8())
            listOf(submit, mine, owner, decision).forEach { assertEquals("Bearer test", it.getHeader("Authorization")) }
        } finally { server.shutdown() }
    }

    @Test fun enrollmentRoutesReplaceTokenAcceptance() {
        val paths = FarmApi::class.java.methods.mapNotNull { it.getAnnotation(GET::class.java)?.value }
        assertTrue("Caller needs their request statuses", paths.contains("api/farm-join-requests"))
        assertTrue("Owner needs approval overview", paths.contains("api/farms/{farmId}/join-requests"))
    }

    @Test fun ownerRegistrationExplicitlyKeepsOwnerOnboarding() {
        val json = JsonParser.parseString(Gson().toJson(RegisterRequest("Sam", "sam@example.com", "Password2026", "Password2026"))).asJsonObject
        assertTrue("Registration must send an explicit onboarding choice", json.has("joinFarm"))
        assertFalse(json.get("joinFarm").asBoolean)
    }
}
