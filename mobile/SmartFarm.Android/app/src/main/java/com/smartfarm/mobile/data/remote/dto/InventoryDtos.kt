package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

data class InventoryItemDto(
    val id: String,
    val farmId: String,
    val name: String,
    val category: Int,
    val unit: String,
    val description: String?,
    val quantityOnHand: BigDecimal,
    val reorderLevel: BigDecimal?,
    val isLowStock: Boolean,
    val archivedAtUtc: String?,
    val rowVersion: String
)

data class InventoryWorkspaceDto(
    val items: List<InventoryItemDto>, val lowStockCount: Int
)

data class InventoryItemInputDto(
    val name: String,
    val category: Int,
    val unit: String,
    val description: String? = null,
    val reorderLevel: BigDecimal? = null,
    val initialQuantity: BigDecimal = BigDecimal.ZERO,
    val rowVersion: String? = null
)

data class InventoryMovementDto(
    val transactionType: Int,
    val quantityChange: BigDecimal,
    val cropActivityId: String? = null,
    val reason: String? = null,
    val rowVersion: String
)

data class InventoryReversalDto(val rowVersion: String, val reason: String)
data class InventoryActivityOptionDto(val id: String, val label: String)

data class InventoryTransactionDto(
    val id: String,
    val inventoryItemId: String,
    val cropActivityId: String?,
    val reversesTransactionId: String?,
    val transactionType: Int,
    val quantityChange: BigDecimal,
    val occurredAtUtc: String,
    val recordedAtUtc: String,
    val recordedByUserId: String,
    val reason: String?
)

