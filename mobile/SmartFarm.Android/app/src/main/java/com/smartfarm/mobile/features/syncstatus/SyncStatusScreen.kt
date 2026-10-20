package com.smartfarm.mobile.features.syncstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.smartfarm.mobile.data.local.entity.PendingOperation

private fun details(operation: PendingOperation): String = try {
    val json = JsonParser.parseString(operation.payloadJson).asJsonObject
    when (operation.operationType) {
        "harvest" -> "Quantity: " + json.getAsJsonObject("harvest").get("quantity").asString
        "inventory" -> {
            val movement = json.getAsJsonObject("movement")
            "Stock quantity change: " + movement.get("quantityChange").asString
        }
        "activity" -> {
            val activity = json.getAsJsonObject("activity")
            "Activity type: " + activity.get("activityType").asString
        }
        "observation" -> {
            val observation = json.getAsJsonObject("observation")
            "Health status: " + observation.get("healthStatus").asString
        }
        else -> operation.operationType
    }
} catch (_: Exception) {
    "Saved local record"
}

@Composable
fun SyncStatusScreen(operations: List<PendingOperation>, onBack: () -> Unit,
    onRetry: (String) -> Unit, onDiscard: (String) -> Unit, onSyncNow: () -> Unit) {
    var discard by remember { mutableStateOf<PendingOperation?>(null) }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to account") }
            Text("Offline sync", style = MaterialTheme.typography.headlineMedium)
            Text("Pending: " + operations.count { it.status == "pending" } +
                " · Needs review: " + operations.count { it.status == "review" })
            Text("Changes stay on this device until the server confirms them. " +
                "Check rejected records before retrying; stock may need a newer version.")
            Button(onClick = onSyncNow) { Text("Sync now") }
            if (operations.isEmpty()) Text("All local changes are synced.")
            operations.forEach { operation ->
                Text(operation.operationType.replaceFirstChar { it.uppercase() } +
                    " · " + operation.status, style = MaterialTheme.typography.titleMedium)
                Text(details(operation))
                operation.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (operation.status == "review") {
                    OutlinedButton(onClick = { onRetry(operation.operationId) }) {
                        Text("Retry this change")
                    }
                    OutlinedButton(onClick = { discard = operation }) {
                        Text("Discard and re-enter")
                    }
                }
            }
        }
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
