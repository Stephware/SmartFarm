package com.smartfarm.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.smartfarm.mobile.data.local.entity.LocalMetadata

@Dao
interface LocalMetadataDao {
    @Query("SELECT value FROM local_metadata WHERE `key` = :key")
    suspend fun read(key: String): String?

    @Upsert
    suspend fun write(metadata: LocalMetadata)
}
