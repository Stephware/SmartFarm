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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val currentRowVersion: String,
    val includesFieldActivity: Boolean = false
)

class SyncRepository(
    private val auth: AuthRepository,
    private val dao: PendingOperationDao,
    private val photoDao: PendingPhotoDao,
    private val files: LocalPhotoFiles,
    private val observationQueue: OfflineObservationQueue,
    private val api: () -> SyncApi,
    private val monitoringApi: () -> MonitoringApi,
    private val inventoryApi: () -> InventoryApi,
    private val work: WorkManager,
    private val cache: com.smartfarm.mobile.data.local.OfflineReadCache? = null
) {
    private val gson = Gson()
    private val uploadMutex = Mutex()
    private val progressMutable = MutableStateFlow(SyncProgress())
    val progress = progressMutable.asStateFlow()

    fun resetProgress() { progressMutable.value = SyncProgress(auth.currentUserId) }

    private fun sending(user: String, farm: String, id: String) {
        if (auth.currentUserId != user) return
        val current = progressMutable.value.takeIf { it.userId == user } ?: SyncProgress(user)
        progressMutable.value = current.copy(sendingId = id, sendingFarmId = farm)
    }

    private fun confirmed(user: String, farm: String) {
        if (auth.currentUserId != user) return
        val current = progressMutable.value.takeIf { it.userId == user } ?: SyncProgress(user)
        progressMutable.value = current.copy(sendingId = null, sendingFarmId = null, signInRequired = false,
            confirmedFarms = current.confirmedFarms + (farm to ((current.confirmedFarms[farm] ?: 0) + 1)))
    }

    private fun requireSignIn(user: String) {
        if (auth.currentUserId == user)
            progressMutable.value = progressMutable.value.copy(signInRequired = true)
    }

    fun watch(): Flow<List<PendingOperation>> =
        dao.watch(requireNotNull(auth.currentUserId) { "Sign in to view pending changes." })

    fun watchPhotos(): Flow<List<PendingPhoto>> =
        photoDao.watch(requireNotNull(auth.currentUserId) { "Sign in to view pending photos." })

    suspend fun queueActivity(farmId: String, cycleId: String, input: ActivityInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "activity", activity = input))

    suspend fun queueActivityWithStock(
        farmId: String, cycleId: String, input: ActivityStockInputDto
    ): String {
        require(input.activity.activityType in 2..3)
        require(input.quantityUsed > BigDecimal.ZERO && input.quantityUsed.scale() <= 3)
        require(input.inventoryItemId.isNotBlank() && input.rowVersion.isNotBlank())
        return queue(SyncWriteRequest(farmId, cycleId, "activity_stock", activityStock = input))
    }

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
        val photoId = UUID.randomUUID().toString()

        // Produce the immutable upload file once. Only publish the matching Room
        // rows in one transaction, so a process crash cannot leave a stranded photo.
        val mime = files.copyFrom(resolver, uri, photoId)
        try {
            if (auth.currentUserId != user) throw IllegalStateException("Account changed.")
            val operation = PendingOperation(
                operationId = operationId, userId = user, farmId = farmId,
                operationType = "observation",
                payloadJson = gson.toJson(SyncWriteRequest(
                    farmId, cycleId, "observation", observation = input)),
                createdAtUtc = System.currentTimeMillis())
            val photo = PendingPhoto(
                photoId = photoId, userId = user, farmId = farmId, cycleId = cycleId,
                observationId = null, parentOperationId = operationId,
                mimeType = mime, savedAtUtcMillis = System.currentTimeMillis())
            observationQueue.add(operation, photo)
        } catch (error: Exception) {
            files.delete(photoId)
            throw error
        }
        schedule()
        files.deleteTemporaryCameraSource(uri)
        return operationId
    }

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        // Ensure a new upload job can follow one that is just finishing.
        work.enqueueUniqueWork("smartfarm-sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
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
        if (operation.operationType in setOf("inventory", "activity_stock"))
            return // Explicit stock review is required.
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
        // A guessed/stale ID must never remove another account's private file.
        if (photoDao.find(user, id) == null) return
        photoDao.remove(user, id)
        files.delete(id)
    }

    // Never silently rebase. The user must review the latest stock total first.
    suspend fun previewStockRetry(id: String): StockRetryPreview {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(id, user)) { "Operation not found." }
        require(row.status == "review" &&
            row.operationType in setOf("inventory", "activity_stock"))
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val combined = input.activityStock
        val itemId = if (row.operationType == "activity_stock") {
            requireNotNull(combined).inventoryItemId
        } else input.targetId
        val change = if (row.operationType == "activity_stock") {
            requireNotNull(combined).quantityUsed.negate()
        } else requireNotNull(input.movement).quantityChange
        val latest = auth.authorized { inventoryApi().item(it, input.farmId, itemId) }
        require(latest.archivedAtUtc == null) { "This supply is archived." }
        if (combined != null) {
            val expectedCategory = if (combined.activity.activityType == 2) 2 else 3
            require(combined.activity.activityType in 2..3 &&
                latest.category == expectedCategory) {
                "The chosen stock item no longer matches this activity."
            }
        }
        val next = latest.quantityOnHand + change
        require(next >= BigDecimal.ZERO && next <= BigDecimal("999999999999999.999")) {
            "Not enough stock, or the quantity is too large."
        }
        return StockRetryPreview(id, latest.name, latest.quantityOnHand,
            change, next, latest.rowVersion,
            includesFieldActivity = row.operationType == "activity_stock")
    }

    suspend fun confirmStockRetry(preview: StockRetryPreview) {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(preview.operationId, user))
        require(row.status == "review" &&
            row.operationType in setOf("inventory", "activity_stock"))
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val combined = input.activityStock
        val itemId = if (row.operationType == "activity_stock") {
            requireNotNull(combined).inventoryItemId
        } else input.targetId
        val fresh = auth.authorized { inventoryApi().item(it, input.farmId, itemId) }
        require(fresh.rowVersion == preview.currentRowVersion && fresh.archivedAtUtc == null) {
            "Stock changed again. Check the latest balance."
        }
        val change = if (row.operationType == "activity_stock") {
            val details = requireNotNull(combined)
            val expectedCategory = if (details.activity.activityType == 2) 2 else 3
            require(details.activity.activityType in 2..3 &&
                fresh.category == expectedCategory) { "Supply category changed." }
            details.quantityUsed.negate()
        } else requireNotNull(input.movement).quantityChange
        require(fresh.quantityOnHand + change >= BigDecimal.ZERO) {
            "Not enough stock. Check the latest balance."
        }
        val updated = if (row.operationType == "activity_stock") {
            input.copy(activityStock = requireNotNull(combined).copy(rowVersion = fresh.rowVersion))
        } else {
            input.copy(movement = requireNotNull(input.movement).copy(rowVersion = fresh.rowVersion))
        }
        check(dao.rebaseInventory(row.operationId, user, gson.toJson(updated)) == 1)
        schedule()
    }

    // false requests WorkManager's exponential network/server retry.
    suspend fun uploadPending(): Boolean = uploadMutex.withLock {
        try { uploadPendingOnce() }
        finally { progressMutable.value = progressMutable.value.copy(sendingId = null, sendingFarmId = null) }
    }

    private suspend fun invalidateConfirmed(user: String, farm: String, target: String, type: String) {
        when (type) {
            "activity", "observation", "photo", "activity_stock" ->
                cache?.invalidate(user, "farm/$farm/cycle/$target/monitoring")
            "harvest" -> cache?.invalidate(user, "farm/$farm/cycle/$target/harvests")
        }
        if (type in setOf("inventory", "activity_stock"))
            cache?.invalidate(user, "farm/$farm/inventory", prefix = true)
        else if (type == "activity") cache?.invalidate(user, "farm/$farm/inventory/activities")
    }

    private suspend fun uploadPendingOnce(): Boolean {
        val user = auth.currentUserId ?: return true
        // Reconcile abandoned partial files; every account's queued IDs are protected.
        files.pruneAbandoned(photoDao.allPhotoIds().toSet())
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
                    sending(user, input.farmId, operation.operationId)
                    val response = auth.authorized {
                        api().submit(it, operation.operationId, input)
                    }
                    // Never discard local data based on an unrelated/malformed reply.
                    if (!SyncReceiptValidator.operationMatches(
                            response, operation.operationId, operation.operationType)) {
                        dao.needsReview(operation.operationId, user,
                            "Unexpected sync confirmation. Review before retrying.")
                        continue
                    }
                    if (operation.operationType == "activity_stock" &&
                        !SyncReceiptValidator.activityStockMatches(response, input.activityStock)) {
                        dao.needsReview(operation.operationId, user,
                            "Unexpected activity and stock confirmation. Review before retrying.")
                        continue
                    }
                    if (operation.operationType == "observation") {
                        val observationId = SyncReceiptValidator.observationId(response)
                        if (observationId == null) {
                            dao.needsReview(operation.operationId, user,
                                "The server did not return a valid observation ID.")
                            continue
                        }
                        photoDao.resolveObservation(user, operation.operationId, observationId)
                    }
                    invalidateConfirmed(user, input.farmId, input.targetId, operation.operationType)
                    dao.remove(operation.operationId, user)
                    confirmed(user, input.farmId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    when (error.code()) {
                        400, 403, 404, 409, 422 ->
                            dao.needsReview(operation.operationId, user,
                                "Server rejected this change (${error.code()}). Review before retrying.")
                        401 -> { requireSignIn(user); return true }
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
                    sending(user, photo.farmId, photo.photoId)
                    val body = file.asRequestBody(photo.mimeType.toMediaType())
                    val part = MultipartBody.Part.createFormData("photo", "field-photo", body)
                    val saved = auth.authorized { bearer ->
                        monitoringApi().uploadWithId(bearer, photo.farmId, photo.cycleId,
                            requireNotNull(photo.observationId), photo.photoId, part)
                    }
                    if (!SyncReceiptValidator.photoMatches(saved, photo)) {
                        photoDao.review(user, photo.photoId,
                            "Unexpected photo confirmation. Review before retrying.")
                        continue
                    }
                    invalidateConfirmed(user, photo.farmId, photo.cycleId, "photo")
                    photoDao.remove(user, photo.photoId)
                    files.delete(photo.photoId)
                    confirmed(user, photo.farmId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    when (error.code()) {
                        400, 403, 404, 409, 422 ->
                            photoDao.review(user, photo.photoId,
                                "Photo rejected (${error.code()}). Review before retrying.")
                        401 -> { requireSignIn(user); return true }
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
