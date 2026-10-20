package com.smartfarm.mobile.features.harvests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.OfflineDataNotice

@Composable
fun HarvestScreen(
    state: HarvestUiState,
    onBack: () -> Unit,
    onRecord: (String, String) -> Unit,
    onVoid: (String, String) -> Unit
) {
    var quantity by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var confirmVoidId by rememberSaveable { mutableStateOf<String?>(null) }
    var voidReason by rememberSaveable { mutableStateOf("") }
    val overview = state.overview

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text("Back to crop cycles") }
            Text("Harvest management", style = MaterialTheme.typography.headlineMedium)
            if (state.busy) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)

            if (overview != null) {
                Text("${overview.cropName} · ${overview.plotName}",
                    style = MaterialTheme.typography.titleLarge)
                Text("Total yield: ${overview.totalQuantity.toPlainString()} ${overview.unit}",
                    style = MaterialTheme.typography.titleMedium)
                Text("${overview.validHarvestCount} valid · ${overview.voidedHarvestCount} voided")
                Text("Harvesting does not automatically complete the crop cycle.")

                if (overview.canRecord) {
                    HorizontalDivider()
                    Text("Record partial harvest", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity (${overview.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onRecord(quantity, notes) },
                        enabled = !state.busy && quantity.isNotBlank()) { Text("Record harvest") }
                } else {
                    Text("Only active crop cycles can accept new harvest entries.")
                }

                HorizontalDivider()
                Text("Harvest history", style = MaterialTheme.typography.titleLarge)
                if (overview.entries.isEmpty()) Text("No harvests recorded yet.")
                overview.entries.forEach { entry ->
                    Text("${entry.quantity.toPlainString()} ${entry.unit}" +
                        if (entry.isVoided) " · Voided" else "",
                        style = MaterialTheme.typography.titleMedium)
                    Text(entry.harvestedAtUtc)
                    entry.notes?.let { Text(it) }
                    if (entry.isVoided) {
                        Text("Voided: ${entry.voidReason.orEmpty()}")
                    } else if (overview.canVoid && !state.offline) {
                        OutlinedButton(onClick = {
                            voidReason = ""
                            confirmVoidId = entry.id
                        }, enabled = !state.busy) { Text("Void entry") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    confirmVoidId?.let { harvestId ->
        AlertDialog(
            onDismissRequest = { confirmVoidId = null },
            title = { Text("Void harvest record?") },
            text = {
                Column {
                    Text("The original entry remains in history and is excluded from totals.")
                    OutlinedTextField(voidReason, { voidReason = it },
                        label = { Text("Reason (required)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = !state.busy && voidReason.trim().length in 3..300,
                    onClick = {
                        confirmVoidId = null
                        onVoid(harvestId, voidReason)
                    }) { Text("Void harvest") }
            },
            dismissButton = {
                TextButton(onClick = { confirmVoidId = null }) { Text("Cancel") }
            }
        )
    }
}

