package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

/** Only read-only fields needed by mobile; unknown server fields are ignored. */
data class FarmHarvestSummaryDto(
    val farmId: String, val farmName: String, val timeZoneId: String,
    val fromDate: String?, val toDate: String?,
    val validHarvestCount: Int, val voidedHarvestCount: Int,
    val unitTotals: List<HarvestUnitSummaryDto>,
    val cropPlotTotals: List<CropPlotYieldDto>,
    val monthlyTotals: List<MonthlyHarvestYieldDto>
)

data class HarvestUnitSummaryDto(val unit: String, val quantity: BigDecimal, val harvestCount: Int)
data class CropPlotYieldDto(
    val cropTypeName: String, val plotName: String, val unit: String,
    val quantity: BigDecimal, val harvestCount: Int
)
data class MonthlyHarvestYieldDto(
    val month: String, val unit: String, val quantity: BigDecimal, val harvestCount: Int
)

data class FarmFinanceSummaryDto(
    val farmId: String, val fromDate: String?, val toDate: String?,
    val incomePhp: BigDecimal, val expensesPhp: BigDecimal, val netPhp: BigDecimal,
    val entryCount: Int, val months: List<MonthlyFinanceSummaryDto>,
    val categories: List<FinanceCategorySummaryDto>
)
data class MonthlyFinanceSummaryDto(
    val month: String, val incomePhp: BigDecimal,
    val expensesPhp: BigDecimal, val netPhp: BigDecimal
)
data class FinanceCategorySummaryDto(
    val kind: String, val category: String, val amountPhp: BigDecimal, val entryCount: Int
)
