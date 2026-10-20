package com.smartfarm.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// The payload and operation ID never change between retries.
@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey val operationId: String,
    val userId: String,
    val farmId: String,
    val operationType: String,
    val payloadJson: String,
    val createdAtUtc: Long,
    val status: String = "pending",
    val errorMessage: String? = null
)
