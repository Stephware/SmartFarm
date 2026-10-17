package com.smartfarm.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartfarm.mobile.data.local.dao.LocalMetadataDao
import com.smartfarm.mobile.data.local.entity.LocalMetadata

@Database(entities = [LocalMetadata::class], version = 1, exportSchema = true)
abstract class SmartFarmDatabase : RoomDatabase() {
    abstract fun metadataDao(): LocalMetadataDao
}
