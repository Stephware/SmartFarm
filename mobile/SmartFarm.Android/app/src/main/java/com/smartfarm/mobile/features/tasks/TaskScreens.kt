package com.smartfarm.mobile.features.tasks

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.ui.components.UnsavedChanges
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.ZoneOffset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.api.TaskCreateInput
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.TaskUpdateInput
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.data.remote.dto.FarmMemberDto
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmLoadingState
import com.smartfarm.mobile.ui.components.FarmErrorState
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.DateInput
import java.time.LocalDate

private val taskNames get() = listOf(uiText(R.string.ui_unknown_b764cdc, "Unknown"), uiText(R.string.ui_planting_7a7418f, "Planting"), uiText(R.string.ui_watering_2ee1114, "Watering"), uiText(R.string.ui_fertilizing_3cc00f9, "Fertilizing"),
    uiText(R.string.ui_inspection_6e4fa13, "Inspection"), uiText(R.string.ui_harvest_c91ab8b, "Harvest"), uiText(R.string.ui_other_f97e9da, "Other"))
private val statuses get() = listOf(uiText(R.string.ui_pending_331551b, "Pending"), uiText(R.string.ui_completed_22a970d, "Completed"), uiText(R.string.ui_skipped_12698ce, "Skipped"), uiText(R.string.ui_cancelled_d353a99, "Cancelled"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    state: TaskUiState, canManage: Boolean, actorId: String?, onBack: () -> Unit,
    onRetry: () -> Unit, onComplete: (TaskDto) -> Unit, onRead: (String) -> Unit,
    isOwner: Boolean = false, onCreate: (TaskCreateInput) -> Unit = {},
    onUpdate: (TaskDto, TaskUpdateInput) -> Unit = { _, _ -> },
    onOpenCrop: (TaskDto) -> Unit = {}
) {
    var adding by rememberSaveable(state.farmId) { mutableStateOf(false) }
    var editingId by rememberSaveable(state.farmId) { mutableStateOf<String?>(null) }
    var selectedView by rememberSaveable(state.farmId) { mutableStateOf("today") }
    var selectedCalendarDay by rememberSaveable(state.farmId) { mutableStateOf<String?>(null) }
    var completedLimit by rememberSaveable(state.farmId) { mutableStateOf(20) }
    var manageLimit by rememberSaveable(state.farmId) { mutableStateOf(20) }
    var showTaskHelp by rememberSaveable(state.farmId) { mutableStateOf(false) }
    val formState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    var handledSave by rememberSaveable(state.farmId) { mutableStateOf(state.lastFormSave?.receipt) }
    val exitGuard = com.smartfarm.mobile.ui.components.LocalExitGuard.current
    LaunchedEffect(state.lastFormSave) {
        val saved = state.lastFormSave
        if (saved != null && saved.receipt != handledSave) {
            handledSave = saved.receipt
            exitGuard?.clear(saved.form)
            if (saved.form == "new-task") {
                adding = false
                formState.removeState("new-task")
            } else if (saved.form == "task-$editingId") {
                formState.removeState("edit-$editingId")
                editingId = null
            }
        }
    }
    val today = state.timeZoneId?.let { zone ->
        runCatching { TaskWorkCenter.today(zone) }.getOrNull()
    }
    // Compose DatePicker encodes calendar dates at UTC midnight; the farm's "today"
    // is still determined only by the configured farm time zone above.
    val calendarDate = selectedCalendarDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val assigned = TaskWorkCenter.myOpenTasks(state.tasks, actorId, isOwner)
    val overdue = today?.let { TaskWorkCenter.overdue(assigned, it) }.orEmpty()
    val dueToday = today?.let { TaskWorkCenter.dueToday(assigned, it) }.orEmpty()
    val upcoming = today?.let { TaskWorkCenter.upcoming(assigned, it) }.orEmpty()
    val closed = TaskDisplay.myHistory(state.tasks, actorId, isOwner)
    val isTasks = state.farmId != null
    val farmTasks = if (canManage) TaskDisplay.farmTasks(state.tasks) else emptyList()
    val hasTaskDraft = canManage && selectedView == "manage" && (adding || editingId != null)
    RecordPage(title = if (isTasks) uiText(R.string.ui_farm_tasks_136a660, "Farm tasks") else uiText(R.string.ui_notifications_7880118, "Notifications"), onBack = onBack) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FarmHero(if (isTasks) uiText(R.string.ui_my_tasks_1f7f5b8, "My tasks") else uiText(R.string.ui_notifications_7880118, "Notifications"),
                        if (isTasks) uiText(R.string.ui_today_s_work_and_upcoming_assignments_89aa771, "Today's work and upcoming assignments.")
                        else uiText(R.string.ui_task_reminders_and_farm_advisories_5073455, "Task reminders and farm advisories."),
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetry, enabled = !state.busy) { Text(uiText(R.string.ui_refresh_0e91610, "Refresh")) }
                }
                if (state.busy) FarmLoadingState(
                    when {
                        !isTasks -> if (state.notifications.isEmpty()) uiText(R.string.ui_loading_notifications_eae12e0, "Loading notifications…") else uiText(R.string.ui_updating_notifications_eb1cf1f, "Updating notifications…")
                        state.tasks.isEmpty() && !hasTaskDraft -> uiText(R.string.ui_loading_farm_tasks_bd691ec, "Loading farm tasks…")
                        else -> uiText(R.string.ui_updating_farm_tasks_dcdc3c0, "Updating farm tasks…")
                    })
                state.error?.let { FarmErrorState(com.smartfarm.mobile.localization.systemMessage(it), onRetry, canRetry = !state.busy) }
                state.notice?.let {
                    Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (isTasks) {
                    TextButton(onClick = { showTaskHelp = !showTaskHelp }) {
                        Text(if (showTaskHelp) uiText(R.string.ui_hide_task_help_5cb80a8, "Hide task help") else uiText(R.string.ui_about_task_updates_2cc3266, "About task updates"))
                    }
                    if (showTaskHelp) {
                        Text(uiText(R.string.ui_task_updates_need_internet_offline_field_logs_are_av_2601137, "Task updates need internet. Offline field logs are available from an active crop."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (isTasks && (state.error == null || state.tasks.isNotEmpty() || hasTaskDraft) &&
            (!state.busy || state.tasks.isNotEmpty() || hasTaskDraft)) {
            item(key = "task-tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("today" to uiText(R.string.ui_today_2b065c7, "Today"), "upcoming" to uiText(R.string.ui_upcoming_5f1a254, "Upcoming"),
                            "completed" to uiText(R.string.ui_completed_22a970d, "Completed")).forEach { (key, title) ->
                            FilterChip(selected = selectedView == key,
                                onClick = { selectedView = key },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text(title) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { selectedView = "calendar" },
                            modifier = Modifier.weight(1f)) {
                            Text(if (selectedView == "day") uiText(R.string.ui_change_date_e45ed0a, "Change date") else uiText(R.string.ui_calendar_d5d0a30, "Calendar"))
                        }
                        if (canManage) {
                            TextButton(onClick = { selectedView = "manage"; adding = false },
                                modifier = Modifier.weight(1f)) { Text(uiText(R.string.ui_all_farm_tasks_722654e, "All farm tasks")) }
                        }
                    }
                    if (canManage) {
                        OutlinedButton(onClick = {
                            selectedView = "manage"
                            adding = !adding
                            editingId = null
                        }, enabled = !state.busy && state.choicesReady,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (adding && selectedView == "manage") uiText(R.string.ui_close_new_task_form_c02fedc, "Close new task form")
                                 else uiText(R.string.ui_create_and_assign_task_f9d3766, "Create and assign task"))
                        }
                        state.choicesError?.let {
                            Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (selectedView == "today") {
                item(key = "today-summary") {
                    if (!state.busy && state.error == null) FarmSectionCard(uiText(R.string.ui_my_work_b4b9c68, "My work"), subtitle = if (today != null)
                        uiText(R.string.ui_farm_date_1_s_2_s_f60cf8e, "Farm date: %1\$s · %2\$s", (TaskDisplay.dueLabel(today.toString())), (state.timeZoneId))
                        else uiText(R.string.ui_farm_time_zone_unavailable_ask_an_owner_or_manager_t_4d541ae, "Farm time zone unavailable. Ask an owner or manager to check settings.")) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FarmStatusChip(uiText(R.string.ui_1_s_overdue_0c9ac3f, "%1\$s overdue", (overdue.size)),
                                if (overdue.isEmpty()) FarmStatusTone.Neutral else FarmStatusTone.Warning)
                            FarmStatusChip(uiText(R.string.ui_1_s_due_today_575905d, "%1\$s due today", (dueToday.size)), FarmStatusTone.Active)
                        }
                    }
                    if (today == null) {
                        FarmEmptyState(uiText(R.string.ui_farm_date_unavailable_23e602c, "Farm date unavailable"),
                            uiText(R.string.ui_reconnect_and_refresh_before_completing_dated_tasks_09ecd34, "Reconnect and refresh before completing dated tasks."))
                    } else if (!state.busy && state.error == null && overdue.isEmpty() && dueToday.isEmpty()) {
                        FarmEmptyState(uiText(R.string.ui_nothing_due_today_2adbd86, "Nothing due today"),
                            if (upcoming.isNotEmpty()) uiText(R.string.ui_your_next_assignment_is_1_s_0d6acc1, "Your next assignment is %1\$s.", (TaskDisplay.dueLabel(upcoming.first().dueDate)))
                            else uiText(R.string.ui_you_re_caught_up_check_upcoming_or_refresh_for_new_w_8366dbd, "You're caught up. Check Upcoming or refresh for new work."))
                    }
                }
                if (today != null) {
                    if (overdue.isNotEmpty()) item(key = "overdue-heading") {
                        FarmSectionHeader(uiText(R.string.ui_overdue_d660945, "Overdue"), uiText(R.string.ui_these_tasks_need_attention_first_f8f6a39, "These tasks need attention first."))
                    }
                    items(overdue.size, key = { "overdue-${overdue[it].id}" }) { index ->
                        TodayTaskCard(overdue[index], state, canManage, actorId, onComplete, onOpenCrop)
                    }
                    if (dueToday.isNotEmpty()) item(key = "due-today-heading") {
                        FarmSectionHeader(uiText(R.string.ui_due_today_19e8213, "Due today"), uiText(R.string.ui_today_s_assigned_fieldwork_de5a7e0, "Today's assigned fieldwork."))
                    }
                    items(dueToday.size, key = { "today-${dueToday[it].id}" }) { index ->
                        TodayTaskCard(dueToday[index], state, canManage, actorId, onComplete, onOpenCrop)
                    }
                }
            }
            if (selectedView == "upcoming") {
                if (today == null) item(key = "upcoming-date-error") {
                    FarmEmptyState(uiText(R.string.ui_farm_date_unavailable_23e602c, "Farm date unavailable"),
                        uiText(R.string.ui_reconnect_and_refresh_to_view_upcoming_tasks_bd6a7e5, "Reconnect and refresh to view upcoming tasks."))
                } else if (upcoming.isEmpty() && !state.busy && state.error == null) item(key = "upcoming-empty") {
                    FarmEmptyState(uiText(R.string.ui_no_upcoming_tasks_cdac756, "No upcoming tasks"),
                        uiText(R.string.ui_your_future_assignments_will_appear_here_35bc20a, "Your future assignments will appear here."))
                } else item(key = "upcoming-heading") {
                    FarmSectionHeader(uiText(R.string.ui_upcoming_5f1a254, "Upcoming"), uiText(R.string.ui_your_next_assigned_tasks_a0bc686, "Your next assigned tasks."))
                }
                if (today != null) items(upcoming.size, key = { "upcoming-${upcoming[it].id}" }) { index ->
                    TodayTaskCard(upcoming[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
            }
            if (selectedView == "completed") {
                if (closed.isEmpty() && !state.busy && state.error == null) item(key = "history-empty") {
                    FarmEmptyState(uiText(R.string.ui_no_completed_tasks_yet_56bf638, "No completed tasks yet"),
                        uiText(R.string.ui_your_completed_skipped_or_cancelled_assignments_will_5bc8d06, "Your completed, skipped or cancelled assignments will appear here."))
                } else item(key = "history-heading") {
                    FarmSectionHeader(uiText(R.string.ui_my_task_history_8d15a8f, "My task history"),
                        uiText(R.string.ui_your_closed_assignments_records_cannot_be_edited_her_3c5260d, "Your closed assignments. Records cannot be edited here."))
                }
                items(closed.take(completedLimit).size, key = { "history-${closed[it].id}" }) { index ->
                    TodayTaskCard(closed[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
                if (closed.size > completedLimit) item(key = "completed-more") {
                    OutlinedButton(onClick = { completedLimit += 20 },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(uiText(R.string.ui_show_20_more_completed_tasks_1_s_remaining_f0541c5, "Show 20 more completed tasks (%1\$s remaining)", (closed.size - completedLimit)))
                    }
                }
            }
            if (selectedView == "calendar") {
                item(key = "calendar") {
                    val picker = rememberDatePickerState(
                        initialDisplayedMonthMillis = (calendarDate ?: today)?.atStartOfDay(ZoneOffset.UTC)
                            ?.toInstant()?.toEpochMilli()
                    )
                    LaunchedEffect(picker.selectedDateMillis) {
                        picker.selectedDateMillis?.let { millis ->
                            selectedCalendarDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC).toLocalDate().toString()
                            selectedView = "day"
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FarmSectionHeader(uiText(R.string.ui_task_calendar_6dc0c41, "Task calendar"), uiText(R.string.ui_choose_a_date_to_see_that_day_s_assignments_4314ef2, "Choose a date to see that day's assignments."))
                        if (today != null) {
                            OutlinedButton(onClick = {
                                selectedCalendarDay = today.toString()
                                selectedView = "day"
                            }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text(uiText(R.string.ui_show_today_s_tasks_4aec0d9, "Show today's tasks"))
                            }
                        }
                        Surface(shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface) {
                            DatePicker(state = picker, modifier = Modifier.testTag("task-calendar-picker"),
                                showModeToggle = false,
                                title = null, headline = null,
                                colors = DatePickerDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surface))
                        }
                    }
                }
            }
            if (selectedView == "day") {
                item(key = "calendar-heading") {
                    FarmSectionHeader(uiText(R.string.ui_assignments_on_95aa08f, "Assignments on ") +
                        (calendarDate?.toString() ?: uiText(R.string.ui_selected_date_4e342f5, "selected date")),
                        uiText(R.string.ui_calendar_closed_after_selection_tap_change_date_to_p_60bdb65, "Calendar closed after selection. Tap Change date to pick another day."))
                }
                val calendarTasks = (if (canManage) farmTasks else (assigned + closed).distinctBy { it.id })
                    .filter { TaskWorkCenter.dueDate(it) == calendarDate }
                    .sortedWith(compareBy<TaskDto> { it.status != 0 }.thenBy { it.dueDate })
                if (calendarTasks.isEmpty() && !state.busy && state.error == null) item(key = "calendar-empty") {
                    FarmEmptyState(uiText(R.string.ui_no_assignments_on_this_date_20e0e75, "No assignments on this date"),
                        uiText(R.string.ui_choose_another_day_or_return_to_today_9c6e113, "Choose another day or return to Today."))
                }
                items(calendarTasks.size, key = { "calendar-${calendarTasks[it].id}" }) { index ->
                    TodayTaskCard(calendarTasks[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
            }
            if (selectedView == "manage" && canManage) {
                if (adding) item(key = "new-task") {
                    formState.SaveableStateProvider("new-task") { NewTaskForm(state, today, onCreate) }
                }
                item(key = "manage-heading") {
                    FarmSectionHeader(uiText(R.string.ui_all_farm_tasks_722654e, "All farm tasks"),
                        uiText(R.string.ui_manage_assignments_and_due_dates_existing_completed__fdbf9b2, "Manage assignments and due dates. Existing completed records are read-only."))
                }
                if (farmTasks.isEmpty() && !state.busy && state.error == null) item(key = "manage-empty") {
                    FarmEmptyState(uiText(R.string.ui_no_farm_tasks_yet_170a951, "No farm tasks yet"),
                        uiText(R.string.ui_create_a_task_above_to_assign_work_to_your_team_d2272be, "Create a task above to assign work to your team."))
                }
                farmTasks.take(manageLimit).forEach { task ->
                    item(key = "manage-${task.id}") {
                        TodayTaskCard(task, state, canManage, actorId, onComplete, onOpenCrop,
                            showAssignee = true)
                        if (task.status == 0 && state.choicesReady) {
                            OutlinedButton(onClick = {
                                editingId = if (editingId == task.id) null else task.id
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (editingId == task.id) uiText(R.string.ui_close_task_editor_989707d, "Close task editor") else uiText(R.string.ui_edit_assignment_f54d322, "Edit assignment"))
                            }
                        }
                    }
                    // The edit form is a separate lazy item so its Save button can
                    // be scrolled fully into view at larger font scales and 320dp.
                    if (task.status == 0 && state.choicesReady && editingId == task.id) {
                        item(key = "edit-${task.id}") {
                            formState.SaveableStateProvider("edit-${task.id}") { EditTaskForm(task, state, onUpdate) }
                        }
                    }
                }
                if (farmTasks.size > manageLimit) item(key = "manage-more") {
                    OutlinedButton(onClick = { manageLimit += 20 },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(uiText(R.string.ui_show_20_more_farm_tasks_1_s_remaining_1f5fba5, "Show 20 more farm tasks (%1\$s remaining)", (farmTasks.size - manageLimit)))
                    }
                }
            }
        }
        if (!isTasks) {
            if (state.notifications.isEmpty() && !state.busy && state.error == null) {
                item { FarmEmptyState(uiText(R.string.ui_no_notifications_yet_ca02c72, "No notifications yet."),
                    uiText(R.string.ui_new_reminders_and_advisories_appear_here_db106db, "New reminders and advisories appear here.")) }
            }
            items(state.notifications.size, key = { "notification-${state.notifications[it].id}" }) { index ->
                val notification = state.notifications[index]
                FarmSectionCard(notification.title) {
                    FarmStatusChip(if (notification.readAtUtc == null) uiText(R.string.ui_unread_1b9f384, "Unread") else uiText(R.string.ui_read_9b9a8d0, "Read"),
                        if (notification.readAtUtc == null) FarmStatusTone.Planned else FarmStatusTone.Neutral)
                    Text(notification.message)
                    state.notificationTimeZones[notification.id]?.let { zone ->
                        Text(com.smartfarm.mobile.ui.components.farmDateTime(notification.createdAtUtc, zone),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (notification.readAtUtc == null) {
                        OutlinedButton(onClick = { onRead(notification.id) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_mark_as_read_50c8b81, "Mark as read")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTaskCard(
    task: TaskDto, state: TaskUiState, canManage: Boolean, actorId: String?,
    onComplete: (TaskDto) -> Unit, onOpenCrop: (TaskDto) -> Unit,
    showAssignee: Boolean = false
) {
    var confirming by rememberSaveable(task.id) { mutableStateOf(false) }
    val cycle = state.cycles.firstOrNull { it.id == task.cropCycleId }
    FarmSectionCard(taskNames.getOrElse(task.taskType) { uiText(R.string.ui_field_task_b52c32b, "Field task") },
        subtitle = uiText(R.string.ui_due_1_s_1207d23, "Due %1\$s", (TaskDisplay.dueLabel(task.dueDate)))) {
        if (task.status != 0) {
            FarmStatusChip(statuses.getOrElse(task.status) { uiText(R.string.ui_closed_c21ead0, "Closed") }, FarmStatusTone.Neutral)
        }
        cycle?.let {
            Text("${it.cropTypeName} · ${it.plotName}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
        }
        task.description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        if (showAssignee) {
            val name = state.members.firstOrNull { it.userId == task.assignedToUserId }?.fullName
                ?: when {
                    task.assignedToUserId == null -> uiText(R.string.ui_unassigned_14d33bd, "Unassigned")
                    task.assignedToUserId == actorId -> uiText(R.string.ui_you_08b0419, "You")
                    else -> uiText(R.string.ui_farm_member_b39565f, "Farm member")
                }
            Text(uiText(R.string.ui_assigned_to_1_s_ade63f5, "Assigned to: %1\$s", (name)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (task.status == 0 && cycle?.status == 1 && task.taskType in 2..5) {
            OutlinedButton(onClick = { onOpenCrop(task) },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (task.taskType == 5) uiText(R.string.ui_open_harvest_record_5b4bd66, "Open harvest record") else uiText(R.string.ui_record_fieldwork_797a75c, "Record fieldwork"))
            }
        }
        if (TaskDisplay.canComplete(task, actorId, canManage)) {
            Button(onClick = { confirming = true }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_mark_completed_2b53d89, "Mark completed")) }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(uiText(R.string.ui_complete_this_task_72c6e96, "Complete this task?")) },
            text = {
                Text(uiText(R.string.ui_mark_1_s_as_completed_8fcc2b3, "Mark %1\$s as completed? ", (taskNames.getOrElse(task.taskType) { "this task" })) +
                    uiText(R.string.ui_this_updates_the_server_task_status_it_does_not_crea_3595e3e, "This updates the server task status; it does not create a fieldwork log."))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    if (TaskDisplay.canComplete(task, actorId, canManage) && !state.busy) {
                        onComplete(task)
                    }
                }) { Text(uiText(R.string.ui_complete_task_f9126d2, "Complete task")) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(uiText(R.string.ui_keep_open_333f472, "Keep open")) }
            }
        )
    }
}

@Composable
private fun TaskMemberPicker(
    selected: String?, members: List<FarmMemberDto>, onSelect: (String?) -> Unit,
    enabled: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val label = members.firstOrNull { it.userId == selected }?.fullName
        ?: if (selected == null) uiText(R.string.ui_unassigned_owner_receives_reminder_d3a2cbc, "Unassigned (owner receives reminder)") else uiText(R.string.ui_person_not_in_current_choices_0a345f7, "Person not in current choices")
    Column {
        Text(uiText(R.string.ui_assign_to_289e2f7, "Assign to"), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(uiText(R.string.ui_unassigned_owner_receives_reminder_d3a2cbc, "Unassigned (owner receives reminder)")) },
                onClick = { onSelect(null); expanded = false })
            members.forEach { member ->
                DropdownMenuItem(text = { Text("${member.fullName} · ${member.role}") },
                    onClick = { onSelect(member.userId); expanded = false })
            }
        }
    }
}

@Composable
private fun NewTaskForm(state: TaskUiState, today: LocalDate?, onCreate: (TaskCreateInput) -> Unit) {
    var type by rememberSaveable(state.farmId) { mutableStateOf(4) }
    var due by rememberSaveable(state.farmId) { mutableStateOf(today?.toString().orEmpty()) }
    var description by rememberSaveable(state.farmId) { mutableStateOf("") }
    var cycleId by rememberSaveable(state.farmId) { mutableStateOf<String?>(null) }
    var assignee by rememberSaveable(state.farmId) { mutableStateOf<String?>(null) }
    UnsavedChanges("new-task", type != 4 || due != today?.toString().orEmpty() || description.isNotEmpty() ||
        cycleId != null || assignee != null)
    val validDate = runCatching { LocalDate.parse(due.trim()) }.isSuccess
    val validCycle = cycleId == null || state.cycles.any { it.id == cycleId && it.status in 0..1 }
    val validAssignee = assignee == null || state.members.any { it.userId == assignee }
    FarmSectionCard(uiText(R.string.ui_new_task_3e99227, "New task"), subtitle = uiText(R.string.ui_choose_a_preset_or_enter_your_own_instructions_6c2779d, "Choose a preset or enter your own instructions.")) {
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkTaskPresets.options.forEach { preset ->
                FilterChip(selected = type == preset.type,
                    enabled = !state.busy,
                    onClick = { type = preset.type; description = preset.suggestedDescription },
                    label = { Text(preset.label) })
            }
        }
        var moreTypes by rememberSaveable(state.farmId) { mutableStateOf(false) }
        Text(uiText(R.string.ui_selected_1f153fd, "Selected: ") + taskNames.getOrElse(type) { uiText(R.string.ui_task_4bc74b2, "Task") },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { moreTypes = true }, enabled = !state.busy) { Text(uiText(R.string.ui_other_task_types_36eac6f, "Other task types")) }
        DropdownMenu(expanded = moreTypes, onDismissRequest = { moreTypes = false }) {
            listOf(1, 6).forEach { code ->
                DropdownMenuItem(text = { Text(taskNames[code]) }, enabled = !state.busy, onClick = {
                    type = code
                    moreTypes = false
                })
            }
        }
        DateInput(due, uiText(R.string.ui_due_date_e1cb6d3, "Due date"), { due = it }, enabled = !state.busy)
        if (due.isNotBlank() && !validDate)
            Text(uiText(R.string.ui_choose_a_valid_due_date_8c81707, "Choose a valid due date."), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        CyclePicker(cycleId, state.cycles, { cycleId = it }, !state.busy && state.choicesReady)
        if (state.choicesReady && !validCycle)
            Text(uiText(R.string.ui_choose_a_listed_crop_or_no_crop_cycle_before_creatin_811806a, "Choose a listed crop or No crop cycle before creating this task."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        TaskMemberPicker(assignee, state.members, { assignee = it }, !state.busy && state.choicesReady)
        if (state.choicesReady && !validAssignee)
            Text(uiText(R.string.ui_choose_a_listed_person_or_unassigned_before_creating_2666691, "Choose a listed person or Unassigned before creating this task."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        OutlinedTextField(description, { description = it }, enabled = !state.busy,
            isError = description.trim().length > 500,
            label = { Text(uiText(R.string.ui_instructions_optional_396f78a, "Instructions (optional)")) }, minLines = 2, maxLines = 4,
            modifier = Modifier.fillMaxWidth())
        if (description.trim().length > 500)
            Text(uiText(R.string.ui_instructions_must_be_500_characters_or_fewer_6b0bfb6, "Instructions must be 500 characters or fewer."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            onCreate(TaskCreateInput(type, LocalDate.parse(due.trim()).toString(),
                description.trim().ifBlank { null }, cycleId, assignee))
        }, enabled = state.choicesReady && !state.busy && validDate && validCycle && validAssignee &&
            description.trim().length <= 500,
            modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_create_task_6f541e1, "Create task")) }
    }
}

@Composable
private fun CyclePicker(
    selected: String?, cycles: List<CropCycleDto>, onSelect: (String?) -> Unit, enabled: Boolean
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val eligible = cycles.filter { it.status == 0 || it.status == 1 }
    val label = eligible.firstOrNull { it.id == selected }?.let {
        "${it.cropTypeName} · ${it.plotName}"
    } ?: if (selected == null) uiText(R.string.ui_no_crop_cycle_ee975e3, "No crop cycle") else uiText(R.string.ui_selected_crop_not_listed_6f49299, "Selected crop not listed")
    Column {
        Text(uiText(R.string.ui_crop_cycle_optional_65752c6, "Crop cycle (optional)"), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(uiText(R.string.ui_no_crop_cycle_ee975e3, "No crop cycle")) }, enabled = enabled, onClick = {
                onSelect(null); expanded = false
            })
            eligible.forEach { cycle ->
                DropdownMenuItem(text = { Text("${cycle.cropTypeName} · ${cycle.plotName}") },
                    enabled = enabled,
                    onClick = { onSelect(cycle.id); expanded = false })
            }
        }
    }
}

@Composable
private fun EditTaskForm(
    task: TaskDto, state: TaskUiState,
    onUpdate: (TaskDto, TaskUpdateInput) -> Unit
) {
    var due by rememberSaveable(task.id) { mutableStateOf(task.dueDate) }
    var description by rememberSaveable(task.id) { mutableStateOf(task.description.orEmpty()) }
    var assignee by rememberSaveable(task.id) { mutableStateOf(task.assignedToUserId) }
    UnsavedChanges("task-${task.id}", due != task.dueDate || description != task.description.orEmpty() || assignee != task.assignedToUserId)
    val validDate = runCatching { LocalDate.parse(due.trim()) }.isSuccess
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DateInput(due, uiText(R.string.ui_due_date_e1cb6d3, "Due date"), { due = it }, enabled = !state.busy)
        // Existing assignees may be the owner, who is absent from member choices; the server validates them.
        TaskMemberPicker(assignee, state.members, { assignee = it }, !state.busy)
        OutlinedTextField(description, { description = it }, enabled = !state.busy,
            isError = description.trim().length > 500,
            label = { Text(uiText(R.string.ui_instructions_934652d, "Instructions")) }, minLines = 2, maxLines = 4,
            modifier = Modifier.fillMaxWidth())
        if (description.trim().length > 500)
            Text(uiText(R.string.ui_instructions_must_be_500_characters_or_fewer_6b0bfb6, "Instructions must be 500 characters or fewer."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            onUpdate(task, TaskUpdateInput(LocalDate.parse(due.trim()).toString(),
                description.trim().ifBlank { null }, assignee, task.rowVersion))
        }, enabled = state.choicesReady && !state.busy && validDate &&
            description.trim().length <= 500, modifier = Modifier.fillMaxWidth()) {
            Text(uiText(R.string.ui_save_changes_dd0ae7a, "Save changes"))
        }
    }
}
