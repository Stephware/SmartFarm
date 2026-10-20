package com.smartfarm.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.data.repository.AuthRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class OfflineCacheTest {
    @Test fun cacheIsAccountScopedAndDoesNotAffectPendingWrites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        val server = MockWebServer()
        server.start()
        try {
            val api = ApiClientFactory.create(
                server.url("/").toString(), allowLocalHttpForDebug = true
            ).create(AuthApi::class.java)
            val auth = AuthRepository { api }
            val cache = OfflineReadCache(auth, db.cachedResponseDao())
            val type = TypeToken.getParameterized(List::class.java, FarmDto::class.java).type

            login(server, auth, "account-a")
            val farm = FarmDto("farm-1", "Field Farm", "Davao", "Asia/Manila", role = "Owner")
            val live = cache.read("farms", type) { listOf(farm) }
            assertFalse(live.offline)
            val stored = cache.read<List<FarmDto>>("farms", type) {
                throw IOException("Disconnected")
            }
            assertTrue(stored.offline)
            assertEquals("Field Farm", stored.value.single().name)
            assertTrue(stored.savedAtUtcMillis > 0)

            db.pendingOperationDao().add(PendingOperation(
                operationId = "pending-a", userId = "account-a", farmId = "farm-1",
                operationType = "activity", payloadJson = "{}", createdAtUtc = 10L
            ))
            auth.logout()
            login(server, auth, "account-b")
            try {
                cache.read<List<FarmDto>>("farms", type) { throw IOException("Offline") }
                fail("Another account must never receive cached farm data")
            } catch (_: IOException) { }
            auth.logout()
            login(server, auth, "account-a")
            assertEquals("farm-1",
                cache.read<List<FarmDto>>("farms", type) {
                    throw IOException("Offline")
                }.value.single().id)
            cache.invalidate("farms")
            assertEquals(1, db.pendingOperationDao().pending("account-a").size)
            assertEquals(null, db.cachedResponseDao().find("account-a", "farms"))
        } finally {
            server.shutdown()
            db.close()
        }
    }

    @Test fun authorizationErrorsNeverReturnAnOldSnapshot() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        val server = MockWebServer()
        server.start()
        try {
            val api = ApiClientFactory.create(
                server.url("/").toString(), allowLocalHttpForDebug = true
            ).create(AuthApi::class.java)
            val auth = AuthRepository { api }
            login(server, auth, "account-a")
            val cache = OfflineReadCache(auth, db.cachedResponseDao())
            val type = TypeToken.getParameterized(List::class.java, FarmDto::class.java).type
            cache.read("farms", type) { emptyList<FarmDto>() }
            try {
                cache.read<List<FarmDto>>("farms", type) {
                    throw HttpException(Response.error<Any>(403, "Forbidden".toResponseBody()))
                }
                fail("403 must not fall back to cached data")
            } catch (error: HttpException) {
                assertEquals(403, error.code())
            }
        } finally {
            server.shutdown()
            db.close()
        }
    }

    private suspend fun login(server: MockWebServer, auth: AuthRepository, id: String) {
        server.enqueue(MockResponse().setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""{"tokenType":"Bearer","accessToken":"test","expiresIn":1800,"refreshToken":"refresh"}"""))
        server.enqueue(MockResponse().setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""{"id":"$id","fullName":"Test Farmer","email":"farmer@example.test"}"""))
        auth.login("farmer@example.test", "ExamplePassword1")
    }
}
