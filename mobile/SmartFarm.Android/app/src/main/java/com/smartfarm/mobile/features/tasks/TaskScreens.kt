package com.smartfarm.mobile.features.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
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
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage
import java.time.LocalDate

private val taskNames = listOf("Unknown", "Planting", "Watering", "Fertilizing",
    "Inspection", "Harvest", "Other")
private val statuses = listOf("Pending", "Completed", "Skipped", "Cancelled")

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
    LaunchedEffect(state.savedCount) {
        if (state.savedCount > 0) {
            adding = false
            editingId = null
        }
    }
    val today = state.timeZoneId?.let { zone ->
        runCatching { TaskWorkCenter.today(zone) }.getOrNull()
    }
    val calendar = rememberDatePickerState(initialSelectedDateMillis = today?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
    val calendarDate = calendar.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val assigned = TaskWorkCenter.myOpenTasks(state.tasks, actorId, isOwner)
    val overdue = today?.let { TaskWorkCenter.overdue(assigned, it) }.orEmpty()
    val dueToday = today?.let { TaskWorkCenter.dueToday(assigned, it) }.orEmpty()
    val upcoming = today?.let { TaskWorkCenter.upcoming(assigned, it) }.orEmpty()
    val closed = TaskDisplay.myHistory(state.tasks, actorId, isOwner)
    val isTasks = state.farmId != null
    val farmTasks = if (canManage) TaskDisplay.farmTasks(state.tasks) else emptyList()
    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero(if (isTasks) "My tasks" else "Notifications",
                    if (isTasks) "Start with today's work. Use Calendar for other dates."
                    else "Task reminders and farm advisories.")
                if (!isTasks) OutlinedButton(onClick = onBack) { Text("Back") }
                if (state.busy) CircularProgressIndicator()
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                state.notice?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Refresh") }
                if (isTasks) {
                    Text("Task updates need internet. Offline field logs are available from an active crop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (isTasks && (state.error == null || state.tasks.isNotEmpty())) {
            item(key = "task-tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("today" to "Today", "upcoming" to "Upcoming",
                            "completed" to "Completed").forEach { (key, title) ->
                            FilterChip(selected = selectedView == key,
                                onClick = { selectedView = key },
                                modifier = Modifier.weight(1f),
                                label = { Text(title) })
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { selectedView = "calendar" },
                            modifier = Modifier.weight(1f)) { Text("Calendar") }
                        if (canManage) {
                            OutlinedButton(onClick = { selectedView = "manage"; adding = false },
                                modifier = Modifier.weight(1f)) { Text("All farm tasks") }
                        }
                    }
                    if (canManage) {
                        Button(onClick = {
                            selectedView = "manage"
                            adding = !adding
                            editingId = null
                        }, enabled = !state.busy && state.choicesReady,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (adding && selectedView == "manage") "Close new task form"
                                 else "Create and assign task")
                        }
                        state.choicesError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (selectedView == "today") {
                item(key = "today-summary") {
                    FarmSectionCard("My work", subtitle = if (today != null)
                        "Farm date: ${TaskDisplay.dueLabel(today.toString())} · ${state.timeZoneId}"
                        else "Farm time zone unavailable. Ask an owner or manager to check settings.") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FarmStatusChip("${overdue.size} overdue",
                                if (overdue.isEmpty()) FarmStatusTone.Neutral else FarmStatusTone.Warning)
                            FarmStatusChip("${dueToday.size} due today", FarmStatusTone.Active)
                        }
                    }
                    if (today == null) {
                        FarmEmptyState("Farm date unavailable",
                            "Reconnect and refresh before completing dated tasks.")
                    } else if (!state.busy && overdue.isEmpty() && dueToday.isEmpty()) {
                        FarmEmptyState("Nothing due today",
                            if (upcoming.isNotEmpty()) "Your next assignment is ${TaskDisplay.dueLabel(upcoming.first().dueDate)}."
                            else "You're caught up. Check Upcoming or refresh for new work.")
                    }
                }
                if (today != null) {
                    if (overdue.isNotEmpty()) item(key = "overdue-heading") {
                        FarmSectionHeader("Overdue", "These tasks need attention first.")
                    }
                    items(overdue.size, key = { "overdue-${overdue[it].id}" }) { index ->
                        TodayTaskCard(overdue[index], state, canManage, actorId, onComplete, onOpenCrop)
                    }
                    if (dueToday.isNotEmpty()) item(key = "due-today-heading") {
                        FarmSectionHeader("Due today", "Today's assigned fieldwork.")
                    }
                    items(dueToday.size, key = { "today-${dueToday[it].id}" }) { index ->
                        TodayTaskCard(dueToday[index], state, canManage, actorId, onComplete, onOpenCrop)
                    }
                }
            }
            if (selectedView == "upcoming") {
                if (today == null) item(key = "upcoming-date-error") {
                    FarmEmptyState("Farm date unavailable",
                        "Reconnect and refresh to view upcoming tasks.")
                } else if (upcoming.isEmpty()) item(key = "upcoming-empty") {
                    FarmEmptyState("No upcoming tasks",
                        "Your future assignments will appear here.")
                } else item(key = "upcoming-heading") {
                    FarmSectionHeader("Upcoming", "Your next assigned tasks.")
                }
                if (today != null) items(upcoming.size, key = { "upcoming-${upcoming[it].id}" }) { index ->
                    TodayTaskCard(upcoming[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
            }
            if (selectedView == "completed") {
                if (closed.isEmpty()) item(key = "history-empty") {
                    FarmEmptyState("No completed tasks yet",
                        "Your completed, skipped or cancelled assignments will appear here.")
                } else item(key = "history-heading") {
                    FarmSectionHeader("My task history",
                        "Your closed assignments. Records cannot be edited here.")
                }
                items(closed.size, key = { "history-${closed[it].id}" }) { index ->
                    TodayTaskCard(closed[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
            }
            if (selectedView == "calendar") {
                item(key = "calendar") {
                    FarmSectionHeader("Task calendar", "Select a farm-local date to view your work.")
                    Surface(shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface) {
                        DatePicker(state = calendar, showModeToggle = false,
                            title = null, headline = null,
                            colors = DatePickerDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surface))
                    }
                }
                item(key = "calendar-heading") {
                    FarmSectionHeader("Assignments on " +
                        (calendarDate?.toString() ?: "selected date"),
                        "Open tasks for this date. Dates follow the farm's time zone.")
                }
                val calendarTasks = assigned.filter { TaskWorkCenter.dueDate(it) == calendarDate }
                if (calendarTasks.isEmpty()) item(key = "calendar-empty") {
                    FarmEmptyState("No assigned work on this date",
                        "Choose another day or return to Today.")
                }
                items(calendarTasks.size, key = { "calendar-${calendarTasks[it].id}" }) { index ->
                    TodayTaskCard(calendarTasks[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
            }
            if (selectedView == "manage" && canManage) {
                if (adding && state.choicesReady) item(key = "new-task") {
                    NewTaskForm(state, today, onCreate)
                }
                item(key = "manage-heading") {
                    FarmSectionHeader("All farm tasks",
                        "Manage assignments and due dates. Existing completed records are read-only.")
                }
                if (farmTasks.isEmpty()) item(key = "manage-empty") {
                    FarmEmptyState("No farm tasks yet",
                        "Create a task above to assign work to your team.")
                }
                items(farmTasks.size, key = { "manage-${farmTasks[it].id}" }) { index ->
                    val task = farmTasks[index]
                    TodayTaskCard(task, state, canManage, actorId, onComplete, onOpenCrop,
                        showAssignee = true)
                    if (task.status == 0 && state.choicesReady) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                editingId = if (editingId == task.id) null else task.id
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (editingId == task.id) "Close task editor" else "Edit assignment")
                            }
                            if (editingId == task.id) EditTaskForm(task, state, onUpdate)
                        }
                    }
                }
            }
        }
        if (!isTasks) {
            if (state.notifications.isEmpty() && !state.busy && state.error == null) {
                item { FarmEmptyState("No notifications yet.",
                    "New reminders and advisories appear here.") }
            }
            items(state.notifications.size, key = { "notification-${state.notifications[it].id}" }) { index ->
                val notification = state.notifications[index]
                FarmSectionCard(notification.title) {
                    FarmStatusChip(if (notification.readAtUtc == null) "Unread" else "Read",
                        if (notification.readAtUtc == null) FarmStatusTone.Planned else FarmStatusTone.Neutral)
                    Text(notification.message)
                    if (notification.readAtUtc == null) {
                        OutlinedButton(onClick = { onRead(notification.id) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) { Text("Mark as read") }
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
    FarmSectionCard(taskNames.getOrElse(task.taskType) { "Field task" },
        subtitle = "Due ${TaskDisplay.dueLabel(task.dueDate)}") {
        if (task.status != 0) {
            FarmStatusChip(statuses.getOrElse(task.status) { "Closed" }, FarmStatusTone.Neutral)
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
                    task.assignedToUserId == null -> "Unassigned"
                    task.assignedToUserId == actorId -> "You"
                    else -> "Farm member"
                }
            Text("Assigned to: $name", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (task.status == 0 && cycle?.status == 1 && task.taskType in 2..5) {
            OutlinedButton(onClick = { onOpenCrop(task) },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (task.taskType == 5) "Open harvest record" else "Record fieldwork")
            }
        }
        if (TaskDisplay.canComplete(task, actorId, canManage)) {
            Button(onClick = { confirming = true }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text("Mark completed") }
        }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Complete this task?") },
            text = {
                Text("Mark ${taskNames.getOrElse(task.taskType) { "this task" }} as completed? " +
                    "This updates the server task status; it does not create a fieldwork log.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    if (TaskDisplay.canComplete(task, actorId, canManage) && !state.busy) {
                        onComplete(task)
                    }
                }) { Text("Complete task") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Keep open") }
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
        ?: if (selected == null) "Unassigned (owner receives reminder)" else "Unavailable member"
    Column {
        Text("Assign to", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Unassigned (owner receives reminder)") },
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
    val validDate = runCatching { LocalDate.parse(due.trim()) }.isSuccess
    FarmSectionCard("New task", subtitle = "Choose a preset or enter your own instructions.") {
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkTaskPresets.options.forEach { preset ->
                FilterChip(selected = type == preset.type,
                    onClick = { type = preset.type; description = preset.suggestedDescription },
                    label = { Text(preset.label) })
            }
        }
        Text("Task type", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..6).forEach { code ->
                FilterChip(selected = type == code, onClick = { type = code },
                    label = { Text(taskNames[code]) })
            }
        }
        OutlinedTextField(due, { due = it }, label = { Text("Due date (YYYY-MM-DD)") },
            singleLine = true, isError = due.isNotEmpty() && !validDate,
            modifier = Modifier.fillMaxWidth())
        CyclePicker(cycleId, state.cycles, { cycleId = it })
        TaskMemberPicker(assignee, state.members, { assignee = it }, !state.busy)
        OutlinedTextField(description, { description = it },
            label = { Text("Instructions (optional)") }, minLines = 2,
            modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            onCreate(TaskCreateInput(type, LocalDate.parse(due.trim()).toString(),
                description.trim().ifBlank { null }, cycleId, assignee))
        }, enabled = state.choicesReady && !state.busy && validDate &&
            description.trim().length <= 500,
            modifier = Modifier.fillMaxWidth()) { Text("Create task") }
    }
}

@Composable
private fun CyclePicker(
    selected: String?, cycles: List<CropCycleDto>, onSelect: (String?) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val eligible = cycles.filter { it.status == 0 || it.status == 1 }
    val label = eligible.firstOrNull { it.id == selected }?.let {
        "${it.cropTypeName} · ${it.plotName}"
    } ?: "No crop cycle"
    Column {
        Text("Crop cycle (optional)", style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("No crop cycle") }, onClick = {
                onSelect(null); expanded = false
            })
            eligible.forEach { cycle ->
                DropdownMenuItem(text = { Text("${cycle.cropTypeName} · ${cycle.plotName}") },
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
    val validDate = runCatching { LocalDate.parse(due.trim()) }.isSuccess
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(due, { due = it }, label = { Text("Due date (YYYY-MM-DD)") },
            isError = due.isNotEmpty() && !validDate, modifier = Modifier.fillMaxWidth())
        TaskMemberPicker(assignee, state.members, { assignee = it }, !state.busy)
        OutlinedTextField(description, { description = it },
            label = { Text("Instructions") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            onUpdate(task, TaskUpdateInput(LocalDate.parse(due.trim()).toString(),
                description.trim().ifBlank { null }, assignee, task.rowVersion))
        }, enabled = state.choicesReady && !state.busy && validDate &&
            description.trim().length <= 500, modifier = Modifier.fillMaxWidth()) {
            Text("Save changes")
        }
    }
}
