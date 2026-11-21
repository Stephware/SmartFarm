package com.smartfarm.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.google.gson.Gson
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.remote.api.*
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.remote.dto.SyncWriteRequest
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.sync.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.TimeUnit

class SyncConfirmationTest {
    @Test fun harvestSummaryNeverRequestsRetiredFinanceData() = runBlocking {
        fixture { (_, server, _, auth, _, _) ->
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"farmId":"farm","farmName":"Farm","timeZoneId":"Asia/Manila","fromDate":null,"toDate":null,"validHarvestCount":2,"voidedHarvestCount":1,"unitTotals":[{"unit":"kg","quantity":2.5,"harvestCount":2}],"cropPlotTotals":[],"monthlyTotals":[]}"""))
            // Any obsolete extra fetch completes, so the regression fails on its count.
            server.enqueue(MockResponse().setResponseCode(410))
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val vm = com.smartfarm.mobile.features.insights.FarmInsightsViewModel(
                com.smartfarm.mobile.data.repository.FarmInsightsRepository(auth) {
                    retrofit.create(FarmInsightsApi::class.java)
                })
            try {
                withContext(Dispatchers.Main) { vm.load("farm", "2026-11-01", "2026-11-14") }
                withTimeout(5000) { while (!vm.state.value.loaded) delay(10) }
                assertEquals(BigDecimal("2.5"), vm.state.value.harvest?.unitTotals?.single()?.quantity)
                assertEquals("/api/farms/farm/reports/harvests?from=2026-11-01&to=2026-11-14",
                    server.takeRequest(2, TimeUnit.SECONDS)?.path)
                assertEquals("Only the confirmed harvest report may be requested", 3, server.requestCount)
            } finally { withContext(Dispatchers.Main) { vm.clear() } }
        }
    }

    @Test fun confirmedInvalidationPreventsOlderReadFromRestoringExactOrPrefixCache() = runBlocking {
        fixture { (db, server, sync, _, _, cache) -> coroutineScope {
            val staleKeys = mutableListOf<String>()
            for (type in listOf("harvest", "inventory")) {
                val target = UUID.randomUUID().toString()
                val key = if (type == "harvest") "farm/farm/cycle/$target/harvests" else "farm/farm/inventory/$target/history"
                val started = kotlinx.coroutines.CompletableDeferred<Unit>()
                val release = kotlinx.coroutines.CompletableDeferred<Unit>()
                val read = async(Dispatchers.IO) { cache.read<String>(key, String::class.java) {
                    started.complete(Unit); release.await(); "old snapshot"
                } }
                started.await()
                val otherStarted = kotlinx.coroutines.CompletableDeferred<Unit>()
                val otherKey = "farm/another/$target"
                val unrelatedRead = async(Dispatchers.IO) { cache.read<String>(otherKey, String::class.java) {
                    otherStarted.complete(Unit); release.await(); "unrelated fresh snapshot"
                } }
                otherStarted.await()
                val id = UUID.randomUUID().toString()
                val input = if (type == "harvest") SyncWriteRequest("farm", target, type,
                    harvest = RecordHarvestDto(BigDecimal.ONE, null)) else SyncWriteRequest("farm", target, type,
                    movement = com.smartfarm.mobile.data.remote.dto.InventoryMovementDto(2, BigDecimal.ONE, null, null, "old"))
                db.pendingOperationDao().add(PendingOperation(id, "user", "farm", type, Gson().toJson(input), 1))
                server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
                    """{"operationId":"$id","operationType":"$type","result":{},"replayed":false}"""))
                assertTrue(sync.uploadPending())
                release.complete(Unit); read.await(); unrelatedRead.await()
                assertNotNull("Unrelated farm reads must still be cached", db.cachedResponseDao().find("user", otherKey))
                if (db.cachedResponseDao().find("user", key) != null) staleKeys.add(type)
            }
            assertEquals("Older reads must not undo confirmed exact/prefix invalidation", emptyList<String>(), staleKeys)
        } }
    }

    @Test fun revokedAccessClearsRetainedSnapshotsAndRecordingPermissions() = runBlocking {
        fixture { (_, server, sync, auth, _, cache) ->
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val inventoryRepo = com.smartfarm.mobile.data.repository.InventoryRepository(auth, cache) { retrofit.create(InventoryApi::class.java) }
            val inventory = com.smartfarm.mobile.features.inventory.InventoryViewModel(inventoryRepo, sync)
            val harvest = com.smartfarm.mobile.features.harvests.HarvestViewModel(
                com.smartfarm.mobile.data.repository.HarvestRepository(auth, cache) { retrofit.create(HarvestApi::class.java) }, sync)
            val monitor = com.smartfarm.mobile.features.monitoring.MonitoringViewModel(
                com.smartfarm.mobile.data.repository.MonitoringRepository(auth, cache) { retrofit.create(MonitoringApi::class.java) }, sync, inventoryRepo)
            fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
            server.enqueue(json("""{"farmId":"farm","cycleId":"crop","cropName":"Rice","plotName":"Plot","unit":"kg","cropStatus":1,"canRecord":true,"canVoid":true,"totalQuantity":2,"validHarvestCount":1,"voidedHarvestCount":0,"entries":[]}"""))
            withContext(Dispatchers.Main) { harvest.load("farm", "crop") }
            withTimeout(5000) { harvest.state.first { !it.busy && it.overview != null } }
            server.enqueue(MockResponse().setResponseCode(403))
            withContext(Dispatchers.Main) { harvest.load("farm", "crop") }
            withTimeout(5000) { harvest.state.first { !it.busy } }
            val harvestRetained = harvest.state.value.overview != null
            server.enqueue(json("""{"items":[{"id":"item","farmId":"farm","name":"Private stock","category":1,"unit":"kg","quantityOnHand":2,"rowVersion":"v"}],"lowStockCount":0}"""))
            withContext(Dispatchers.Main) { inventory.load("farm") }
            withTimeout(5000) { inventory.state.first { !it.busy && it.workspace.items.isNotEmpty() } }
            server.enqueue(MockResponse().setResponseCode(404))
            withContext(Dispatchers.Main) { inventory.load("farm") }
            withTimeout(5000) { inventory.state.first { !it.busy } }
            val stockRetained = inventory.state.value.workspace.items.isNotEmpty()
            server.enqueue(json("""{"farmId":"farm","cycleId":"crop","canRecord":true,"activities":[],"observations":[]}"""))
            server.enqueue(json("""{"items":[],"lowStockCount":0}"""))
            withContext(Dispatchers.Main) { monitor.load("farm", "crop") }
            withTimeout(5000) { monitor.state.first { !it.busy && it.canRecord } }
            server.enqueue(MockResponse().setResponseCode(403))
            withContext(Dispatchers.Main) { monitor.load("farm", "crop") }
            withTimeout(5000) { monitor.state.first { !it.busy } }
            val recordPermissionRetained = monitor.state.value.canRecord
            withContext(Dispatchers.Main) { harvest.clear(); inventory.clear(); monitor.clear() }
            assertEquals("Explicit access denial must clear old data/authority", listOf(false, false, false),
                listOf(harvestRetained, stockRetained, recordPermissionRetained))
        }
    }

    @Test fun expiredSessionKeepsDraftWithoutTreatingItAsARecordConflict() = runBlocking {
        fixture { (db, server, sync, _, _) ->
            val id = UUID.randomUUID().toString()
            db.pendingOperationDao().add(PendingOperation(id, "user", "farm", "harvest", Gson().toJson(
                SyncWriteRequest("farm", "crop", "harvest", harvest = RecordHarvestDto(BigDecimal.ONE, null))), 1))
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setResponseCode(401))
            assertTrue(sync.uploadPending())
            assertEquals("pending", db.pendingOperationDao().find(id, "user")?.status)
            assertTrue(sync.progress.value.confirmedFarms.isEmpty())
            assertTrue("Expired credentials require a new sign-in, not stock/data review", sync.progress.value.signInRequired)
        }
    }

    @Test fun partialPhotoFailureKeepsFileAndRejectsUnrelatedConfirmation() = runBlocking {
        fixture { (db, server, sync, _, files) ->
            val id = UUID.randomUUID().toString()
            val observation = UUID.randomUUID().toString()
            val photo = UUID.randomUUID().toString()
            val input = SyncWriteRequest("farm", "crop", "observation",
                observation = com.smartfarm.mobile.data.remote.dto.ObservationInputDto(1, null, null))
            OfflineObservationQueue(db).add(PendingOperation(id, "user", "farm", "observation", Gson().toJson(input), 1),
                com.smartfarm.mobile.data.local.entity.PendingPhoto(photo, "user", "farm", "crop", null, id, "image/png", 1))
            files.file(photo).apply { parentFile!!.mkdirs() }.writeBytes(android.util.Base64.decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aF1cAAAAASUVORK5CYII=", android.util.Base64.DEFAULT))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"operationId":"$id","operationType":"observation","result":{"id":"$observation"},"replayed":false}"""))
            server.enqueue(MockResponse().setResponseCode(503))
            assertFalse(sync.uploadPending())
            assertNull(db.pendingOperationDao().find(id, "user"))
            assertEquals(observation, db.pendingPhotoDao().find("user", photo)?.observationId)
            assertTrue(files.file(photo).exists())
            assertEquals(1L, sync.progress.value.confirmedFarms["farm"])
            fun photoReceipt(photoId: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"id":"$photoId","observationId":"$observation","contentType":"image/png","fileSizeBytes":68,"uploadedAtUtc":"2026-11-14T01:00:00Z"}""")
            server.enqueue(photoReceipt(UUID.randomUUID().toString()))
            assertTrue(sync.uploadPending())
            assertEquals("review", db.pendingPhotoDao().find("user", photo)?.status)
            assertTrue(files.file(photo).exists())
            assertEquals(1L, sync.progress.value.confirmedFarms["farm"])
            sync.retryPhoto(photo)
            server.enqueue(photoReceipt(photo))
            assertTrue(sync.uploadPending())
            assertNull(db.pendingPhotoDao().find("user", photo))
            assertFalse(files.file(photo).exists())
            assertEquals(2L, sync.progress.value.confirmedFarms["farm"])
        }
    }

    @Test fun stockConflictNeverRetriesOrRebasesWithoutCurrentReviewedVersion() = runBlocking {
        fixture { (db, server, sync, _, _) ->
            val id = UUID.randomUUID().toString()
            val item = UUID.randomUUID().toString()
            val payload = Gson().toJson(SyncWriteRequest("farm", item, "inventory",
                movement = com.smartfarm.mobile.data.remote.dto.InventoryMovementDto(3, BigDecimal("-2"), "activity", null, "old")))
            db.pendingOperationDao().add(PendingOperation(id, "user", "farm", "inventory", payload, 1))
            server.enqueue(MockResponse().setResponseCode(409))
            assertTrue(sync.uploadPending())
            assertEquals("review", db.pendingOperationDao().find(id, "user")?.status)
            val calls = server.requestCount
            sync.retry(id)
            assertTrue(sync.uploadPending())
            assertEquals(calls, server.requestCount)
            fun stock(version: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"id":"$item","farmId":"farm","name":"Seeds","category":1,"unit":"kg","quantityOnHand":5,"rowVersion":"$version"}""")
            server.enqueue(stock("fresh"))
            val preview = sync.previewStockRetry(id)
            assertEquals(BigDecimal("3"), preview.expectedBalance)
            server.enqueue(stock("changed-again"))
            try { sync.confirmStockRetry(preview); fail("A changed version requires a new review") }
            catch (_: IllegalArgumentException) { }
            assertEquals("review", db.pendingOperationDao().find(id, "user")?.status)
            assertEquals(payload, db.pendingOperationDao().find(id, "user")?.payloadJson)
            assertTrue(sync.progress.value.confirmedFarms.isEmpty())
        }
    }

    @Test fun clearingDuringRefreshNeverRestoresPrivateInventoryOrHarvestData() = runBlocking {
        fixture { (_, server, sync, auth, _) ->
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val inventory = com.smartfarm.mobile.features.inventory.InventoryViewModel(
                com.smartfarm.mobile.data.repository.InventoryRepository(auth) { retrofit.create(InventoryApi::class.java) }, sync)
            val harvest = com.smartfarm.mobile.features.harvests.HarvestViewModel(
                com.smartfarm.mobile.data.repository.HarvestRepository(auth) { retrofit.create(HarvestApi::class.java) }, sync)
            server.enqueue(MockResponse().setBodyDelay(1, TimeUnit.SECONDS).setHeader("Content-Type", "application/json")
                .setBody("""{"items":[{"id":"item","farmId":"farm","name":"Private stock","category":1,"unit":"kg","quantityOnHand":2,"rowVersion":"v"}],"lowStockCount":0}"""))
            withContext(Dispatchers.Main) { inventory.load("farm") }
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            withContext(Dispatchers.Main) { inventory.clear() }
            delay(1300)
            val inventoryRestored = inventory.state.value.workspace.items.isNotEmpty()
            server.enqueue(MockResponse().setBodyDelay(1, TimeUnit.SECONDS).setHeader("Content-Type", "application/json")
                .setBody("""{"farmId":"farm","cycleId":"crop","cropName":"Rice","plotName":"Plot","unit":"kg","cropStatus":1,"canRecord":true,"canVoid":true,"totalQuantity":2,"validHarvestCount":1,"voidedHarvestCount":0,"entries":[]}"""))
            withContext(Dispatchers.Main) { harvest.load("farm", "crop") }
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            withContext(Dispatchers.Main) { harvest.clear() }
            delay(1300)
            assertEquals("Cleared inventory/harvest must not be repopulated by old requests",
                listOf(false, false), listOf(inventoryRestored, harvest.state.value.overview != null))
        }
    }

    @Test fun logoutDuringUploadKeepsLocalWorkAndNeverPublishesConfirmation() = runBlocking {
        fixture { (db, server, sync, auth, _) -> coroutineScope {
            val id = UUID.randomUUID().toString()
            db.pendingOperationDao().add(PendingOperation(id, "user", "farm", "harvest", Gson().toJson(
                SyncWriteRequest("farm", "crop", "harvest", harvest = RecordHarvestDto(BigDecimal.ONE, null))), 1))
            server.enqueue(receipt(id).setBodyDelay(1, TimeUnit.SECONDS))
            val upload = async(Dispatchers.IO) { sync.uploadPending() }
            withTimeout(5000) { sync.progress.first { it.sendingId == id } }
            auth.logout()
            assertTrue(upload.await())
            assertNotNull(db.pendingOperationDao().find(id, "user"))
            assertTrue(sync.progress.value.confirmedFarms.isEmpty())
            assertNull(sync.progress.value.sendingId)
        } }
    }

    @Test fun refreshRetainsConfirmedTotalsWhileLoading() = runBlocking {
        fixture { (_, server, sync, auth, _) ->
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val harvest = com.smartfarm.mobile.features.harvests.HarvestViewModel(
                com.smartfarm.mobile.data.repository.HarvestRepository(auth) { retrofit.create(HarvestApi::class.java) }, sync)
            fun overview() = MockResponse().setHeader("Content-Type", "application/json").setBody(
                """{"farmId":"farm","cycleId":"crop","cropName":"Rice","plotName":"Plot","unit":"kg","cropStatus":1,"canRecord":true,"canVoid":true,"totalQuantity":2,"validHarvestCount":1,"voidedHarvestCount":0,"entries":[]}""")
            server.enqueue(overview())
            withContext(Dispatchers.Main) { harvest.load("farm", "crop") }
            withTimeout(5000) { harvest.state.first { !it.busy && it.overview != null } }
            server.enqueue(overview().setBodyDelay(1, TimeUnit.SECONDS))
            withContext(Dispatchers.Main) {
                harvest.load("farm", "crop")
                assertEquals("An automatic refresh must keep confirmed totals visible", BigDecimal("2"), harvest.state.value.overview?.totalQuantity)
                harvest.clear()
            }
        }
    }

    @Test fun sendingIsNotConfirmedUntilMatchingReceipt() = runBlocking {
        fixture { (db, server, sync, _, _) -> coroutineScope {
            val id = UUID.randomUUID().toString()
            db.pendingOperationDao().add(PendingOperation(id, "user", "farm", "harvest", Gson().toJson(
                SyncWriteRequest("farm", "crop", "harvest", harvest = RecordHarvestDto(BigDecimal.ONE, null))), 1))
            server.enqueue(receipt(id).setBodyDelay(1, TimeUnit.SECONDS))
            val upload = async(Dispatchers.IO) { sync.uploadPending() }
            withTimeout(5000) { sync.progress.first { it.sendingId == id } }
            assertTrue(sync.progress.value.confirmedFarms.isEmpty())
            assertNotNull(db.pendingOperationDao().find(id, "user"))
            assertTrue(upload.await())
            assertNull(sync.progress.value.sendingId)
            assertEquals(1L, sync.progress.value.confirmedFarms["farm"])
        } }
    }
    @Test fun retryKeepsIdentityAndInvalidatesOnlyConfirmedAccountAndResource() = runBlocking {
        fixture { (db, server, sync, _, _) ->
            val id = UUID.randomUUID().toString()
            val input = SyncWriteRequest("farm", "crop", "harvest", harvest = RecordHarvestDto(BigDecimal("2.500"), null))
            val key = "farm/farm/cycle/crop/harvests"
            val keys = listOf("user" to key, "other" to key, "user" to "farm/another/cycle/crop/harvests", "user" to "farm/farm/weather")
            keys.forEach { (user, resource) -> db.cachedResponseDao().save(CachedResponse(user, resource, "{}", 1)) }
            db.pendingOperationDao().add(PendingOperation(id, "user", "farm", "harvest", Gson().toJson(input), 1))
            server.enqueue(MockResponse().setResponseCode(503))
            assertFalse(sync.uploadPending())
            assertNotNull(db.pendingOperationDao().find(id, "user"))
            assertNotNull(db.cachedResponseDao().find("user", key))
            server.enqueue(receipt(id))
            assertTrue(sync.uploadPending())
            assertNull(db.pendingOperationDao().find(id, "user"))
            assertNull("Confirmed harvest must invalidate its old totals", db.cachedResponseDao().find("user", key))
            keys.drop(1).forEach { (user, resource) -> assertNotNull(db.cachedResponseDao().find(user, resource)) }
            val first = server.takeRequest(); val second = server.takeRequest()
            assertEquals(first.path, second.path)
            assertEquals(first.body.readUtf8(), second.body.readUtf8())
        }
    }

    @Test fun mismatchedReceiptKeepsDraftAndCacheForExplicitReview() = runBlocking {
        fixture { (db, server, sync, _, _) ->
            val id = UUID.randomUUID().toString()
            val key = "farm/farm/cycle/crop/harvests"
            db.cachedResponseDao().save(CachedResponse("user", key, "{}", 1))
            db.pendingOperationDao().add(PendingOperation(id, "user", "farm", "harvest", Gson().toJson(
                SyncWriteRequest("farm", "crop", "harvest", harvest = RecordHarvestDto(BigDecimal.ONE, null))), 1))
            server.enqueue(receipt(UUID.randomUUID().toString()))
            assertTrue(sync.uploadPending())
            assertEquals("review", db.pendingOperationDao().find(id, "user")?.status)
            assertNotNull(db.cachedResponseDao().find("user", key))
        }
    }

    private fun receipt(id: String) = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"operationId":"$id","operationType":"harvest","result":{},"replayed":false}""")

    private data class Environment(val db: SmartFarmDatabase, val server: MockWebServer,
        val sync: SyncRepository, val auth: AuthRepository, val files: LocalPhotoFiles,
        val cache: com.smartfarm.mobile.data.local.OfflineReadCache)

    private suspend fun fixture(test: suspend (Environment) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        val privateFiles = java.io.File(context.cacheDir, "sync-confirmation-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getFilesDir(): java.io.File = privateFiles
        }
        val files = LocalPhotoFiles(isolated)
        val server = MockWebServer(); server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"accessToken":"test","refreshToken":"refresh","expiresIn":3600,"tokenType":"Bearer"}"""))
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"id":"user","fullName":"Test","email":"test@example.test"}"""))
            auth.login("test@example.test", "test")
            server.takeRequest(); server.takeRequest()
            val cache = com.smartfarm.mobile.data.local.OfflineReadCache(auth, db.cachedResponseDao())
            val sync = SyncRepository(auth, db.pendingOperationDao(), db.pendingPhotoDao(), files,
                OfflineObservationQueue(db), { retrofit.create(SyncApi::class.java) },
                { retrofit.create(MonitoringApi::class.java) }, { retrofit.create(InventoryApi::class.java) },
                WorkManager.getInstance(context), cache)
            test(Environment(db, server, sync, auth, files, cache))
        } finally { server.shutdown(); db.close(); privateFiles.deleteRecursively() }
    }
}
