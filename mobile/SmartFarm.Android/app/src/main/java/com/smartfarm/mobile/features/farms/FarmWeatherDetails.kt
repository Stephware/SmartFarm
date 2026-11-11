package com.smartfarm.mobile.features.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
internal fun FarmWeatherDetails(state: FarmUiState) {
    val weather = state.weather
    FarmSectionHeader("Saved farm forecast", "Forecast dates use your farm's time zone. This is not live weather.")
    if (state.weatherOffline) FarmStatusChip(
        "Offline copy — reconnect to check new warnings", FarmStatusTone.Warning)
    when {
        weather == null -> FarmEmptyState("Forecast unavailable",
            state.weatherMessage ?: "Reconnect and reopen your farm for saved weather.")
        !weather.hasCoordinates -> FarmEmptyState("No farm location pin",
            "An owner or manager can add the farm coordinates in Settings.")
        else -> {
            val stale = WeatherFreshness.isStale(weather.days)
            if (stale) FarmStatusChip("Saved weather older than 12 hours or invalid",
                FarmStatusTone.Warning)
            val date = runCatching { LocalDate.now(ZoneId.of(weather.timeZoneId)) }.getOrNull()
            if (date == null) Text("Farm time zone invalid — check Settings. Forecast dates are shown as saved.",
                color = MaterialTheme.colorScheme.error)
            val days = weather.days.sortedBy { it.date }
            val upcoming = days.firstOrNull { day ->
                val parsed = runCatching { LocalDate.parse(day.date) }.getOrNull()
                parsed != null && date != null && !parsed.isBefore(date)
            }
            FarmSectionCard("Forecast summary", subtitle = "Saved predictions, not measured conditions.") {
                if (upcoming != null) {
                    Text(upcoming.date, style = MaterialTheme.typography.titleMedium)
                    Text("${upcoming.minTemperatureC?.toPlainString() ?: "—"}–${upcoming.maxTemperatureC?.toPlainString() ?: "—"} °C",
                        style = MaterialTheme.typography.headlineSmall)
                    Text("Rain chance: ${upcoming.maxPrecipitationProbability?.toPlainString() ?: "—"}% · " +
                        "Expected rainfall: ${upcoming.rainfallMm?.toPlainString() ?: "—"} mm")
                } else {
                    Text("No forecast from today onward. Existing saved dates are listed below.")
                }
                Text("${weather.advisories.count { it.status == 0 }} active saved advisories",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Check official current forecasts before weather-sensitive fieldwork.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FarmSectionHeader("Forecast by day", "Sorted dates, with source and last-check time.")
            if (days.isEmpty()) FarmEmptyState("No forecast days",
                "Reconnect and reopen the farm after the weather worker refreshes.")
            days.forEach { day ->
                FarmSectionCard(day.date, subtitle = "Source: ${day.provider}") {
                    if (upcoming?.date == day.date) {
                        FarmStatusChip("Next available forecast", FarmStatusTone.Planned)
                    }
                    Text("${day.minTemperatureC?.toPlainString() ?: "—"}–${day.maxTemperatureC?.toPlainString() ?: "—"} °C",
                        style = MaterialTheme.typography.titleLarge)
                    Text("Rain: ${day.rainfallMm?.toPlainString() ?: "—"} mm · " +
                        "Chance: ${day.maxPrecipitationProbability?.toPlainString() ?: "—"}%")
                    Text("Max wind: ${day.maxWindKph?.toPlainString() ?: "—"} km/h")
                    Text("Last checked: ${day.retrievedAtUtc} UTC",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FarmSectionHeader("Saved advisories", "Not a substitute for official current warnings.")
            val advisories = weather.advisories.filter { it.status == 0 }
            if (advisories.isEmpty()) FarmEmptyState("No active saved advisories",
                "This does not rule out current hazards.")
            advisories.forEach { a ->
                FarmSectionCard("Advisory · ${a.date}") {
                    FarmStatusChip("Severity ${a.severity}", FarmStatusTone.Warning)
                    Text(a.message)
                }
            }
        }
    }
}
