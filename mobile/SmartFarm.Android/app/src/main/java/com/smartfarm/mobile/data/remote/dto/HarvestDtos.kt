package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

data class RecordHarvestDto(val quantity: BigDecimal, val notes: String? = null)
data class VoidHarvestDto(val reason: String)

data class HarvestEntryDto(
    val id: String,
    val cropCycleId: String,
    val quantity: BigDecimal,
    val unit: String,
    val harvestedAtUtc: String,
    val recordedAtUtc: String,
    val recordedByUserId: String,
    val notes: String?,
    val isVoided: Boolean,
    val voidedAtUtc: String?,
    val voidedByUserId: String?,
    val voidReason: String?
)

data class HarvestOverviewDto(
    val farmId: String,
    val cycleId: String,
    val cropName: String,
    val plotName: String,
    val unit: String,
    val cropStatus: Int,
    val canRecord: Boolean,
    val canVoid: Boolean,
    val totalQuantity: BigDecimal,
    val validHarvestCount: Int,
    val voidedHarvestCount: Int,
    val entries: List<HarvestEntryDto>
)

