package com.smartfarm.mobile.features.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.api.TaskDto

@Composable
fun TaskScreen(state: TaskUiState, canManage: Boolean, actorId: String?, onBack: () -> Unit,
    onRetry: () -> Unit, onComplete: (TaskDto) -> Unit, onRead: (String) -> Unit) {
    val taskNames = listOf("Task", "Planting", "Watering", "Fertilizing", "Inspection", "Harvest", "Other")
    val statuses = listOf("Pending", "Completed", "Skipped", "Cancelled")
    RecordPage {
            item {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Text(if (state.farmId == null) "Notifications" else "Farm tasks", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text("Connect to the internet to refresh and update these records.")
                if (state.busy) CircularProgressIndicator()
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Refresh") }
                if (!state.busy && state.error == null && state.tasks.isEmpty() && state.notifications.isEmpty())
                    Text(if (state.farmId == null) "No notifications yet." else "No tasks scheduled yet.")
            }
            items(state.tasks, key = { it.id }) { task ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(taskNames.getOrElse(task.taskType) { "Task" }, style = MaterialTheme.typography.titleMedium)
                    Text("Due ${task.dueDate} · ${statuses.getOrElse(task.status) { "Unknown status" }}")
                    task.description?.let { Text(it) }
                    if (task.status == 0 && (canManage || task.assignedToUserId == actorId && actorId != null))
                        Button(onClick = { onComplete(task) }, enabled = !state.busy) { Text("Mark completed") }
                    HorizontalDivider()
                }
            }
            items(state.notifications, key = { it.id }) { notification ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(notification.title, style = MaterialTheme.typography.titleMedium)
                    Text(notification.message)
                    Text(if (notification.readAtUtc == null) "Unread" else "Read")
                    if (notification.readAtUtc == null)
                        OutlinedButton(onClick = { onRead(notification.id) }, enabled = !state.busy) { Text("Mark as read") }
                    HorizontalDivider()
                }
            }
    }
}
