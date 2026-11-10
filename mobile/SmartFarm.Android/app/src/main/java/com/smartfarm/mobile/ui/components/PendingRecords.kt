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
import com.google.gson.JsonParser
import com.smartfarm.mobile.data.local.entity.PendingOperation

fun pendingRecordDescription(operation: PendingOperation): String = try {
    val json = JsonParser.parseString(operation.payloadJson).asJsonObject
    when (operation.operationType) {
        "harvest" -> "Harvest quantity: " + json.getAsJsonObject("harvest").get("quantity").asString
        "inventory" -> "Stock quantity change: " + json.getAsJsonObject("movement").get("quantityChange").asString
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
    FarmSectionCard(
        "Saved on this device",
        subtitle = "These records are not server-confirmed and are not included in the totals above."
    ) {
        pending.forEach { operation ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FarmStatusChip(
                    if (operation.status == "review") "Needs review" else "Waiting to sync",
                    if (operation.status == "review") FarmStatusTone.Warning else FarmStatusTone.Planned
                )
                Text(pendingRecordDescription(operation), style = MaterialTheme.typography.bodyMedium)
                operation.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
            Text("Review sync status")
        }
    }
}
