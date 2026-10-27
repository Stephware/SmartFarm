package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.remote.api.*
import com.smartfarm.mobile.data.repository.*
import com.smartfarm.mobile.features.farms.FarmViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.TimeUnit

class EnrollmentStateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enrollmentFailureKeepsFarmsAndRefreshClearsOwnerDataForOtherRoles() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        val server = MockWebServer()
        var role = "Owner"
        var enrollmentUnavailable = false
        val ownerCalls = AtomicInteger()
        val detailDelay = AtomicLong()
        val detailCalls = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/api/farms/farm") detailCalls.incrementAndGet()
                val body = when (request.path) {
                    "/api/auth/login" -> """{"accessToken":"test","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}"""
                    "/api/auth/me" -> """{"id":"user","fullName":"Tester","email":"test@example.test"}"""
                    "/api/farms" -> """[{"id":"farm","name":"North farm","locationText":"Location","timeZoneId":"Asia/Manila","role":"$role"}]"""
                    "/api/farms/farm" -> """{"id":"farm","name":"North farm","locationText":"Location","timeZoneId":"Asia/Manila","role":"$role"}"""
                    "/api/farms/farm/weather" -> """{"farmId":"farm","hasCoordinates":false,"timeZoneId":"Asia/Manila","days":[],"advisories":[]}"""
                    "/api/farm-join-requests" -> return MockResponse().setResponseCode(503)
                    "/api/farms/farm/join-requests" -> {
                        ownerCalls.incrementAndGet()
                        if (enrollmentUnavailable) return MockResponse().setResponseCode(503)
                        """{"joinCode":"ABCD12345678","requests":[{"id":"req","farmId":"farm","farmName":"North farm","userId":"farmer","fullName":"Sam","email":"private@example.com","status":"Pending","requestedAtUtc":"2026-10-27T01:00:00Z"}]}"""
                    }
                    else -> "[]"
                }
                return MockResponse().setBody(body).addHeader("Content-Type", "application/json")
                    .setBodyDelay(if (request.path == "/api/farms/farm") detailDelay.get() else 0, TimeUnit.MILLISECONDS)
            }
        }
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            auth.login("test@example.test", "Password2026")
            val cache = OfflineReadCache(auth, db.cachedResponseDao())
            val farms = FarmViewModel(FarmRepository(auth, cache) { retrofit.create(FarmApi::class.java) },
                WeatherRepository(auth, cache) { retrofit.create(WeatherApi::class.java) })
            compose.runOnIdle { farms.load() }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertEquals("North farm", farms.state.value.farms.single().name)
            assertNull(farms.state.value.error)
            assertNotNull(farms.state.value.enrollmentMessage)
            compose.runOnIdle { farms.loadDetail("farm") }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertEquals("private@example.com", farms.state.value.enrollment?.requests?.single()?.email)
            val cachedFarm = db.cachedResponseDao().find("user", "farm/farm")!!
            assertFalse(cachedFarm.json.contains("ABCD12345678"))
            assertFalse(cachedFarm.json.contains("private@example.com"))
            assertNull(db.cachedResponseDao().find("user", "farm/farm/join-requests"))

            role = "Manager"
            compose.runOnIdle {
                farms.loadDetail("farm")
                assertNull(farms.state.value.enrollment)
            }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertEquals("Manager", farms.state.value.selected?.role)
            assertNull(farms.state.value.enrollment)
            assertEquals(1, ownerCalls.get())

            role = "Owner"
            enrollmentUnavailable = true
            compose.runOnIdle { farms.loadDetail("farm") }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertEquals("farm", farms.state.value.selected?.id)
            assertNull(farms.state.value.error)
            assertNull(farms.state.value.enrollment)
            assertNotNull(farms.state.value.enrollmentMessage)

            enrollmentUnavailable = false
            compose.runOnIdle { farms.loadDetail("farm") }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertNotNull(farms.state.value.enrollment)
            // A cancelled refresh must not clear the replacement refresh's busy state.
            role = "Manager"
            detailDelay.set(400)
            val beforeRefresh = detailCalls.get()
            compose.runOnIdle { farms.loadDetail("farm") }
            compose.waitUntil(5000) { detailCalls.get() > beforeRefresh }
            compose.runOnIdle { farms.loadDetail("farm") }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertEquals("Manager", farms.state.value.selected?.role)
            assertNull(farms.state.value.enrollment)
            compose.runOnIdle { auth.logout(); farms.clear() }
            assertNull(farms.state.value.enrollment)
            assertTrue(farms.state.value.myRequests.isEmpty())
            assertNull(farms.state.value.selected)
        } finally { server.shutdown(); db.close() }
    }
}
