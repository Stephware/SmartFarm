package com.smartfarm.mobile.data.sync

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
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.remote.api.SyncApi
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.AuthRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.io.IOException

class SyncRepository(
    private val auth: AuthRepository,
    private val dao: PendingOperationDao,
    private val api: () -> SyncApi,
    private val work: WorkManager
) {
    private val gson = Gson()

    fun watch(): Flow<List<PendingOperation>> {
        val userId = requireNotNull(auth.currentUserId) { "Sign in to view your pending changes." }
        return dao.watch(userId)
    }

    suspend fun queueActivity(farmId: String, cycleId: String, input: ActivityInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "activity", activity = input))

    suspend fun queueObservation(farmId: String, cycleId: String, input: ObservationInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "observation", observation = input))

    suspend fun queueInventory(farmId: String, itemId: String, input: InventoryMovementDto): String =
        queue(SyncWriteRequest(farmId, itemId, "inventory", movement = input))

    suspend fun queueHarvest(farmId: String, cycleId: String, input: RecordHarvestDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "harvest", harvest = input))

    private suspend fun queue(input: SyncWriteRequest): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        require(input.farmId.isNotBlank() && input.targetId.isNotBlank())
        val id = UUID.randomUUID().toString()
        dao.add(PendingOperation(
            operationId = id, userId = user, farmId = input.farmId,
            operationType = input.operationType,
            payloadJson = gson.toJson(input), createdAtUtc = System.currentTimeMillis()))
        schedule()
        return id
    }

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        work.enqueueUniqueWork("smartfarm-sync", ExistingWorkPolicy.KEEP, request)
    }

    fun enablePeriodicChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints).build()
        work.enqueueUniquePeriodicWork("smartfarm-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP, request)
    }

    suspend fun retry(id: String) {
        val userId = auth.currentUserId ?: return
        dao.retryAfterReview(id, userId)
        schedule()
    }

    // false means WorkManager should retry later; HTTP conflicts need manual review.
    suspend fun uploadPending(): Boolean {
        val userId = auth.currentUserId ?: return true
        while (true) {
            val pending = dao.pending(userId)
            if (pending.isEmpty()) return true
            for (operation in pending) {
            if (auth.currentUserId != userId) return true
            val input = gson.fromJson(operation.payloadJson, SyncWriteRequest::class.java)
            try {
                auth.authorized { bearer -> api().submit(bearer, operation.operationId, input) }
                dao.remove(operation.operationId, userId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: HttpException) {
                when (error.code()) {
                    400, 403, 404, 409, 422 -> dao.needsReview(operation.operationId, userId,
                        "Server rejected this change (" + error.code() + "). Review before retrying.")
                    401 -> return true // Keep it queued until the account signs in again.
                    else -> return false
                }
            } catch (_: IOException) {
                return false
            } catch (_: IllegalStateException) {
                // Logged out before refreshing the token. Leave the queue intact.
                return true
            }
            }
        }
    }
}
