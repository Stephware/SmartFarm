package com.smartfarm.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPhotoDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun add(photo: PendingPhoto)

    @Query("SELECT * FROM pending_photos WHERE userId = :userId ORDER BY savedAtUtcMillis DESC LIMIT 100")
    fun watch(userId: String): Flow<List<PendingPhoto>>

    @Query("SELECT * FROM pending_photos WHERE userId = :userId AND status = 'pending' AND observationId IS NOT NULL ORDER BY savedAtUtcMillis ASC LIMIT 40")
    suspend fun ready(userId: String): List<PendingPhoto>

    @Query("UPDATE pending_photos SET observationId = :observationId WHERE userId = :userId AND parentOperationId = :operationId AND observationId IS NULL")
    suspend fun resolveObservation(userId: String, operationId: String, observationId: String)

    @Query("SELECT * FROM pending_photos WHERE userId = :userId AND photoId = :photoId LIMIT 1")
    suspend fun find(userId: String, photoId: String): PendingPhoto?

    @Query("UPDATE pending_photos SET status = 'review', errorMessage = :message WHERE userId = :userId AND photoId = :photoId")
    suspend fun review(userId: String, photoId: String, message: String)

    @Query("UPDATE pending_photos SET status = 'pending', errorMessage = NULL WHERE userId = :userId AND photoId = :photoId AND status = 'review'")
    suspend fun retry(userId: String, photoId: String)

    @Query("DELETE FROM pending_photos WHERE userId = :userId AND photoId = :photoId")
    suspend fun remove(userId: String, photoId: String)
}
