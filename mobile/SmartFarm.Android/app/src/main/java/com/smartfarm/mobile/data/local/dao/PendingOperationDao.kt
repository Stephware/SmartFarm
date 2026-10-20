package com.smartfarm.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartfarm.mobile.data.local.entity.PendingOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(operation: PendingOperation)

    @Query("SELECT * FROM pending_operations WHERE userId = :userId AND status = 'pending' ORDER BY createdAtUtc ASC LIMIT 40")
    suspend fun pending(userId: String): List<PendingOperation>

    @Query("SELECT * FROM pending_operations WHERE userId = :userId ORDER BY createdAtUtc DESC LIMIT 100")
    fun watch(userId: String): Flow<List<PendingOperation>>

    @Query("SELECT * FROM pending_operations WHERE operationId = :id AND userId = :userId LIMIT 1")
    suspend fun find(id: String, userId: String): PendingOperation?

    // Rebase only after the user has reviewed the latest server stock.
    @Query("UPDATE pending_operations SET payloadJson = :payload, status = 'pending', errorMessage = NULL WHERE operationId = :id AND userId = :userId AND status = 'review' AND operationType = 'inventory'")
    suspend fun rebaseInventory(id: String, userId: String, payload: String): Int

    @Query("DELETE FROM pending_operations WHERE operationId = :id AND userId = :userId")
    suspend fun remove(id: String, userId: String)

    @Query("UPDATE pending_operations SET status = 'review', errorMessage = :message WHERE operationId = :id AND userId = :userId")
    suspend fun needsReview(id: String, userId: String, message: String)

    @Query("UPDATE pending_operations SET status = 'pending', errorMessage = NULL WHERE operationId = :id AND userId = :userId AND status = 'review'")
    suspend fun retryAfterReview(id: String, userId: String)
}
