package com.smartfarm.mobile.ui.components

import androidx.compose.material3.MaterialTheme
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
    Text("Saved on this device", style = MaterialTheme.typography.titleMedium)
    Text("These records are not server-confirmed and are not included in the totals above.")
    pending.forEach { operation ->
        Text(pendingRecordDescription(operation) + if (operation.status == "review") " · Needs review" else " · Waiting to sync")
        operation.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
    OutlinedButton(onClick = onSync) { Text("Review sync status") }
}
