package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.smartfarm.mobile.data.sync.SyncProgress
import com.google.gson.JsonParser
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.features.syncstatus.SyncRecordState
import com.smartfarm.mobile.features.syncstatus.syncState

val LocalSyncProgress = staticCompositionLocalOf { SyncProgress() }

fun pendingRecordDescription(operation: PendingOperation): String = try {
    val json = JsonParser.parseString(operation.payloadJson).asJsonObject
    when (operation.operationType) {
        "harvest" -> "Harvest quantity: " + json.getAsJsonObject("harvest").get("quantity").asString
        "inventory" -> "Stock quantity change: " + json.getAsJsonObject("movement").get("quantityChange").asString
        "activity_stock" -> "Field activity + supply used: " +
            json.getAsJsonObject("activityStock").get("quantityUsed").asString +
            " (both save together after sync)"
        "activity" -> listOf("Watering", "Fertilizing", "Pesticide", "Weeding", "Other")
            .getOrElse(json.getAsJsonObject("activity").get("activityType").asInt - 1) { "Activity" }
        "observation" -> "Observation: " + listOf("Not assessed", "Healthy", "Needs attention", "Critical")
            .getOrElse(json.getAsJsonObject("observation").get("healthStatus").asInt) { "Not assessed" }
        else -> "Saved local record"
    }
} catch (_: Exception) { "Saved local record" }

@Composable
fun PendingRecords(operations: List<PendingOperation>, farmId: String, targetId: String,
    types: Set<String>, onSync: () -> Unit) {
    val pending = operations.filter { operation ->
        operation.farmId == farmId && operation.operationType in types && runCatching {
            JsonParser.parseString(operation.payloadJson).asJsonObject.get("targetId").asString == targetId
        }.getOrDefault(false)
    }
    if (pending.isEmpty()) return
    val progress = LocalSyncProgress.current
    val needsReview = pending.any { syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW }
    FarmSectionCard(
        "Saved on phone",
        subtitle = "These records are not server-confirmed and are not included in the totals above."
    ) {
        pending.forEach { operation ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val transfer = syncState(operation.status, operation.errorMessage,
                    progress.userId == operation.userId && progress.sendingId == operation.operationId)
                FarmStatusChip(
                    when (transfer) { SyncRecordState.NEEDS_REVIEW -> "Needs review"; SyncRecordState.SENDING -> "Sending"; else -> "Saved on phone" },
                    if (transfer == SyncRecordState.NEEDS_REVIEW)
                        FarmStatusTone.Warning else FarmStatusTone.Planned,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                Text(pendingRecordDescription(operation), style = MaterialTheme.typography.bodyMedium)
                operation.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (needsReview || progress.signInRequired) {
            if (progress.signInRequired) Text("Sign in again to send your saved work.")
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text(if (needsReview) "Review saved work" else "Open sync status")
            }
        } else Text("Uploads automatically when a connection is available.")
    }
}
