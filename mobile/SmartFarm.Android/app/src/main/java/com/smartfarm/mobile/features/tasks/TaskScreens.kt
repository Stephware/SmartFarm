package com.smartfarm.mobile.features.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmEmptyState
import androidx.compose.foundation.layout.fillMaxWidth
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
                FarmHero(
                    if (state.farmId == null) "Notifications" else "Farm tasks",
                    if (state.farmId == null) "Task reminders and farm advisories."
                    else "Track assigned work and completion status."
                )
                OutlinedButton(onClick = onBack) { Text("Back") }
                Text("Connect to the internet to refresh and update these records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.busy) CircularProgressIndicator()
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Refresh") }
                if (!state.busy && state.error == null && state.tasks.isEmpty() && state.notifications.isEmpty()) {
                    FarmEmptyState(
                        if (state.farmId == null) "No notifications yet." else "No tasks scheduled yet.",
                        if (state.farmId == null) "New reminders and advisories appear here."
                        else "Upcoming farm work will appear here once scheduled."
                    )
                }
            }
            items(state.tasks, key = { it.id }) { task ->
                FarmSectionCard(
                    title = taskNames.getOrElse(task.taskType) { "Task" },
                    subtitle = "Due ${task.dueDate}"
                ) {
                    FarmStatusChip(statuses.getOrElse(task.status) { "Unknown status" },
                        if (task.status == 0) FarmStatusTone.Planned else FarmStatusTone.Neutral)
                    task.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (task.status == 0 && (canManage || task.assignedToUserId == actorId && actorId != null)) {
                        Button(onClick = { onComplete(task) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) { Text("Mark completed") }
                    }
                }
            }
            items(state.notifications, key = { it.id }) { notification ->
                FarmSectionCard(title = notification.title) {
                    FarmStatusChip(if (notification.readAtUtc == null) "Unread" else "Read",
                        if (notification.readAtUtc == null) FarmStatusTone.Planned else FarmStatusTone.Neutral)
                    Text(notification.message, style = MaterialTheme.typography.bodyMedium)
                    if (notification.readAtUtc == null) {
                        OutlinedButton(onClick = { onRead(notification.id) }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()) { Text("Mark as read") }
                    }
                }
            }
    }
}
