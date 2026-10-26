package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.remote.api.*
import com.smartfarm.mobile.data.repository.*
import com.smartfarm.mobile.data.sync.LocalPhotoFiles
import com.smartfarm.mobile.data.sync.SyncRepository
import com.smartfarm.mobile.features.farms.FarmViewModel
import com.smartfarm.mobile.features.inventory.InventoryViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RetryRefreshTest {
    @get:Rule val compose = createComposeRule()

    @Test fun refreshingTheSameRecordKeepsItsIdentityAndDraftKeys() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val body = when (request.path) {
                    "/api/auth/login" -> """{"accessToken":"test","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}"""
                    "/api/auth/me" -> """{"id":"user","fullName":"Tester","email":"test@example.test"}"""
                    "/api/farms/farm" -> """{"id":"farm","name":"North farm","locationText":"Location","timeZoneId":"Asia/Manila","role":"Owner","rowVersion":"version"}"""
                    "/api/farms/farm/weather" -> """{"farmId":"farm","hasCoordinates":false,"timeZoneId":"Asia/Manila","days":[],"advisories":[]}"""
                    "/api/farms/farm/inventory/item" -> """{"id":"item","farmId":"farm","name":"Seeds","category":1,"unit":"kg","quantityOnHand":1,"isLowStock":false,"rowVersion":"version"}"""
                    else -> "[]"
                }
                return MockResponse().setBody(body).addHeader("Content-Type", "application/json")
            }
        }
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            auth.login("test@example.test", "Password2026")
            val farms = FarmViewModel(FarmRepository(auth) { retrofit.create(FarmApi::class.java) },
                WeatherRepository(auth, OfflineReadCache(auth, db.cachedResponseDao())) { retrofit.create(WeatherApi::class.java) })
            compose.runOnIdle { farms.loadDetail("farm") }
            compose.waitUntil(5000) { !farms.state.value.loading }
            assertEquals("farm", farms.state.value.selected?.id)
            compose.runOnIdle {
                farms.loadDetail("farm")
                assertEquals("farm", farms.state.value.selected?.id)
            }
            compose.waitUntil(5000) { !farms.state.value.loading }
            val sync = SyncRepository(auth, db.pendingOperationDao(), db.pendingPhotoDao(), LocalPhotoFiles(context),
                { retrofit.create(SyncApi::class.java) }, { retrofit.create(MonitoringApi::class.java) },
                { retrofit.create(InventoryApi::class.java) }, WorkManager.getInstance(context))
            val inventory = InventoryViewModel(InventoryRepository(auth) { retrofit.create(InventoryApi::class.java) }, sync)
            compose.runOnIdle { inventory.loadItem("farm", "item") }
            compose.waitUntil(5000) { !inventory.state.value.busy }
            assertEquals("item", inventory.state.value.item?.id)
            compose.runOnIdle {
                inventory.loadItem("farm", "item")
                assertEquals("item", inventory.state.value.item?.id)
            }
            compose.waitUntil(5000) { !inventory.state.value.busy }
            compose.runOnIdle { farms.clear(); inventory.clear() }
        } finally { server.shutdown(); db.close() }
    }
}
