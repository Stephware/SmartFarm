package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

/** Only read-only fields needed by mobile; unknown server fields are ignored. */
data class FarmHarvestSummaryDto(
    val farmId: String, val farmName: String, val timeZoneId: String,
    val fromDate: String?, val toDate: String?,
    val validHarvestCount: Int, val voidedHarvestCount: Int,
    val unitTotals: List<HarvestUnitSummaryDto>
)

data class HarvestUnitSummaryDto(val unit: String, val quantity: BigDecimal, val harvestCount: Int)
