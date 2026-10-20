package com.smartfarm.mobile.data.sync

import android.content.ContentResolver
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.smartfarm.mobile.data.local.dao.PendingOperationDao
import com.smartfarm.mobile.data.local.dao.PendingPhotoDao
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import com.smartfarm.mobile.data.remote.api.SyncApi
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.AuthRepository
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

data class StockRetryPreview(
    val operationId: String,
    val itemName: String,
    val currentBalance: BigDecimal,
    val change: BigDecimal,
    val expectedBalance: BigDecimal,
    val currentRowVersion: String
)

class SyncRepository(
    private val auth: AuthRepository,
    private val dao: PendingOperationDao,
    private val photoDao: PendingPhotoDao,
    private val files: LocalPhotoFiles,
    private val api: () -> SyncApi,
    private val monitoringApi: () -> MonitoringApi,
    private val inventoryApi: () -> InventoryApi,
    private val work: WorkManager
) {
    private val gson = Gson()
    private val uploadMutex = Mutex()

    fun watch(): Flow<List<PendingOperation>> =
        dao.watch(requireNotNull(auth.currentUserId) { "Sign in to view pending changes." })

    fun watchPhotos(): Flow<List<PendingPhoto>> =
        photoDao.watch(requireNotNull(auth.currentUserId) { "Sign in to view pending photos." })

    suspend fun queueActivity(farmId: String, cycleId: String, input: ActivityInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "activity", activity = input))

    suspend fun queueObservation(farmId: String, cycleId: String, input: ObservationInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "observation", observation = input))

    suspend fun queueInventory(farmId: String, itemId: String, input: InventoryMovementDto): String =
        queue(SyncWriteRequest(farmId, itemId, "inventory", movement = input))

    suspend fun queueHarvest(farmId: String, cycleId: String, input: RecordHarvestDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "harvest", harvest = input))

    private suspend fun queue(input: SyncWriteRequest, operationId: String = UUID.randomUUID().toString()): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        require(input.farmId.isNotBlank() && input.targetId.isNotBlank())
        dao.add(PendingOperation(
            operationId = operationId, userId = user, farmId = input.farmId,
            operationType = input.operationType, payloadJson = gson.toJson(input),
            createdAtUtc = System.currentTimeMillis()))
        schedule()
        return operationId
    }

    private suspend fun stagePhoto(
        farmId: String, cycleId: String, observationId: String?,
        parentOperationId: String?, uri: Uri, resolver: ContentResolver
    ): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        val photoId = UUID.randomUUID().toString()
        val mime = files.copyFrom(resolver, uri, photoId)
        try {
            if (auth.currentUserId != user) throw IllegalStateException("Account changed.")
            photoDao.add(PendingPhoto(
                photoId = photoId, userId = user, farmId = farmId, cycleId = cycleId,
                observationId = observationId, parentOperationId = parentOperationId,
                mimeType = mime, savedAtUtcMillis = System.currentTimeMillis()))
        } catch (error: Exception) {
            files.delete(photoId)
            throw error
        }
        return photoId
    }

    suspend fun queuePhoto(
        farmId: String, cycleId: String, observationId: String,
        uri: Uri, resolver: ContentResolver
    ): String {
        require(UUID.fromString(observationId).toString() == observationId.lowercase())
        val id = stagePhoto(farmId, cycleId, observationId, null, uri, resolver)
        schedule()
        files.deleteTemporaryCameraSource(uri)
        return id
    }

    // One stable observation operation ID is used for both the upload and photo dependency.
    suspend fun queueObservationWithPhoto(
        farmId: String, cycleId: String, input: ObservationInputDto,
        uri: Uri, resolver: ContentResolver
    ): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        val operationId = UUID.randomUUID().toString()
        val photoId = stagePhoto(farmId, cycleId, null, operationId, uri, resolver)
        try {
            queue(SyncWriteRequest(farmId, cycleId, "observation", observation = input), operationId)
        } catch (error: Exception) {
            photoDao.remove(user, photoId)
            files.delete(photoId)
            throw error
        }
        files.deleteTemporaryCameraSource(uri)
        return operationId
    }

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        work.enqueueUniqueWork("smartfarm-sync", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build())
    }

    fun enablePeriodicChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        work.enqueueUniquePeriodicWork("smartfarm-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints).build())
    }

    suspend fun retry(id: String) {
        val user = auth.currentUserId ?: return
        val operation = dao.find(id, user) ?: return
        if (operation.operationType == "inventory") return // Explicit stock review is required.
        dao.retryAfterReview(id, user)
        schedule()
    }

    suspend fun retryPhoto(id: String) {
        val user = auth.currentUserId ?: return
        photoDao.retry(user, id)
        schedule()
    }

    suspend fun discard(id: String) {
        val user = auth.currentUserId ?: return
        for (photo in photoDao.forObservationOperation(user, id)) {
            photoDao.remove(user, photo.photoId)
            files.delete(photo.photoId)
        }
        dao.remove(id, user)
    }

    suspend fun discardPhoto(id: String) {
        val user = auth.currentUserId ?: return
        photoDao.remove(user, id)
        files.delete(id)
    }

    // Never silently rebase. The user must first see and accept the latest stock total.
    suspend fun previewStockRetry(id: String): StockRetryPreview {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(id, user)) { "Operation not found." }
        require(row.status == "review" && row.operationType == "inventory")
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val movement = requireNotNull(input.movement)
        val latest = auth.authorized { inventoryApi().item(it, input.farmId, input.targetId) }
        require(latest.archivedAtUtc == null) { "This inventory item is archived." }
        val next = latest.quantityOnHand + movement.quantityChange
        require(next >= BigDecimal.ZERO && next <= BigDecimal("999999999999999.999")) {
            "Not enough stock, or the quantity is too large."
        }
        return StockRetryPreview(id, latest.name, latest.quantityOnHand,
            movement.quantityChange, next, latest.rowVersion)
    }

    suspend fun confirmStockRetry(preview: StockRetryPreview) {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(preview.operationId, user))
        require(row.operationType == "inventory" && row.status == "review")
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val fresh = auth.authorized { inventoryApi().item(it, input.farmId, input.targetId) }
        require(fresh.rowVersion == preview.currentRowVersion && fresh.archivedAtUtc == null) {
            "Stock changed again. Check the latest balance."
        }
        val movement = requireNotNull(input.movement)
        val updated = input.copy(movement = movement.copy(rowVersion = fresh.rowVersion))
        check(dao.rebaseInventory(row.operationId, user, gson.toJson(updated)) == 1)
        schedule()
    }

    // false requests WorkManager's exponential network/server retry.
    suspend fun uploadPending(): Boolean = uploadMutex.withLock { uploadPendingOnce() }

    private suspend fun uploadPendingOnce(): Boolean {
        val user = auth.currentUserId ?: return true
        while (true) {
            val pending = dao.pending(user)
            if (pending.isEmpty()) break
            for (operation in pending) {
                if (auth.currentUserId != user) return true
                val input = try {
                    gson.fromJson(operation.payloadJson, SyncWriteRequest::class.java)
                } catch (_: Exception) {
                    dao.needsReview(operation.operationId, user, "Invalid local entry.")
                    continue
                }
                try {
                    val response = auth.authorized {
                        api().submit(it, operation.operationId, input)
                    }
                    if (operation.operationType == "observation") {
                        val observationId = response.result.get("id")?.asString
                        if (observationId.isNullOrBlank()) {
                            dao.needsReview(operation.operationId, user,
                                "The server did not return an observation ID.")
                            continue
                        }
                        photoDao.resolveObservation(user, operation.operationId, observationId)
                    }
                    dao.remove(operation.operationId, user)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    when (error.code()) {
                        400, 403, 404, 409, 422 ->
                            dao.needsReview(operation.operationId, user,
                                "Server rejected this change (${error.code()}). Review before retrying.")
                        401 -> return true
                        else -> return false
                    }
                } catch (_: IOException) {
                    return false
                } catch (_: IllegalStateException) {
                    return true
                }
            }
        }
        while (true) {
            val photos = photoDao.ready(user)
            if (photos.isEmpty()) return true
            for (photo in photos) {
                if (auth.currentUserId != user) return true
                val file = files.file(photo.photoId)
                if (!file.exists() || file.length() !in 1..LocalPhotoFiles.MAX_BYTES.toLong()) {
                    photoDao.review(user, photo.photoId,
                        "Local photo is missing or too large. Discard and attach it again.")
                    continue
                }
                try {
                    val body = file.asRequestBody(photo.mimeType.toMediaType())
                    val part = MultipartBody.Part.createFormData("photo", "field-photo", body)
                    auth.authorized { bearer ->
                        monitoringApi().uploadWithId(bearer, photo.farmId, photo.cycleId,
                            requireNotNull(photo.observationId), photo.photoId, part)
                    }
                    photoDao.remove(user, photo.photoId)
                    files.delete(photo.photoId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    when (error.code()) {
                        400, 403, 404, 409, 422 ->
                            photoDao.review(user, photo.photoId,
                                "Photo rejected (${error.code()}). Review before retrying.")
                        401 -> return true
                        else -> return false
                    }
                } catch (_: IOException) {
                    return false
                } catch (_: IllegalStateException) {
                    return true
                }
            }
        }
    }
}
