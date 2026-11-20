package com.smartfarm.mobile.features.farms
import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText
/** Display mapping only; submitted role, status and section codes stay unchanged. */
internal fun farmRequestStatusLabel(status: String): String = when(status) {
    "Pending" -> uiText(R.string.ui_pending_331551b, "Pending")
    "Approved" -> uiText(R.string.ui_approved_87b42e4, "Approved")
    "Rejected" -> uiText(R.string.ui_rejected_aea4a04, "Rejected")
    "Canceled" -> uiText(R.string.ui_canceled_13ca2ee, "Canceled")
    "Cancelled" -> uiText(R.string.ui_cancelled_d353a99, "Cancelled")
    else -> status
}
internal fun farmSectionLabel(section: String): String = when(section) {
    "Weather" -> uiText(R.string.ui_farm_weather_8a3ec1b, "Farm weather")
    "Plots" -> uiText(R.string.ui_farm_plots_bfad5ec, "Farm plots")
    "Members" -> uiText(R.string.ui_farm_team_8277784, "Farm team")
    "Settings" -> uiText(R.string.ui_farm_settings_df0ed3a, "Farm settings")
    "Restricted" -> uiText(R.string.ui_farm_settings_unavailable_4f8394a, "Farm settings unavailable")
    else -> uiText(R.string.ui_today_2b065c7, "Today")
}
