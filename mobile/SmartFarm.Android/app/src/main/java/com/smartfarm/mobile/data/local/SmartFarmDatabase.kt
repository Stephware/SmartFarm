package com.smartfarm.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartfarm.mobile.data.local.dao.LocalMetadataDao
import com.smartfarm.mobile.data.local.dao.PendingOperationDao
import com.smartfarm.mobile.data.local.entity.LocalMetadata
import com.smartfarm.mobile.data.local.entity.PendingOperation

@Database(entities = [LocalMetadata::class, PendingOperation::class], version = 2, exportSchema = true)
abstract class SmartFarmDatabase : RoomDatabase() {
    abstract fun metadataDao(): LocalMetadataDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
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
