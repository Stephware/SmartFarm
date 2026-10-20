package com.smartfarm.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartfarm.mobile.data.local.entity.CachedResponse

@Dao
interface CachedResponseDao {
    @Query("SELECT * FROM cached_responses WHERE userId = :userId AND resourceKey = :key LIMIT 1")
    suspend fun find(userId: String, key: String): CachedResponse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(response: CachedResponse)

    @Query("DELETE FROM cached_responses WHERE userId = :userId AND resourceKey = :key")
    suspend fun delete(userId: String, key: String)

    @Query("DELETE FROM cached_responses WHERE userId = :userId AND resourceKey LIKE :prefix")
    suspend fun deleteByPrefix(userId: String, prefix: String)
}
