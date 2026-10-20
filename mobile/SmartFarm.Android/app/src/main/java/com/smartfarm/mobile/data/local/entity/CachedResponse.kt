package com.smartfarm.mobile.data.local.entity

import androidx.room.Entity

// Snapshot of a successful, authenticated API read, scoped to one account.
@Entity(tableName = "cached_responses", primaryKeys = ["userId", "resourceKey"])
data class CachedResponse(
    val userId: String,
    val resourceKey: String,
    val json: String,
    val savedAtUtcMillis: Long
)
