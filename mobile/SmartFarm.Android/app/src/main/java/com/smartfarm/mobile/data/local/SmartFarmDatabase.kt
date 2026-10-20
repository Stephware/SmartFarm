package com.smartfarm.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.smartfarm.mobile.data.local.dao.CachedResponseDao
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.local.dao.LocalMetadataDao
import com.smartfarm.mobile.data.local.dao.PendingPhotoDao
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.local.dao.PendingOperationDao
import com.smartfarm.mobile.data.local.entity.LocalMetadata
import com.smartfarm.mobile.data.local.entity.PendingOperation

@Database(entities = [LocalMetadata::class, PendingOperation::class, CachedResponse::class, PendingPhoto::class], version = 4, exportSchema = true)
abstract class SmartFarmDatabase : RoomDatabase() {
    abstract fun metadataDao(): LocalMetadataDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun pendingPhotoDao(): PendingPhotoDao
    abstract fun cachedResponseDao(): CachedResponseDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_photos` (
                        `photoId` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `farmId` TEXT NOT NULL,
                        `cycleId` TEXT NOT NULL,
                        `observationId` TEXT,
                        `parentOperationId` TEXT,
                        `mimeType` TEXT NOT NULL,
                        `savedAtUtcMillis` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT
                    )
                """.trimIndent())
            }
        }


        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cached_responses` (
                        `userId` TEXT NOT NULL,
                        `resourceKey` TEXT NOT NULL,
                        `json` TEXT NOT NULL,
                        `savedAtUtcMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `resourceKey`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_operations` (
                        `operationId` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `farmId` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `createdAtUtc` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT
                    )
                """.trimIndent())
            }
        }
    }
}
