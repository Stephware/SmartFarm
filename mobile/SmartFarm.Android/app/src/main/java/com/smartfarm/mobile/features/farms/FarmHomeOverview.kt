package com.smartfarm.mobile.features.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmMetricCard
import com.smartfarm.mobile.ui.components.FarmPlotPreview
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.data.remote.api.TaskDto
import java.time.LocalDate

private val taskTypes = listOf("Task", "Planting", "Watering", "Fertilizing",
    "Inspection", "Harvest", "Other")

internal fun farmHomeTaskPreview(
    state: TaskUiState?, farmId: String, actorId: String?, isOwner: Boolean, today: LocalDate?
): List<TaskDto>? {
    if (state == null || state.farmId != farmId || state.busy ||
        state.error != null || today == null) return null
    return TaskWorkCenter.myOpenTasks(state.tasks, actorId, isOwner)
}

/** A read-only daily overview. All forms and crop sync remain on the original screens. */
@Composable
internal fun FarmHomeOverview(
    state: FarmUiState, tasks: TaskUiState?, crops: CropUiState?, actorId: String?,
    onTasks: () -> Unit, onCrops: () -> Unit, onInventory: () -> Unit,
    onWeather: () -> Unit, onInsights: () -> Unit
) {
    val farm = state.selected ?: return
    val today = runCatching { TaskWorkCenter.today(farm.timeZoneId) }.getOrNull()
    val myWork = if (state.offline) null else farmHomeTaskPreview(tasks, farm.id, actorId, farm.role == "Owner", today)
    val dueToday = myWork?.count { TaskWorkCenter.dueDate(it) == today }
    val overdue = myWork?.count { TaskWorkCenter.dueDate(it)?.isBefore(today) == true }
    val visibleCrops = crops?.takeIf { it.farmId == farm.id && !it.busy && it.error == null }
    val activeCrops = visibleCrops?.cycles?.count { it.status == 1 }

    FarmSectionHeader("Today on your farm",
        if (today != null) "Farm date: " + today + " · " + farm.timeZoneId
        else "Farm time zone unavailable. Review farm settings.")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FarmMetricCard("Active plots", state.plots.size.toString(), "Growing areas",
                modifier = Modifier.weight(1f))
            FarmMetricCard("Active crops", activeCrops?.toString() ?: "—",
                if (visibleCrops?.offline == true) "Saved crop records" else "Currently planted",
                modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FarmMetricCard("My open tasks", myWork?.size?.toString() ?: "—",
                if (myWork == null) "Connect to load" else "Assigned to you",
                modifier = Modifier.weight(1f))
            FarmMetricCard("Overdue", overdue?.toString() ?: "—",
                if (dueToday == null) "Connect to load" else dueToday.toString() + " due today",
                modifier = Modifier.weight(1f))
        }
    }

    FarmSectionCard("Today's work", subtitle = "Your assignments due today or overdue.") {
        val due = myWork.orEmpty().filter { task ->
            val date = TaskWorkCenter.dueDate(task)
            today != null && date != null && !date.isAfter(today)
        }.take(3)
        when {
            state.offline -> Text("Tasks require a connection. Offline crop inspections remain available.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            tasks?.farmId != farm.id || tasks.busy -> Text("Loading tasks…")
            tasks.error != null -> Text("Tasks unavailable. Open Tasks to refresh.",
                color = MaterialTheme.colorScheme.error)
            myWork == null -> Text("Task dates unavailable. Check the farm time zone.")
            due.isEmpty() -> FarmEmptyState("Nothing due today",
                "Upcoming assignments are available in Tasks.")
            else -> due.forEach { task ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(taskTypes.getOrElse(task.taskType) { "Task" },
                            style = MaterialTheme.typography.titleMedium)
                        task.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        Text("Due " + task.dueDate, style = MaterialTheme.typography.bodySmall)
                    }
                    FarmStatusChip(if (TaskWorkCenter.dueDate(task)?.isBefore(today) == true)
                        "Overdue" else "Today",
                        if (TaskWorkCenter.dueDate(task)?.isBefore(today) == true)
                            FarmStatusTone.Warning else FarmStatusTone.Active)
                }
            }
        }
        Button(onClick = onTasks, modifier = Modifier.fillMaxWidth()) { Text("Open all tasks") }
    }

    FarmSectionCard("Growing areas",
        subtitle = "Schematic rows, not real plot geometry or crop health.") {
        if (state.plots.isEmpty()) {
            FarmEmptyState("No plots yet", "Add a plot in the Plots tab.")
        } else {
            state.plots.take(2).forEach { plot ->
                val crop = visibleCrops?.cycles?.firstOrNull {
                    it.plotId == plot.id && it.status == 1
                } ?: visibleCrops?.cycles?.firstOrNull {
                    it.plotId == plot.id && it.status == 0
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    FarmPlotPreview(cropName = crop?.cropTypeName)
                    Text(plot.name, style = MaterialTheme.typography.titleMedium)
                    Text((crop?.cropTypeName ?: if (visibleCrops == null) "Crop data unavailable" else "No current crop")
                        + " · " + plot.areaSqMeters + " m²",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(onClick = onCrops, modifier = Modifier.fillMaxWidth()) {
            Text("Manage crops and plots")
        }
    }

    FarmSectionCard("Saved weather", subtitle = "Forecast data, not live conditions.") {
        val weather = state.weather
        val date = today
        val day = weather?.days?.filter { d ->
            val parsed = runCatching { LocalDate.parse(d.date) }.getOrNull()
            parsed != null && (date == null || !parsed.isBefore(date))
        }?.minByOrNull { it.date }
        when {
            weather == null -> Text(state.weatherMessage ?: "Weather unavailable.")
            !weather.hasCoordinates -> Text("Set the farm's location pin to enable forecasts.")
            day == null -> Text("No saved forecast for today or later.")
            else -> {
                Text((day.minTemperatureC?.toPlainString() ?: "—") + "–" +
                    (day.maxTemperatureC?.toPlainString() ?: "—") + " °C",
                    style = MaterialTheme.typography.headlineSmall)
                Text("For " + day.date + " · " +
                    (day.maxPrecipitationProbability?.toPlainString() ?: "—") +
                    "% rain chance · " + (day.rainfallMm?.toPlainString() ?: "—") + " mm rain")
                if (state.weatherOffline || WeatherFreshness.isStale(weather.days)) {
                    Text("Saved data may be outdated. Check current conditions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                Text("Checked " + day.retrievedAtUtc + " UTC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (weather != null && weather.advisories.any { it.status == 0 }) {
            FarmStatusChip(weather.advisories.count { it.status == 0 }.toString() +
                " saved advisories", FarmStatusTone.Warning)
        }
        OutlinedButton(onClick = onWeather, modifier = Modifier.fillMaxWidth()) {
            Text("View full saved forecast")
        }
    }

    FarmSectionCard("Quick actions", subtitle = "Go directly to your fieldwork.") {
        OutlinedButton(onClick = onTasks, modifier = Modifier.fillMaxWidth()) { Text("Review tasks") }
        OutlinedButton(onClick = onCrops, modifier = Modifier.fillMaxWidth()) { Text("Crop monitoring") }
        OutlinedButton(onClick = onInventory, modifier = Modifier.fillMaxWidth()) { Text("Check inventory") }
        OutlinedButton(onClick = onInsights, modifier = Modifier.fillMaxWidth()) { Text("Harvest reports & finances") }
    }
}
