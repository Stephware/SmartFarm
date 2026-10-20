package com.smartfarm.mobile.features.syncstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.local.entity.PendingOperation

@Composable
fun SyncStatusScreen(operations: List<PendingOperation>, onBack: () -> Unit,
    onRetry: (String) -> Unit, onSyncNow: () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to account") }
            Text("Offline sync", style = MaterialTheme.typography.headlineMedium)
            Text("Pending: " + operations.count { it.status == "pending" } +
                " · Needs review: " + operations.count { it.status == "review" })
            Text("Pending entries stay on this device until the server confirms them. " +
                "Review rejected entries before retrying.")
            Button(onClick = onSyncNow) { Text("Sync now") }
            if (operations.isEmpty()) Text("All local changes are synced.")
            operations.forEach { operation ->
                Text(operation.operationType.replaceFirstChar { it.uppercase() } +
                    " · " + operation.status, style = MaterialTheme.typography.titleMedium)
                Text("Farm: " + operation.farmId)
                operation.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (operation.status == "review") {
                    OutlinedButton(onClick = { onRetry(operation.operationId) }) {
                        Text("Retry this change")
                    }
                }
            }
        }
    }
}
