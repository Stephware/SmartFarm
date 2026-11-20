package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.farmDateTime
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.data.remote.dto.GuidanceMessageDto
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun FarmWeatherDetails(state: FarmUiState,
    cropState: CropUiState? = null, onGuidance: (String, String?) -> Unit = { _, _ -> }) {
    val weather = state.weather
    var selectingAdvisory by rememberSaveable(state.selected?.id) { mutableStateOf<String?>(null) }
    val availableCrops = cropState?.takeIf {
        it.farmId == state.selected?.id && !it.offline && !it.busy && it.error == null
    }?.cycles?.filter { it.status == 1 }.orEmpty()
    var showDailyForecast by rememberSaveable(state.selected?.id) { mutableStateOf(false) }
    FarmSectionHeader(uiText(R.string.ui_saved_farm_forecast_c4fd6d0, "Saved farm forecast"), uiText(R.string.ui_saved_predictions_not_live_measurements_6f71fa1, "Saved predictions, not live measurements."))
    if (state.weatherOffline) FarmStatusChip(
        uiText(R.string.ui_offline_copy_reconnect_to_check_new_warnings_b6ffeeb, "Offline copy — reconnect to check new warnings"), FarmStatusTone.Warning)
    when {
        weather == null -> FarmEmptyState(uiText(R.string.ui_forecast_unavailable_31f441c, "Forecast unavailable"),
            state.weatherMessage ?: uiText(R.string.ui_reconnect_and_reopen_your_farm_for_saved_weather_a3c4f51, "Reconnect and reopen your farm for saved weather."))
        !weather.hasCoordinates -> FarmEmptyState(uiText(R.string.ui_no_farm_location_pin_d6cab1e, "No farm location pin"),
            uiText(R.string.ui_an_owner_or_manager_can_add_the_farm_coordinates_in__bd531e6, "An owner or manager can add the farm coordinates in Settings."))
        else -> {
            val stale = WeatherFreshness.isStale(weather.days)
            if (stale) FarmStatusChip(uiText(R.string.ui_saved_weather_older_than_12_hours_or_invalid_2ddbe71, "Saved weather older than 12 hours or invalid"),
                FarmStatusTone.Warning)
            val date = runCatching { LocalDate.now(ZoneId.of(weather.timeZoneId)) }.getOrNull()
            if (date == null) Text(uiText(R.string.ui_farm_time_zone_invalid_check_settings_forecast_dates_8d25f03, "Farm time zone invalid — check Settings. Forecast dates are shown as saved."),
                color = MaterialTheme.colorScheme.error)
            val days = weather.days.sortedBy { it.date }
            val upcoming = days.firstOrNull { day ->
                val parsed = runCatching { LocalDate.parse(day.date) }.getOrNull()
                parsed != null && date != null && !parsed.isBefore(date)
            }
            FarmSectionCard(uiText(R.string.ui_forecast_summary_ee11094, "Forecast summary")) {
                if (upcoming != null) {
                    Text(upcoming.date, style = MaterialTheme.typography.titleMedium)
                    Text(uiText(R.string.ui_source_1_s_800bc31, "Source: %1\$s", (upcoming.provider)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(uiText(R.string.ui_last_checked_1_s_7273969, "Last checked: %1\$s", (farmDateTime(upcoming.retrievedAtUtc, weather.timeZoneId))), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${upcoming.minTemperatureC?.toPlainString() ?: "—"}–${upcoming.maxTemperatureC?.toPlainString() ?: "—"} °C",
                        style = MaterialTheme.typography.headlineSmall)
                    Text(uiText(R.string.ui_rain_chance_1_s_dd62a68, "Rain chance: %1\$s%% · ", (upcoming.maxPrecipitationProbability?.toPlainString() ?: "—")) +
                        uiText(R.string.ui_expected_rainfall_1_s_mm_545cc86, "Expected rainfall: %1\$s mm", (upcoming.rainfallMm?.toPlainString() ?: "—")))
                } else {
                    Text(uiText(R.string.ui_no_forecast_from_today_onward_existing_saved_dates_a_f7e2df4, "No forecast from today onward. Existing saved dates are listed below."))
                }
                Text(uiText(R.string.ui_1_s_active_saved_advisories_12f6f3e, "%1\$s active saved advisories", (weather.advisories.count { it.status == 0 })),
                    style = MaterialTheme.typography.bodyMedium)
                Text(uiText(R.string.ui_check_official_current_forecasts_before_weather_sens_e7bf9ac, "Check official current forecasts before weather-sensitive fieldwork."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { showDailyForecast = !showDailyForecast }) {
                Text(if (showDailyForecast) uiText(R.string.ui_hide_daily_forecast_1b2d895, "Hide daily forecast") else uiText(R.string.ui_show_daily_forecast_d2b5d2c, "Show daily forecast"))
            }
            if (showDailyForecast) {
                if (days.isEmpty()) FarmEmptyState(uiText(R.string.ui_no_forecast_days_ac08085, "No forecast days"),
                    uiText(R.string.ui_reconnect_and_reopen_the_farm_after_the_weather_work_18796c8, "Reconnect and reopen the farm after the weather worker refreshes."))
                days.forEach { day ->
                    FarmSectionCard(day.date, subtitle = uiText(R.string.ui_source_1_s_800bc31, "Source: %1\$s", (day.provider))) {
                        if (upcoming?.date == day.date) {
                            FarmStatusChip(uiText(R.string.ui_next_available_forecast_d87d75c, "Next available forecast"), FarmStatusTone.Planned)
                        }
                        Text("${day.minTemperatureC?.toPlainString() ?: "—"}–${day.maxTemperatureC?.toPlainString() ?: "—"} °C",
                            style = MaterialTheme.typography.titleLarge)
                        Text(uiText(R.string.ui_rain_1_s_mm_0710e31, "Rain: %1\$s mm · ", (day.rainfallMm?.toPlainString() ?: "—")) +
                            uiText(R.string.ui_chance_1_s_945edfe, "Chance: %1\$s%%", (day.maxPrecipitationProbability?.toPlainString() ?: "—")))
                        Text(uiText(R.string.ui_max_wind_1_s_km_h_6324500, "Max wind: %1\$s km/h", (day.maxWindKph?.toPlainString() ?: "—")))
                        Text(uiText(R.string.ui_last_checked_1_s_7273969, "Last checked: %1\$s", (farmDateTime(day.retrievedAtUtc, weather.timeZoneId))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            FarmSectionHeader(uiText(R.string.ui_saved_advisories_acaab51, "Saved advisories"), uiText(R.string.ui_check_official_warnings_for_current_conditions_f4af3f4, "Check official warnings for current conditions."))
            val advisories = weather.advisories.filter { it.status == 0 }
            if (advisories.isEmpty()) FarmEmptyState(uiText(R.string.ui_no_active_saved_advisories_0b5b738, "No active saved advisories"),
                uiText(R.string.ui_this_does_not_rule_out_current_hazards_a4e8667, "This does not rule out current hazards."))
            advisories.forEach { a ->
                FarmSectionCard(uiText(R.string.ui_advisory_1_s_c1509d9, "Advisory · %1\$s", (a.date))) {
                    FarmStatusChip(uiText(R.string.ui_severity_1_s_31458b3, "Severity %1\$s", (a.severity)), FarmStatusTone.Warning)
                    Text(a.message)
                    TextButton(onClick = { selectingAdvisory = a.id },
                        enabled = !state.offline && !state.weatherOffline && !state.guidanceBusy) {
                        Text(uiText(R.string.guidance_explain, "Explain this advisory"))
                    }
                    if (state.guidanceAdvisoryId == a.id) {
                        if (state.guidanceBusy) CircularProgressIndicator()
                        state.guidanceError?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                        state.weatherGuidance?.let { guidance ->
                            val source = if (guidance.generationSource == "gemini")
                                uiText(R.string.guidance_source_ai, "AI-assisted guidance from approved recommendations.")
                            else uiText(R.string.guidance_source_fallback,
                                "Rule-based guidance (AI unavailable or not appropriate).")
                            Text(source, style = MaterialTheme.typography.labelMedium)
                            if (guidance.isStale) Text(uiText(R.string.guidance_stale,
                                "The forecast is old. Check official current forecasts."))
                            if (guidance.isSynthetic) Text(uiText(R.string.guidance_simulated,
                                "Demonstration forecast, not a real weather warning."))
                            Text(uiText(R.string.guidance_explanations, "What the forecast means"),
                                style = MaterialTheme.typography.titleMedium)
                            guidance.explanations.forEach { message ->
                                Text("• " + weatherGuidanceText(message))
                            }
                            Text(uiText(R.string.guidance_recommendations, "Suggested next steps"),
                                style = MaterialTheme.typography.titleMedium)
                            guidance.recommendations.forEach { message ->
                                Text("• " + weatherGuidanceText(message))
                            }
                            Text(uiText(R.string.guidance_informational,
                                "Advice only. AniGo cannot change farm records automatically."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    selectingAdvisory?.let { advisoryId ->
        AlertDialog(onDismissRequest = { selectingAdvisory = null },
            title = { Text(uiText(R.string.guidance_choose_crop, "Choose crop context (optional)")) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        selectingAdvisory = null
                        onGuidance(advisoryId, null)
                    }) { Text(uiText(R.string.guidance_general, "General farm guidance")) }
                    availableCrops.forEach { crop ->
                        TextButton(onClick = {
                            selectingAdvisory = null
                            onGuidance(advisoryId, crop.id)
                        }) { Text(crop.cropTypeName + " — " + crop.plotName) }
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { selectingAdvisory = null }) {
                    Text(uiText(R.string.ui_close_7d9eb7a, "Close"))
                }
            })
    }
}

@Composable
private fun weatherGuidanceText(message: GuidanceMessageDto): String = when (message.key) {
    "rain.roots" -> uiText(R.string.guidance_key_rain_roots,
        "Forecast heavy rain may leave soil waterlogged and affect roots. Check actual field conditions before deciding what to do.")
    "wind.damage" -> uiText(R.string.guidance_key_wind_damage,
        "Forecast strong wind may damage crops and equipment. Assess exposure before working outdoors.")
    "heat.stress" -> uiText(R.string.guidance_key_heat_stress,
        "Forecast heat may increase crop stress. It does not establish that damage has occurred on this farm.")
    "drainage.check" -> uiText(R.string.guidance_key_drainage_check,
        "Check drainage and areas that may collect water when it is safe to do so.")
    "supports.check" -> uiText(R.string.guidance_key_supports_check,
        "Check crop supports and secure loose equipment before stronger winds arrive.")
    "fieldwork.review" -> uiText(R.string.guidance_key_fieldwork_review,
        "Review whether weather-sensitive work should be postponed. Any changes remain your decision.")
    "stress.inspect" -> uiText(R.string.guidance_key_stress_inspect,
        "Inspect actual crop conditions and record what you observe. Do not assume damage from the forecast alone.")
    "water.check" -> uiText(R.string.guidance_key_water_check,
        "Check soil moisture before deciding about watering. No precise water amount is prescribed here.")
    else -> message.text
}
