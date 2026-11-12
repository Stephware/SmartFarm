package com.smartfarm.mobile.features.syncstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.sync.StockRetryPreview
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.pendingRecordDescription
import com.smartfarm.mobile.ui.components.farmDateTime

/**
 * Work remains on this device until a validated server receipt removes it from Room.
 * Do not claim an operation is uploaded merely because WorkManager was scheduled.
 */
@Composable
fun SyncStatusScreen(
    operations: List<PendingOperation>,
    photos: List<PendingPhoto>,
    stockPreview: StockRetryPreview?,
    error: String?,
    onBack: () -> Unit,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onRetryPhoto: (String) -> Unit,
    onDiscardPhoto: (String) -> Unit,
    onReviewStock: (String) -> Unit,
    onApproveStock: () -> Unit,
    onCancelStock: () -> Unit,
    onSyncNow: () -> Unit,
    currentFarmId: String? = null,
    farmNames: Map<String, String> = emptyMap(),
    notice: String? = null,
    farmTimeZone: String? = null
) {
    var discard by rememberSaveable { mutableStateOf<String?>(null) }
    var discardPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var allFarms by rememberSaveable(currentFarmId) { mutableStateOf(currentFarmId.isNullOrBlank()) }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    val farmId = if (allFarms) null else currentFarmId
    val shownOperations = operations.filter { farmId == null || it.farmId == farmId }
        .filter { row ->
            statusFilter == "all" ||
                (syncState(row.status, row.errorMessage) == SyncRecordState.NEEDS_REVIEW) ==
                    (statusFilter == "review")
        }.sortedWith(compareByDescending<PendingOperation> {
            syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW
        }.thenByDescending { it.createdAtUtc })
    val shownPhotos = photos.filter { farmId == null || it.farmId == farmId }
        .filter { photo ->
            statusFilter == "all" ||
                (syncState(photo.status, photo.errorMessage) == SyncRecordState.NEEDS_REVIEW) ==
                    (statusFilter == "review")
        }.sortedWith(compareByDescending<PendingPhoto> {
            syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW
        }.thenByDescending { it.savedAtUtcMillis })
    val summary = syncQueueSummary(operations, photos, farmId)
    val global = syncQueueSummary(operations, photos)
    fun farmLabel(id: String): String = farmNames[id] ?: "Farm ${id.take(8)}"

    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                FarmHero("Offline sync",
                    "Review records saved on this phone before the farm server confirms them.")
                FarmSectionCard("Sync queue", subtitle = "Saved here, not yet included in confirmed totals.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FarmStatusChip("Pending: ${summary.waiting}", FarmStatusTone.Planned)
                        FarmStatusChip("Needs review: ${summary.needsReview}",
                            if (summary.needsReview > 0) FarmStatusTone.Warning
                            else FarmStatusTone.Neutral)
                    }
                    Text("${summary.total} local ${if (summary.total == 1) "record" else "records"} on this view",
                        style = MaterialTheme.typography.bodyMedium)
                    if (global.total > summary.total) {
                        Text("${global.total - summary.total} more saved in other farms.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (currentFarmId != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !allFarms, onClick = { allFarms = false },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text("This farm") })
                        FilterChip(selected = allFarms, onClick = { allFarms = true },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text("All my farms") })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "All", "review" to "Needs review",
                        "waiting" to "Waiting").forEach { (key, label) ->
                        FilterChip(selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text(label) })
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                notice?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Text("A record is confirmed only after the server accepts it. Sync will run when a connection is available.",
                    style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text("Sync now")
                }
                if (shownOperations.isEmpty() && shownPhotos.isEmpty()) {
                    FarmEmptyState(
                        if (statusFilter == "review") "No records need review"
                        else if (statusFilter == "waiting") "No waiting records"
                        else if (summary.total == 0) "No records waiting on this phone"
                        else "No matching records",
                        if (summary.total == 0)
                            "Previously confirmed work is available in its farm history."
                        else "Choose another filter or view all farms to find saved records."
                    )
                }
                if (operations.size >= 100 || photos.size >= 100) {
                    Text("Showing the most recent local entries. Older records are kept on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (shownOperations.isNotEmpty()) item(key = "records-heading") {
            FarmSectionHeader("Field records", "Needs-review entries appear first.")
        }
        items(shownOperations, key = { "operation-${it.operationId}" }) { operation ->
            val needs = syncState(operation.status, operation.errorMessage) == SyncRecordState.NEEDS_REVIEW
            val isStock = operation.operationType in setOf("inventory", "activity_stock")
            FarmSectionCard(syncRecordType(operation.operationType),
                subtitle = "${farmLabel(operation.farmId)} · " +
                    farmDateTime(java.time.Instant.ofEpochMilli(operation.createdAtUtc).toString(), farmTimeZone)) {
                FarmStatusChip(if (needs) "Needs attention" else "Waiting to sync",
                    if (needs) FarmStatusTone.Warning else FarmStatusTone.Planned)
                Text(pendingRecordDescription(operation))
                if (needs) {
                    operation.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    }
                    Text(if (isStock)
                        "Stock may have changed. Check its latest balance before retrying."
                        else "Check the record and message before retrying.",
                        style = MaterialTheme.typography.bodySmall)
                    if (operation.status == "review") {
                        if (isStock) {
                            OutlinedButton(onClick = { onReviewStock(operation.operationId) },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text("Check latest stock")
                            }
                        } else {
                            OutlinedButton(onClick = { onRetry(operation.operationId) },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text("Retry this change")
                            }
                        }
                    }
                    TextButton(onClick = { discard = operation.operationId }) {
                        Text("Discard and re-enter")
                    }
                }
            }
        }
        if (shownPhotos.isNotEmpty()) item(key = "photos-heading") {
            FarmSectionHeader("Crop photos", "Photos upload after their linked inspections.")
        }
        items(shownPhotos, key = { "photo-${it.photoId}" }) { photo ->
            val needs = syncState(photo.status, photo.errorMessage) == SyncRecordState.NEEDS_REVIEW
            FarmSectionCard("Inspection photo",
                subtitle = "${farmLabel(photo.farmId)} · " +
                    farmDateTime(java.time.Instant.ofEpochMilli(photo.savedAtUtcMillis).toString(), farmTimeZone)) {
                FarmStatusChip(if (needs) "Needs attention" else "Waiting to sync",
                    if (needs) FarmStatusTone.Warning else FarmStatusTone.Planned)
                if (photo.observationId == null) {
                    Text("Waiting for its inspection to be accepted before uploading the photo.",
                        style = MaterialTheme.typography.bodySmall)
                }
                if (needs) {
                    photo.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    }
                    if (canRetryPhoto(photo)) {
                        OutlinedButton(onClick = { onRetryPhoto(photo.photoId) },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                            Text("Retry photo")
                        }
                    } else if (syncNeedsParent(photo)) {
                        Text("Review the inspection entry above first. Retrying this photo alone cannot finish it.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { discardPhoto = photo.photoId }) {
                        Text("Discard local photo")
                    }
                }
            }
        }
    }

    stockPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onCancelStock,
            title = { Text("Confirm stock retry") },
            text = {
                Text("${preview.itemName}\nCurrent stock: ${preview.currentBalance}\n" +
                    "Queued change: ${preview.change}\n" +
                    "Stock after change: ${preview.expectedBalance}\n" +
                    (if (preview.includesFieldActivity)
                        "The field activity and supply consumption will be saved together. "
                    else "") +
                    "Only retry if this movement still reflects actual farm work.")
            },
            confirmButton = { TextButton(onClick = onApproveStock) { Text("Approve and retry") } },
            dismissButton = { TextButton(onClick = onCancelStock) { Text("Cancel") } }
        )
    }
    discardPhoto?.let { photoId ->
        AlertDialog(onDismissRequest = { discardPhoto = null },
            title = { Text("Discard offline photo?") },
            text = { Text("The photo file will be permanently deleted from this phone. This cannot be undone.") },
            confirmButton = { TextButton(onClick = {
                onDiscardPhoto(photoId); discardPhoto = null
            }) { Text("Discard photo") } },
            dismissButton = { TextButton(onClick = { discardPhoto = null }) { Text("Cancel") } })
    }
    discard?.let { operationId ->
        AlertDialog(onDismissRequest = { discard = null },
            title = { Text("Discard unsynced change?") },
            text = { Text("This saved local record will be permanently deleted. Linked unsynced photos may also be removed. This cannot be undone.") },
            confirmButton = { TextButton(onClick = {
                onDiscard(operationId); discard = null
            }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { discard = null }) { Text("Cancel") } })
    }
}
