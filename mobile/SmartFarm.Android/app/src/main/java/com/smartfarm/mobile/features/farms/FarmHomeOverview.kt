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
    onTasks: () -> Unit, onWeather: () -> Unit,
    onTask: (TaskDto) -> Unit = {},
    onRecord: () -> Unit = {}, onRecordAction: (String) -> Unit = {},
    onSync: () -> Unit = {}, syncSummary: HomeSyncSummary = HomeSyncSummary(0, 0)
) {
    val farm = state.selected ?: return
    val today = runCatching { TaskWorkCenter.today(farm.timeZoneId) }.getOrNull()
    val myWork = if (state.offline) null else farmHomeTaskPreview(
        tasks, farm.id, actorId, farm.role == "Owner", today)
    val work = if (myWork != null && today != null) homeDailyWork(myWork, today) else null
    val visibleCrops = crops?.takeIf { it.farmId == farm.id && !it.busy && it.error == null }
    val currentCycles = visibleCrops?.cycles.orEmpty()
    val localWeather = state.weather?.takeIf { it.farmId == farm.id }
    val savedAdvisories = localWeather?.advisories?.count { it.status == 0 } ?: 0
    val localForecast = localWeather?.takeIf { it.hasCoordinates }
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

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Water" to "watering", "Inspect" to "inspection", "Harvest" to "harvest")
            .forEach { (label, action) ->
                OutlinedButton(onClick = { onRecordAction(action) },
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                    Text(label)
                }
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
                    val isLate = today?.let { TaskWorkCenter.dueDate(task)?.isBefore(it) } == true
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
                    val canRecordTask = task.taskType in 2..5 && currentCycles.any {
                        it.id == task.cropCycleId && it.status == 1
                    }
                    OutlinedButton(onClick = { if (canRecordTask) onTask(task) else onTasks() },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(if (canRecordTask) "Record ${taskTypes[task.taskType].lowercase()}" else "View task")
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
