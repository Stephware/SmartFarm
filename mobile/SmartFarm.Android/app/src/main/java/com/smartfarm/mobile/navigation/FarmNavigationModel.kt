package com.smartfarm.mobile.navigation

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
    route == "login" -> uiText(R.string.ui_log_in_c189840, "Log in")
    route == "register" -> uiText(R.string.ui_create_account_798ca2c, "Create account")
    route == "forgot-password" -> uiText(R.string.ui_password_help_aea062c, "Password help")
    route == "account" -> uiText(R.string.ui_my_account_b53181a, "My account")
    route == "farms" -> uiText(R.string.ui_my_farms_0517a09, "My farms")
    route == "notifications" -> uiText(R.string.ui_notifications_7880118, "Notifications")
    route == "sync" -> uiText(R.string.ui_offline_sync_843fdb9, "Offline sync")
    route == "tasks/{farmId}" -> uiText(R.string.ui_farm_tasks_136a660, "Farm tasks")
    route == "crops/{farmId}" -> uiText(R.string.ui_crops_0abd7c5, "Crops")
    route == "inventory/{farmId}" -> uiText(R.string.ui_supplies_6be7560, "Supplies")
    route == "inventory/{farmId}/{itemId}" -> uiText(R.string.ui_supply_details_734f9d1, "Supply details")
    route == "insights/{farmId}" -> uiText(R.string.ui_harvest_summary_2a05eb0, "Harvest summary")
    route?.startsWith("harvests/") == true -> uiText(R.string.ui_harvests_a2b3922, "Harvests")
    route?.startsWith("monitor/") == true -> when (quick) {
        "watering" -> uiText(R.string.ui_record_watering_d15d0cc, "Record watering"); "fertilizing" -> uiText(R.string.ui_record_fertilizing_db7f592, "Record fertilizing")
        "inspection" -> uiText(R.string.ui_record_inspection_9a13794, "Record inspection"); "other" -> uiText(R.string.ui_record_activity_423d9be, "Record activity"); else -> uiText(R.string.ui_crop_monitoring_d14cf4e, "Crop monitoring")
    }
    route == "farm/{farmId}/{section}" -> when (safeFarmSection(section)) {
        "plots" -> uiText(R.string.ui_farm_plots_bfad5ec, "Farm plots"); "members" -> uiText(R.string.ui_team_access_c3b54bb, "Team & access"); "settings" -> uiText(R.string.ui_farm_settings_df0ed3a, "Farm settings")
        "weather" -> uiText(R.string.ui_farm_weather_8a3ec1b, "Farm weather"); else -> uiText(R.string.ui_today_2b065c7, "Today")
    }
    route == "myfarm/{farmId}" -> uiText(R.string.ui_my_farm_b2e9f88, "My Farm")
    route == "farm/{farmId}" -> uiText(R.string.ui_today_2b065c7, "Today")
    // Missing-history parents are concrete routes rather than destination templates.
    route?.startsWith("crops/") == true -> uiText(R.string.ui_crops_0abd7c5, "Crops")
    route?.startsWith("inventory/") == true -> uiText(R.string.ui_supplies_6be7560, "Supplies")
    route?.startsWith("farm/") == true -> uiText(R.string.ui_today_2b065c7, "Today")
    else -> uiText(R.string.ui_welcome_0e2226b, "Welcome")
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
