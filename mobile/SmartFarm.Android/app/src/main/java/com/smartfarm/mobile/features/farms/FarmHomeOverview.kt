package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
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

private val taskTypes get() = listOf(uiText(R.string.ui_task_4bc74b2, "Task"), uiText(R.string.ui_planting_7a7418f, "Planting"), uiText(R.string.ui_watering_2ee1114, "Watering"), uiText(R.string.ui_fertilizing_3cc00f9, "Fertilizing"),
    uiText(R.string.ui_inspection_6e4fa13, "Inspection"), uiText(R.string.ui_harvest_c91ab8b, "Harvest"), uiText(R.string.ui_other_f97e9da, "Other"))

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
        ?: uiText(R.string.ui_farm_date_unavailable_23e602c, "Farm date unavailable")

    FarmSectionHeader(uiText(R.string.ui_your_work_today_80e250a, "Your work today"), "$dateText · ${farm.timeZoneId}")

    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRecord,
            modifier = Modifier.weight(1f).widthIn(min = 160.dp).defaultMinSize(minHeight = 52.dp)) {
            Text(uiText(R.string.ui_record_fieldwork_797a75c, "Record fieldwork"))
        }
        OutlinedButton(onClick = onTasks,
            modifier = Modifier.weight(1f).widthIn(min = 160.dp).defaultMinSize(minHeight = 52.dp)) {
            Text(uiText(R.string.ui_my_tasks_1f7f5b8, "My tasks"))
        }
    }

    // Secondary shortcuts use a single quiet surface instead of three full buttons.
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(uiText(R.string.ui_water_7ca7dea, "Water") to "watering", uiText(R.string.ui_inspect_e0723a8, "Inspect") to "inspection",
                uiText(R.string.ui_harvest_c91ab8b, "Harvest") to "harvest").forEach { (label, action) ->
                TextButton(onClick = { onRecordAction(action) },
                    modifier = Modifier.weight(1f).widthIn(min = 112.dp).defaultMinSize(minHeight = 48.dp)) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // Only actionable alerts appear here. Weather warnings are explicitly saved, not live.
    val hasAlerts = (work?.overdue?.isNotEmpty() == true) ||
        savedAdvisories > 0 || syncSummary.needsReview > 0
    if (hasAlerts) {
        FarmSectionCard(uiText(R.string.ui_needs_attention_c1ebc78, "Needs attention"), subtitle = uiText(R.string.ui_check_these_before_heading_into_the_field_9427c7c, "Check these before heading into the field.")) {
            if (work?.overdue?.isNotEmpty() == true) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(uiText(R.string.ui_1_s_overdue_2_s_afe6a50, "%1\$s overdue %2\$s", (work.overdue.size), (if (work.overdue.size == 1) "task" else "tasks")),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    FarmStatusChip(uiText(R.string.ui_overdue_d660945, "Overdue"), FarmStatusTone.Warning)
                }
                OutlinedButton(onClick = onTasks, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_review_overdue_tasks_200daad, "Review overdue tasks"))
                }
            }
            if (syncSummary.needsReview > 0) {
                Text(uiText(R.string.ui_1_s_saved_2_s_review_96f071c, "%1\$s saved %2\$s review", (syncSummary.needsReview), (if (syncSummary.needsReview == 1) "record needs" else "records need")),
                    style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_review_sync_issues_1108ae0, "Review sync issues"))
                }
            }
            if (savedAdvisories > 0) {
                Text(uiText(R.string.ui_1_s_saved_weather_2_s_eadea64, "%1\$s saved weather %2\$s", (savedAdvisories), (if (savedAdvisories == 1) "advisory" else "advisories")),
                    style = MaterialTheme.typography.titleMedium)
                Text(uiText(R.string.ui_these_may_be_outdated_check_official_current_warning_8baebe2, "These may be outdated. Check official current warnings before fieldwork."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onWeather, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_review_saved_advisories_5271105, "Review saved advisories"))
                }
            }
        }
    }

    FarmSectionCard(uiText(R.string.ui_today_s_tasks_0ad22a4, "Today's tasks"),
        subtitle = uiText(R.string.ui_due_work_and_the_next_assignments_cdbe9ad, "Due work and the next assignments.")) {
        when {
            state.offline -> {
                Text(uiText(R.string.ui_tasks_need_a_connection_987d9a6, "Tasks need a connection"), style = MaterialTheme.typography.titleMedium)
                Text(uiText(R.string.ui_reconnect_to_load_current_assignments_offline_field__264b05c, "Reconnect to load current assignments. Offline field records can still be saved."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            tasks?.farmId != farm.id || tasks.busy -> Text(uiText(R.string.ui_loading_today_s_work_16b20be, "Loading today's work…"))
            tasks.error != null -> {
                Text(uiText(R.string.ui_tasks_unavailable_connect_and_refresh_to_see_current_85f55e6, "Tasks unavailable. Connect and refresh to see current assignments."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            work == null -> Text(uiText(R.string.ui_farm_date_or_task_list_unavailable_ask_an_owner_to_c_2bc1a32, "Farm date or task list unavailable. Ask an owner to check the time zone."))
            work.dueCount == 0 -> {
                if (work.upcoming.isNotEmpty()) {
                    val next = work.upcoming.first()
                    Text(uiText(R.string.ui_nothing_due_today_2adbd86, "Nothing due today"), style = MaterialTheme.typography.titleMedium)
                    Text(uiText(R.string.ui_next_1_s_2_s_87e33bb, "Next: %1\$s · %2\$s", (taskTypes.getOrElse(next.taskType) { uiText(R.string.ui_task_4bc74b2, "Task") }), (next.dueDate)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(uiText(R.string.ui_no_tasks_due_today_6679b23, "No tasks due today"), style = MaterialTheme.typography.titleMedium)
                    Text(if (state.plots.isEmpty()) uiText(R.string.ui_set_up_your_growing_areas_to_begin_a65be6a, "Set up your growing areas to begin.")
                        else uiText(R.string.ui_you_re_caught_up_open_tasks_to_see_assignments_159545a, "You're caught up. Open Tasks to see assignments."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            Text(taskTypes.getOrElse(task.taskType) { uiText(R.string.ui_field_task_b52c32b, "Field task") },
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
                            Text(uiText(R.string.ui_due_1_s_1207d23, "Due %1\$s", (task.dueDate)), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FarmStatusChip(if (isLate) uiText(R.string.ui_overdue_d660945, "Overdue") else uiText(R.string.ui_today_2b065c7, "Today"),
                            if (isLate) FarmStatusTone.Warning else FarmStatusTone.Active)
                    }
                    val canRecordTask = task.taskType in 2..5 && currentCycles.any {
                        it.id == task.cropCycleId && it.status == 1
                    }
                    OutlinedButton(onClick = { if (canRecordTask) onTask(task) else onTasks() },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(if (canRecordTask) uiText(R.string.ui_record_1_s_0dca5e7, "Record %1\$s", (taskTypes[task.taskType].lowercase())) else uiText(R.string.ui_view_task_01444a2, "View task"))
                    }
                }
                if (work.dueCount > 3) {
                    Text(uiText(R.string.ui_and_1_s_more_tasks_due_485a86a, "And %1\$s more tasks due.", (work.dueCount - 3)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TextButton(onClick = onTasks,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text(uiText(R.string.ui_open_tasks_87cfa1a, "Open tasks"))
        }
    }

    if (syncSummary.pending > 0) {
        FarmSectionCard(uiText(R.string.ui_saved_on_this_phone_4c03b51, "Saved on this phone"),
            subtitle = uiText(R.string.ui_local_records_are_not_yet_confirmed_on_the_server_24ad767, "Local records are not yet confirmed on the server.")) {
            FarmStatusChip(uiText(R.string.ui_1_s_waiting_to_sync_f65c057, "%1\$s waiting to sync", (syncSummary.pending)),
                if (syncSummary.needsReview > 0) FarmStatusTone.Warning else FarmStatusTone.Planned)
            if (syncSummary.needsReview > 0) {
                Text(uiText(R.string.ui_1_s_need_attention_before_syncing_f9d5f77, "%1\$s need attention before syncing.", (syncSummary.needsReview)),
                    color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text(uiText(R.string.ui_view_sync_status_dd6be1c, "View sync status"))
            }
        }
    }

    if (localForecast != null) {
        val day = localForecast.days.filter { day ->
            val forecastDate = runCatching { LocalDate.parse(day.date) }.getOrNull()
            forecastDate != null && (today == null || !forecastDate.isBefore(today))
        }.minByOrNull { it.date }
        FarmSectionCard(uiText(R.string.ui_saved_weather_85786c4, "Saved weather"), subtitle = uiText(R.string.ui_forecast_not_live_observations_6de5bb1, "Forecast, not live observations.")) {
            if (day == null) {
                Text(uiText(R.string.ui_no_upcoming_saved_forecast_353ab1d, "No upcoming saved forecast."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("${day.minTemperatureC?.toPlainString() ?: "—"}–" +
                    "${day.maxTemperatureC?.toPlainString() ?: "—"} °C · " +
                    uiText(R.string.ui_1_s_rain_chance_89e8e01, "%1\$s%% rain chance", (day.maxPrecipitationProbability?.toPlainString() ?: "—")),
                    style = MaterialTheme.typography.titleMedium)
                Text(uiText(R.string.ui_forecast_for_1_s_1f2194b, "Forecast for %1\$s", (day.date)), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.weatherOffline || WeatherFreshness.isStale(localForecast.days)) {
                    FarmStatusChip(uiText(R.string.ui_saved_forecast_may_be_outdated_f92fb64, "Saved forecast may be outdated"), FarmStatusTone.Warning)
                }
            }
            OutlinedButton(onClick = onWeather, modifier = Modifier.fillMaxWidth()) {
                Text(uiText(R.string.ui_view_forecast_6c87311, "View forecast"))
            }
        }
    }
}
