package com.smartfarm.mobile.data.repository

import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.remote.dto.*

class InventoryRepository(
    private val auth: AuthRepository, private val cache: OfflineReadCache? = null,
    private val api: () -> InventoryApi
) {
    private val transactionList = TypeToken.getParameterized(
        List::class.java, InventoryTransactionDto::class.java).type
    private val activityList = TypeToken.getParameterized(
        List::class.java, InventoryActivityOptionDto::class.java).type

    suspend fun listSnapshot(farmId: String): CachedRead<InventoryWorkspaceDto> =
        cache?.read("farm/$farmId/inventory", InventoryWorkspaceDto::class.java) {
            auth.authorized { api().list(it, farmId) }
        } ?: CachedRead(auth.authorized { api().list(it, farmId) },
            false, System.currentTimeMillis())
    suspend fun list(farmId: String) = listSnapshot(farmId).value

    suspend fun activitiesSnapshot(farmId: String): CachedRead<List<InventoryActivityOptionDto>> =
        cache?.read("farm/$farmId/inventory/activities", activityList) {
            auth.authorized { api().activities(it, farmId) }
        } ?: CachedRead(auth.authorized { api().activities(it, farmId) },
            false, System.currentTimeMillis())
    suspend fun activities(farmId: String) = activitiesSnapshot(farmId).value

    suspend fun itemSnapshot(farmId: String, itemId: String): CachedRead<InventoryItemDto> =
        cache?.read("farm/$farmId/inventory/$itemId", InventoryItemDto::class.java) {
            auth.authorized { api().item(it, farmId, itemId) }
        } ?: CachedRead(auth.authorized { api().item(it, farmId, itemId) },
            false, System.currentTimeMillis())
    suspend fun item(farmId: String, itemId: String) = itemSnapshot(farmId, itemId).value

    suspend fun historySnapshot(farmId: String, itemId: String): CachedRead<List<InventoryTransactionDto>> =
        cache?.read("farm/$farmId/inventory/$itemId/history", transactionList) {
            auth.authorized { api().history(it, farmId, itemId) }
        } ?: CachedRead(auth.authorized { api().history(it, farmId, itemId) },
            false, System.currentTimeMillis())
    suspend fun history(farmId: String, itemId: String) = historySnapshot(farmId, itemId).value

    suspend fun create(farmId: String, input: InventoryItemInputDto): InventoryItemDto =
        auth.authorized { api().create(it, farmId, input) }.also {
            cache?.invalidate("farm/$farmId/inventory")
        }

    suspend fun update(farmId: String, itemId: String, input: InventoryItemInputDto): InventoryItemDto =
        auth.authorized { api().update(it, farmId, itemId, input) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }

    suspend fun archive(farmId: String, itemId: String, version: String) =
        auth.authorized { api().archive(it, farmId, itemId, RowVersionDto(version)) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }

    suspend fun post(farmId: String, itemId: String, input: InventoryMovementDto): InventoryItemDto =
        auth.authorized { api().post(it, farmId, itemId, input) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }

    suspend fun reverse(farmId: String, itemId: String, transactionId: String,
        input: InventoryReversalDto): InventoryItemDto =
        auth.authorized { api().reverse(it, farmId, itemId, transactionId, input) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }
}

