package com.smartfarm.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Technical app metadata only; domain tables will be introduced with their features.
@Entity(tableName = "local_metadata")
data class LocalMetadata(@PrimaryKey val key: String, val value: String)
