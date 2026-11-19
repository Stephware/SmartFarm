package com.smartfarm.mobile.navigation

import com.smartfarm.mobile.data.remote.dto.CropCycleDto

/** One destination for every secondary farm section; no nested horizontal tabs. */
internal val farmSections = setOf("weather", "plots", "members", "settings")

internal fun safeFarmSection(section: String?): String =
    if (section in farmSections) requireNotNull(section) else "overview"

/** Used only when there is no actual caller (for example a restored/direct entry). */
internal fun navigationParent(route: String?, farmId: String): String? = when {
    route in setOf("welcome", "onboarding", "farms") -> null
    route in setOf("login", "register", "onboarding-preview") -> "welcome"
    route == "forgot-password" -> "login"
    farmId.isBlank() -> "farms"
    route?.startsWith("monitor/") == true || route?.startsWith("harvests/") == true -> "crops/$farmId"
    route == "inventory/{farmId}/{itemId}" -> "inventory/$farmId"
    route in setOf("farm/{farmId}", "myfarm/{farmId}") -> "farms"
    else -> "farm/$farmId"
}

internal fun navigationTitle(route: String?, section: String? = null, quick: String? = null): String = when {
    route == "login" -> "Log in"
    route == "register" -> "Create account"
    route == "forgot-password" -> "Password help"
    route == "account" -> "My account"
    route == "farms" -> "My farms"
    route == "notifications" -> "Notifications"
    route == "sync" -> "Offline sync"
    route == "tasks/{farmId}" -> "Farm tasks"
    route == "crops/{farmId}" -> "Crops"
    route == "inventory/{farmId}" -> "Supplies"
    route == "inventory/{farmId}/{itemId}" -> "Supply details"
    route == "insights/{farmId}" -> "Harvest summary"
    route?.startsWith("harvests/") == true -> "Harvests"
    route?.startsWith("monitor/") == true -> when (quick) {
        "watering" -> "Record watering"; "fertilizing" -> "Record fertilizing"
        "inspection" -> "Record inspection"; "other" -> "Record activity"; else -> "Crop monitoring"
    }
    route == "farm/{farmId}/{section}" -> when (section) {
        "plots" -> "Farm plots"; "members" -> "Team & access"; "settings" -> "Farm settings"; else -> "Farm weather"
    }
    route == "myfarm/{farmId}" -> "My Farm"
    route == "farm/{farmId}" -> "Today"
    else -> "Welcome"
}

/** Only an active cycle can receive a new activity, inspection, or harvest. */
internal fun activeRecordCycles(cycles: List<CropCycleDto>): List<CropCycleDto> =
    cycles.filter { it.status == 1 }.sortedWith(compareBy({ it.plotName }, { it.cropTypeName }))

/** Centralized, testable routes; never interpolate a name or arbitrary user text into a route. */
internal fun quickRecordRoute(farmId: String, action: String, cycleId: String?, taskId: String? = null): String? {
    if (farmId.isBlank()) return null
    if (action == "supplies") return "inventory/$farmId"
    if (cycleId.isNullOrBlank()) return null
    val linkedTask = if (action in setOf("watering", "fertilizing") && !taskId.isNullOrBlank())
        "&taskId=" + java.net.URLEncoder.encode(taskId, "UTF-8") else ""
    return when (action) {
        "watering", "fertilizing", "inspection", "other" ->
            "monitor/$farmId/$cycleId?quick=$action$linkedTask"
        "harvest" -> "harvests/$farmId/$cycleId"
        else -> null
    }
}
