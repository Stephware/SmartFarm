package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.api.WeatherApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.WeatherRepository
import com.smartfarm.mobile.features.farms.FarmViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherOfflineStateTest {
    @get:Rule val compose = createComposeRule()

    @Test fun weatherOnlyFallbackDoesNotDisableOnlineFarmManagement() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        val server = MockWebServer()
        val userId = "d56b3ae2-08ac-4662-9d7f-9b9582bd76be"
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.path) {
                    "/api/auth/login" ->
                        """{"accessToken":"access","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}"""
                    "/api/auth/me" ->
                        """{"id":"$userId","fullName":"Test Farmer","email":"farmer@example.test"}"""
                    "/api/farms/farm" ->
                        """{"id":"farm","name":"North farm","locationText":"Location","timeZoneId":"Asia/Manila","role":"Owner","rowVersion":"version"}"""
                    "/api/farms/farm/plots", "/api/farms/farm/members" -> "[]"
                    "/api/farms/farm/weather" ->
                        return MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                    "/api/farms/farm/join-requests" ->
                        """{"joinCode":"EXAMPLE","requests":[],"history":[]}"""
                    else -> return MockResponse().setResponseCode(404)
                }
                return MockResponse().addHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            auth.login("farmer@example.test", "ExamplePassword1")
            val cache = OfflineReadCache(auth, db.cachedResponseDao())
            val savedAt = System.currentTimeMillis() - 60_000L
            db.cachedResponseDao().save(CachedResponse(
                userId, "farm/farm/weather",
                """{"farmId":"farm","hasCoordinates":true,"timeZoneId":"Asia/Manila","days":[],"advisories":[]}""",
                savedAt))
            val viewModel = FarmViewModel(
                FarmRepository(auth, cache) { retrofit.create(FarmApi::class.java) },
                WeatherRepository(auth, cache) { retrofit.create(WeatherApi::class.java) })
            compose.runOnIdle { viewModel.loadDetail("farm") }
            compose.waitUntil(10_000) { !viewModel.state.value.loading }
            val state = viewModel.state.value
            assertNotNull(state.selected)
            assertEquals("Owner", state.selected?.role)
            assertFalse("Live farm records must not be labelled offline", state.offline)
            assertTrue("Only weather should show an offline warning", state.weatherOffline)
            assertEquals(savedAt, state.weatherCachedAtUtcMillis)
            assertNotNull(state.weather)
            compose.runOnIdle { viewModel.clear() }
        } finally {
            server.shutdown()
            db.close()
        }
    }
}
