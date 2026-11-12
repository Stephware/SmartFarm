package com.smartfarm.mobile.navigation

import com.smartfarm.mobile.data.remote.dto.CropCycleDto

/** One destination for every secondary farm section; no nested horizontal tabs. */
internal val farmSections = setOf("weather", "plots", "members", "settings")

internal fun safeFarmSection(section: String?): String =
    if (section in farmSections) requireNotNull(section) else "overview"

/** Only an active cycle can receive a new activity, inspection, or harvest. */
internal fun activeRecordCycles(cycles: List<CropCycleDto>): List<CropCycleDto> =
    cycles.filter { it.status == 1 }.sortedWith(compareBy({ it.plotName }, { it.cropTypeName }))

/** Centralized, testable routes; never interpolate a name or arbitrary user text into a route. */
internal fun quickRecordRoute(farmId: String, action: String, cycleId: String?): String? {
    if (farmId.isBlank()) return null
    if (action == "supplies") return "inventory/$farmId"
    if (cycleId.isNullOrBlank()) return null
    return when (action) {
        "watering", "fertilizing", "inspection" ->
            "monitor/$farmId/$cycleId?quick=$action"
        "harvest" -> "harvests/$farmId/$cycleId"
        else -> null
    }
}

/** Warn before leaving screens that can contain an unfinished field entry. */
internal fun requiresExitConfirmation(route: String?): Boolean =
    route !in setOf("farm/{farmId}", "more/{farmId}", "farms")
