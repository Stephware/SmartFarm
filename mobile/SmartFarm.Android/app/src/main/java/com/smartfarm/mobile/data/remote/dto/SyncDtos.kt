package com.smartfarm.mobile.data.remote.dto

import com.google.gson.JsonObject

data class SyncWriteRequest(
    val farmId: String,
    val targetId: String,
    val operationType: String,
    val activity: ActivityInputDto? = null,
    val observation: ObservationInputDto? = null,
    val movement: InventoryMovementDto? = null,
    val harvest: RecordHarvestDto? = null
)

data class SyncWriteResponse(
    val operationId: String,
    val operationType: String,
    val result: JsonObject,
    val replayed: Boolean
)
