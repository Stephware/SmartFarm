package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.remote.dto.*

class InventoryRepository(
    private val auth: AuthRepository, private val api: () -> InventoryApi
) {
    suspend fun list(farmId: String): InventoryWorkspaceDto =
        auth.authorized { api().list(it, farmId) }

    suspend fun activities(farmId: String): List<InventoryActivityOptionDto> =
        auth.authorized { api().activities(it, farmId) }

    suspend fun create(farmId: String, input: InventoryItemInputDto): InventoryItemDto =
        auth.authorized { api().create(it, farmId, input) }

    suspend fun item(farmId: String, itemId: String): InventoryItemDto =
        auth.authorized { api().item(it, farmId, itemId) }

    suspend fun update(farmId: String, itemId: String, input: InventoryItemInputDto): InventoryItemDto =
        auth.authorized { api().update(it, farmId, itemId, input) }

    suspend fun archive(farmId: String, itemId: String, version: String) =
        auth.authorized { api().archive(it, farmId, itemId, RowVersionDto(version)) }

    suspend fun history(farmId: String, itemId: String): List<InventoryTransactionDto> =
        auth.authorized { api().history(it, farmId, itemId) }

    suspend fun post(farmId: String, itemId: String, input: InventoryMovementDto): InventoryItemDto =
        auth.authorized { api().post(it, farmId, itemId, input) }

    suspend fun reverse(farmId: String, itemId: String, transactionId: String,
        input: InventoryReversalDto): InventoryItemDto =
        auth.authorized { api().reverse(it, farmId, itemId, transactionId, input) }
}

