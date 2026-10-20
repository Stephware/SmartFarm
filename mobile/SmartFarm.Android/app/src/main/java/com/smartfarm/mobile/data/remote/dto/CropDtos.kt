package com.smartfarm.mobile.data.remote.dto

data class CropTypeDto(
    val id: String,
    val name: String,
    val defaultGrowthDays: Int?,
    val defaultWateringIntervalDays: Int?,
    val defaultHarvestUnit: String
)

data class CropTypeInputDto(
    val name: String,
    val defaultGrowthDays: Int? = null,
    val defaultWateringIntervalDays: Int? = null,
    val defaultHarvestUnit: String = "kg"
)

data class CropCycleDto(
    val id: String,
    val farmId: String,
    val plotId: String,
    val plotName: String,
    val cropTypeId: String,
    val cropTypeName: String,
    val status: Int,
    val plannedPlantingDate: String,
    val plantedDate: String?,
    val expectedHarvestDate: String?,
    val harvestUnit: String,
    val wateringIntervalDays: Int?,
    val completedAtUtc: String?,
    val terminationReason: String?,
    val rowVersion: String
) {
    val statusName: String
        get() = when (status) {
            0 -> "Planned"
            1 -> "Active"
            2 -> "Completed"
            3 -> "Failed"
            4 -> "Cancelled"
            else -> "Unknown"
        }
}

data class CropCyclePlanDto(
    val cropTypeId: String,
    val plannedPlantingDate: String,
    val expectedHarvestDate: String? = null,
    val harvestUnit: String? = null,
    val wateringIntervalDays: Int? = null,
    val rowVersion: String? = null
)

data class CropTransitionDto(
    val rowVersion: String,
    val status: Int,
    val plantedDate: String? = null,
    val terminationReason: String? = null
)

