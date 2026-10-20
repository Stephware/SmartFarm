package com.smartfarm.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Photos live in private app storage. A pending observation is linked by operation ID.
@Entity(tableName = "pending_photos")
data class PendingPhoto(
    @PrimaryKey val photoId: String,
    val userId: String,
    val farmId: String,
    val cycleId: String,
    val observationId: String?,
    val parentOperationId: String?,
    val mimeType: String,
    val savedAtUtcMillis: Long,
    val status: String = "pending",
    val errorMessage: String? = null
)
