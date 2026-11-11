package com.smartfarm.mobile.features.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
    var showAll by rememberSaveable(state.farmId) { mutableStateOf(false) }
    LaunchedEffect(state.savedCount) {
        if (state.savedCount > 0) {
            adding = false
            editingId = null
        }
    }
    val today = state.timeZoneId?.let { zone ->
        runCatching { TaskWorkCenter.today(zone) }.getOrNull()
    }
    val assigned = TaskWorkCenter.myOpenTasks(state.tasks, actorId, isOwner)
    val overdue = today?.let { TaskWorkCenter.overdue(assigned, it) }.orEmpty()
    val dueToday = today?.let { TaskWorkCenter.dueToday(assigned, it) }.orEmpty()
    val upcoming = today?.let { TaskWorkCenter.upcoming(assigned, it) }.orEmpty()
    val isTasks = state.farmId != null
    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero(if (isTasks) "Today's farm work" else "Notifications",
                    if (isTasks) "Your assignments and upcoming fieldwork, using the farm's local date."
                    else "Task reminders and farm advisories.")
                OutlinedButton(onClick = onBack) { Text("Back") }
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

        if (isTasks && state.error == null || isTasks && state.tasks.isNotEmpty()) {
            item(key = "today-summary") {
                FarmSectionCard("My work", subtitle = if (today != null)
                    "Farm date: $today · ${state.timeZoneId}"
                else "Farm time zone unavailable. Refresh before planning work.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FarmStatusChip("${overdue.size} overdue",
                            if (overdue.isEmpty()) FarmStatusTone.Neutral else FarmStatusTone.Warning)
                        FarmStatusChip("${dueToday.size} due today", FarmStatusTone.Active)
                    }
                    Text("${upcoming.size} upcoming assigned task(s)",
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (today != null && assigned.isEmpty() && !state.busy) {
                    FarmEmptyState("No assigned work right now",
                        "New assignments will appear here after a refresh.")
                }
            }
            if (today != null) {
                if (overdue.isNotEmpty()) item(key = "overdue-heading") {
                    FarmSectionHeader("Overdue", "Open work past the farm's local due date.")
                }
                items(overdue.size, key = { "overdue-${overdue[it].id}" }) { index ->
                    TodayTaskCard(overdue[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
                if (dueToday.isNotEmpty()) item(key = "today-heading") {
                    FarmSectionHeader("Due today", "Work scheduled for the farm's current date.")
                }
                items(dueToday.size, key = { "today-${dueToday[it].id}" }) { index ->
                    TodayTaskCard(dueToday[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
                if (upcoming.isNotEmpty()) item(key = "upcoming-heading") {
                    FarmSectionHeader("Upcoming", "Your next scheduled assignments.")
                }
                items(upcoming.take(4).size, key = { "upcoming-${upcoming[it].id}" }) { index ->
                    TodayTaskCard(upcoming[index], state, canManage, actorId, onComplete, onOpenCrop)
                }
            }
            if (canManage) item(key = "manager-action") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { adding = !adding }, enabled = !state.busy &&
                        state.choicesReady, modifier = Modifier.fillMaxWidth()) {
                        Text(if (adding) "Close new task form" else "Create and assign task")
                    }
                    state.choicesError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    if (adding && state.choicesReady) {
                        NewTaskForm(state, today, onCreate)
                    }
                    Text("Planting, watering and harvest reminders may already be generated by the Worker. " +
                        "These presets fill forms only; they do not schedule extra recurring jobs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "all-tasks-toggle") {
                OutlinedButton(onClick = { showAll = !showAll }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showAll) "Hide full task list" else "View all farm tasks (${state.tasks.size})")
                }
            }
            if (showAll) {
                if (state.tasks.isEmpty()) item(key = "empty-tasks") {
                    FarmEmptyState("No tasks scheduled yet", "Managers can add a task above.")
                }
                items(state.tasks.size, key = { "all-${state.tasks[it].id}" }) { index ->
                    val task = state.tasks[index]
                    FarmSectionCard(
                        taskNames.getOrElse(task.taskType) { "Unknown task" },
                        subtitle = "Due ${task.dueDate}"
                    ) {
                        FarmStatusChip(statuses.getOrElse(task.status) { "Unknown" },
                            if (task.status == 0) FarmStatusTone.Planned else FarmStatusTone.Neutral)
                        task.description?.let { Text(it) }
                        val assignedName = state.members.firstOrNull {
                            it.userId == task.assignedToUserId
                        }?.fullName ?: if (task.assignedToUserId == null) "Unassigned"
                            else if (task.assignedToUserId == actorId) "You" else "Farm owner / member"
                        Text("Assigned: $assignedName",
                            style = MaterialTheme.typography.bodySmall)
                        if (task.status == 0 && (canManage ||
                                actorId != null && task.assignedToUserId == actorId)) {
                            Button(onClick = { onComplete(task) }, enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth()) { Text("Mark completed") }
                        }
                        if (task.status == 0 && canManage && state.choicesReady) {
                            OutlinedButton(onClick = {
                                editingId = if (editingId == task.id) null else task.id
                            }) { Text(if (editingId == task.id) "Close editor" else "Edit assignment / due date") }
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
    onComplete: (TaskDto) -> Unit, onOpenCrop: (TaskDto) -> Unit
) {
    FarmSectionCard(taskNames.getOrElse(task.taskType) { "Task" },
        subtitle = "Due ${task.dueDate}") {
        task.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        val crop = state.cycles.firstOrNull { it.id == task.cropCycleId }
        if (crop != null) {
            Text("${crop.cropTypeName} · ${crop.plotName}",
                style = MaterialTheme.typography.bodySmall)
            if (crop.status == 1 && task.taskType in 2..5) {
                OutlinedButton(onClick = { onOpenCrop(task) },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (task.taskType == 5) "Open harvest records" else "Open crop logging")
                }
            }
        }
        if (task.status == 0 && (canManage ||
                actorId != null && task.assignedToUserId == actorId)) {
            Button(onClick = { onComplete(task) }, enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()) { Text("Mark completed") }
        }
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
