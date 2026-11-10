package com.smartfarm.mobile.data.sync

import androidx.room.withTransaction
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto

/** A photo and its parent observation must either both be queued or neither be queued. */
class OfflineObservationQueue(private val database: SmartFarmDatabase) {
    suspend fun add(operation: PendingOperation, photo: PendingPhoto) {
        require(operation.operationType == "observation" && photo.userId == operation.userId)
        require(photo.farmId == operation.farmId &&
            photo.parentOperationId == operation.operationId &&
            photo.observationId == null)
        database.withTransaction {
            database.pendingPhotoDao().add(photo)
            database.pendingOperationDao().add(operation)
        }
    }
}
