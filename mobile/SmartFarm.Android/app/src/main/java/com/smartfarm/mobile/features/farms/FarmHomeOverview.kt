package com.smartfarm.mobile.features.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import com.smartfarm.mobile.ui.components.CropArtworkImage
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val taskTypes = listOf("Task", "Planting", "Watering", "Fertilizing",
    "Inspection", "Harvest", "Other")

internal fun farmHomeTaskPreview(
    state: TaskUiState?, farmId: String, actorId: String?, isOwner: Boolean, today: LocalDate?
): List<TaskDto>? {
    if (state == null || state.farmId != farmId || state.busy ||
        state.error != null || today == null) return null
    return TaskWorkCenter.myOpenTasks(state.tasks, actorId, isOwner)
}

/** Action-first farm Home. No local record is counted as completed server work. */
@Composable
internal fun FarmHomeOverview(
    state: FarmUiState, tasks: TaskUiState?, crops: CropUiState?, actorId: String?,
    onTasks: () -> Unit, onCrops: () -> Unit, onInventory: () -> Unit,
    onWeather: () -> Unit, onInsights: () -> Unit,
    onRecord: () -> Unit = {}, onPlots: () -> Unit = {},
    onSync: () -> Unit = {}, syncSummary: HomeSyncSummary = HomeSyncSummary(0, 0)
) {
    val farm = state.selected ?: return
    val today = runCatching { TaskWorkCenter.today(farm.timeZoneId) }.getOrNull()
    val myWork = if (state.offline) null else farmHomeTaskPreview(
        tasks, farm.id, actorId, farm.role == "Owner", today)
    val work = if (myWork != null && today != null) homeDailyWork(myWork, today) else null
    val visibleCrops = crops?.takeIf { it.farmId == farm.id && !it.busy && it.error == null }
    val currentCycles = visibleCrops?.cycles.orEmpty()
    val growing = currentCycles.filter { it.status == 1 }
    val savedAdvisories = state.weather?.advisories?.count { it.status == 0 } ?: 0
    val localForecast = state.weather?.takeIf { it.farmId == farm.id && it.hasCoordinates }
    val dateText = today?.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        ?: "Farm date unavailable"

    FarmSectionHeader("Your work today", "$dateText · ${farm.timeZoneId}")

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onRecord,
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp)) {
            Text("Record fieldwork")
        }
        OutlinedButton(onClick = onTasks,
            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp)) {
            Text("My tasks")
        }
    }

    // Only actionable alerts appear here. Weather warnings are explicitly saved, not live.
    val hasAlerts = (work?.overdue?.isNotEmpty() == true) ||
        savedAdvisories > 0 || syncSummary.needsReview > 0
    if (hasAlerts) {
        FarmSectionCard("Needs attention", subtitle = "Check these before heading into the field.") {
            if (work?.overdue?.isNotEmpty() == true) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${work.overdue.size} overdue ${if (work.overdue.size == 1) "task" else "tasks"}",
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FarmStatusChip("Overdue", FarmStatusTone.Warning)
                }
                OutlinedButton(onClick = onTasks, modifier = Modifier.fillMaxWidth()) {
                    Text("Review overdue tasks")
                }
            }
            if (syncSummary.needsReview > 0) {
                Text("${syncSummary.needsReview} saved ${if (syncSummary.needsReview == 1) "record needs" else "records need"} review",
                    style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                    Text("Review sync issues")
                }
            }
            if (savedAdvisories > 0) {
                Text("$savedAdvisories saved weather ${if (savedAdvisories == 1) "advisory" else "advisories"}",
                    style = MaterialTheme.typography.titleMedium)
                Text("These may be outdated. Check official current warnings before fieldwork.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onWeather, modifier = Modifier.fillMaxWidth()) {
                    Text("Review saved advisories")
                }
            }
        }
    }

    FarmSectionCard("Today's tasks",
        subtitle = "Your assignments and, for owners, unassigned farm work.") {
        when {
            state.offline -> {
                FarmEmptyState("Tasks need a connection",
                    "Reconnect to load current assignments. Offline field records can still be saved.")
            }
            tasks?.farmId != farm.id || tasks.busy -> Text("Loading today's work…")
            tasks.error != null -> {
                Text("Tasks unavailable. Connect and refresh to see current assignments.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            work == null -> Text("Farm date or task list unavailable. Ask an owner to check the time zone.")
            work.dueCount == 0 -> {
                if (work.upcoming.isNotEmpty()) {
                    val next = work.upcoming.first()
                    Text("Nothing due today", style = MaterialTheme.typography.titleMedium)
                    Text("Next: ${taskTypes.getOrElse(next.taskType) { "Task" }} · ${next.dueDate}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FarmEmptyState("No tasks due today",
                        if (state.plots.isEmpty()) "Set up your growing areas to begin."
                        else "You're caught up. Open Tasks to see assignments.")
                }
            }
            else -> {
                val due = (work.overdue + work.dueToday).take(3)
                due.forEach { task ->
                    val isLate = TaskWorkCenter.dueDate(task)?.isBefore(today) == true
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(taskTypes.getOrElse(task.taskType) { "Field task" },
                                style = MaterialTheme.typography.titleMedium)
                            task.description?.takeIf { it.isNotBlank() }?.let {
                                Text(it, maxLines = 2,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            val crop = currentCycles.firstOrNull { it.id == task.cropCycleId }
                            if (crop != null) {
                                Text("${crop.cropTypeName} · ${crop.plotName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Due ${task.dueDate}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FarmStatusChip(if (isLate) "Overdue" else "Today",
                            if (isLate) FarmStatusTone.Warning else FarmStatusTone.Active)
                    }
                }
                if (work.dueCount > 3) {
                    Text("And ${work.dueCount - 3} more tasks due.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        OutlinedButton(onClick = onTasks,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text("Open tasks")
        }
    }

    if (syncSummary.pending > 0) {
        FarmSectionCard("Saved on this phone",
            subtitle = "Local records are not yet confirmed on the server.") {
            FarmStatusChip("${syncSummary.pending} waiting to sync",
                if (syncSummary.needsReview > 0) FarmStatusTone.Warning else FarmStatusTone.Planned)
            if (syncSummary.needsReview > 0) {
                Text("${syncSummary.needsReview} need attention before syncing.",
                    color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text("View sync status")
            }
        }
    }

    // Keep previews small. Never show a dashboard filled with zero-value statistic tiles.
    if (state.plots.isNotEmpty()) {
        FarmSectionCard("My farm plots",
            subtitle = "A quick look at your growing areas; illustrations aren't geographic.") {
            state.plots.take(2).forEach { plot ->
                val crop = currentCycles.firstOrNull { it.plotId == plot.id && it.status == 1 }
                    ?: currentCycles.firstOrNull { it.plotId == plot.id && it.status == 0 }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (crop != null) {
                        CropArtworkImage(crop.cropTypeName, Modifier.size(58.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(plot.name, style = MaterialTheme.typography.titleMedium)
                        Text(crop?.let { "${it.cropTypeName} · ${it.statusName}" } ?: "No current crop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${plot.areaSqMeters} m²", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.plots.size > 2) {
                Text("+${state.plots.size - 2} more growing areas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPlots, modifier = Modifier.weight(1f)) {
                    Text("All plots")
                }
                OutlinedButton(onClick = onCrops, modifier = Modifier.weight(1f)) {
                    Text("Manage crops")
                }
            }
        }
    } else if (farm.role != "Owner" && farm.role != "Manager") {
        FarmEmptyState("No growing areas yet",
            "Ask a farm owner or manager to add a plot before recording crop work.")
    }

    if (localForecast != null) {
        val day = localForecast.days.filter { day ->
            val forecastDate = runCatching { LocalDate.parse(day.date) }.getOrNull()
            forecastDate != null && (today == null || !forecastDate.isBefore(today))
        }.minByOrNull { it.date }
        FarmSectionCard("Saved weather", subtitle = "Forecast, not live observations.") {
            if (day == null) {
                Text("No upcoming saved forecast.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${day.minTemperatureC?.toPlainString() ?: "—"}–" +
                    "${day.maxTemperatureC?.toPlainString() ?: "—"} °C · " +
                    "${day.maxPrecipitationProbability?.toPlainString() ?: "—"}% rain chance",
                    style = MaterialTheme.typography.titleMedium)
                Text("Forecast for ${day.date}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.weatherOffline || WeatherFreshness.isStale(localForecast.days)) {
                    FarmStatusChip("Saved forecast may be outdated", FarmStatusTone.Warning)
                }
            }
            OutlinedButton(onClick = onWeather, modifier = Modifier.fillMaxWidth()) {
                Text("View forecast")
            }
        }
    }
}
