package com.smartfarm.mobile.features.syncstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.pendingRecordDescription
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.sync.StockRetryPreview

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
    onSyncNow: () -> Unit
) {
    var discard by remember { mutableStateOf<PendingOperation?>(null) }
    var discardPhoto by remember { mutableStateOf<PendingPhoto?>(null) }

    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Text("Offline sync", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text("Pending: " + (operations.count { it.status == "pending" } +
                photos.count { it.status == "pending" }) +
                " · Needs review: " + (operations.count { it.status == "review" } +
                photos.count { it.status == "review" }))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
            Text("Changes stay on this device until the server confirms them. " +
                "Check rejected records before retrying; stock may need a newer version.")
            Button(onClick = onSyncNow) { Text("Sync now") }
            if (operations.isEmpty() && photos.isEmpty()) Text("All local changes are synced.")
            }
        }
        items(operations, key = { "operation-${it.operationId}" }) { operation ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(operation.operationType.replaceFirstChar { it.uppercase() } +
                    " · " + operation.status, style = MaterialTheme.typography.titleMedium)
                Text(pendingRecordDescription(operation))
                operation.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                if (operation.status == "review") {
                    if (operation.operationType == "inventory") {
                        OutlinedButton(onClick = { onReviewStock(operation.operationId) }) {
                            Text("Check latest stock")
                        }
                    } else {
                        OutlinedButton(onClick = { onRetry(operation.operationId) }) {
                            Text("Retry this change")
                        }
                    }
                    OutlinedButton(onClick = { discard = operation }) {
                        Text("Discard and re-enter")
                    }
                }
            }
        }
        if (photos.isNotEmpty()) item(key = "photos-heading") {
            Text("Crop photos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        items(photos, key = { "photo-${it.photoId}" }) { photo ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Photo · " + photo.status,
                        style = MaterialTheme.typography.titleMedium)
                    if (photo.observationId == null) {
                        Text("Waiting for its observation to sync first.")
                    }
                    photo.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    }
                    if (photo.status == "review") {
                        OutlinedButton(onClick = { onRetryPhoto(photo.photoId) }) {
                            Text("Retry photo")
                        }
                    }
                    OutlinedButton(onClick = { discardPhoto = photo }) {
                        Text("Discard local photo")
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
                    "Only retry if this movement still reflects actual farm work.")
            },
            confirmButton = {
                TextButton(onClick = onApproveStock) { Text("Approve and retry") }
            },
            dismissButton = {
                TextButton(onClick = onCancelStock) { Text("Cancel") }
            }
        )
    }

    discardPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { discardPhoto = null },
            title = { Text("Discard offline photo?") },
            text = { Text("The saved picture will be permanently deleted from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    onDiscardPhoto(photo.photoId)
                    discardPhoto = null
                }) { Text("Discard photo") }
            },
            dismissButton = {
                TextButton(onClick = { discardPhoto = null }) { Text("Cancel") }
            }
        )
    }

    discard?.let { operation ->
        AlertDialog(
            onDismissRequest = { discard = null },
            title = { Text("Discard unsynced change?") },
            text = { Text("This local record will be deleted. Re-enter it after checking the latest server data.") },
            confirmButton = {
                TextButton(onClick = {
                    onDiscard(operation.operationId)
                    discard = null
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { discard = null }) { Text("Cancel") }
            }
        )
    }
}
