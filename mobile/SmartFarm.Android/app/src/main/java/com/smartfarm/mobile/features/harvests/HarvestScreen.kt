package com.smartfarm.mobile.features.harvests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.CropArtworkImage
import java.math.BigDecimal
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmEmptyState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.ui.components.OfflineDataNotice

@Composable
fun HarvestScreen(
    state: HarvestUiState,
    onBack: () -> Unit,
    onRecord: (String, String) -> Unit,
    onVoid: (String, String) -> Unit,
    pending: List<PendingOperation> = emptyList(),
    onSync: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var quantity by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var notes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var confirmVoidId by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    var voidReason by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    val receipt = state.saveReceipts["${state.farmId}/${state.cycleId}"]
    var handled by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(receipt) }
    LaunchedEffect(receipt) {
        if (receipt != null && receipt != handled) {
            quantity = ""; notes = ""; handled = receipt
        }
    }
    val overview = state.overview
    val validQuantity = quantity.trim().toBigDecimalOrNull()?.let {
        it > BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 3 &&
            it <= BigDecimal("999999999999999.999")
    } == true
    val validNotes = notes.length <= 1000

    RecordPage {
        item(key = "form") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back to crop cycles") }
            FarmHero("Harvest management", "Record partial harvests and review the crop's yield history.")
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
            }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)

            if (overview != null) {
                FarmSectionCard("${overview.cropName} · ${overview.plotName}",
                    subtitle = "Server-confirmed totals. Unsynced local records are shown separately.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CropArtworkImage(overview.cropName, Modifier.size(92.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${overview.totalQuantity.toPlainString()} ${overview.unit}",
                                style = MaterialTheme.typography.headlineMedium)
                            Text("Total valid harvest quantity",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FarmStatusChip("${overview.validHarvestCount} valid", FarmStatusTone.Active)
                        FarmStatusChip("${overview.voidedHarvestCount} voided", FarmStatusTone.Neutral)
                    }
                    Text("Multiple partial harvests are allowed. Recording does not finish the crop cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                PendingRecords(pending, state.farmId, state.cycleId, setOf("harvest"), onSync)

                if (overview.canRecord) {
                    FarmSectionCard("Record partial harvest",
                        subtitle = "Each saved quantity becomes a separate record; the crop cycle remains active.") {
                    OutlinedTextField(quantity, { quantity = it }, enabled = !state.busy,
                        isError = quantity.isNotBlank() && !validQuantity,
                        label = { Text("Harvested quantity (${overview.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (quantity.isNotBlank() && !validQuantity)
                        Text("Enter a positive quantity with at most three decimal places.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    OutlinedTextField(notes, { notes = it }, enabled = !state.busy,
                        isError = !validNotes, label = { Text("Notes (optional)") },
                        minLines = 2, modifier = Modifier.fillMaxWidth())
                    Text("This harvest will be saved on your device before syncing to the server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { onRecord(quantity.trim(), notes) },
                        enabled = !state.busy && validQuantity && validNotes,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Save partial harvest")
                    }
                    }
                } else {
                    Text("Only active crop cycles can accept new harvest entries.")
                }

                FarmSectionHeader("Harvest history",
                    "Server-confirmed entries. Voided records remain visible but are excluded from yield totals.")
                if (overview.entries.isEmpty())
                    FarmEmptyState("No harvests recorded yet.", "Saved harvests will appear here.")
            }
            }
        }
        items(overview?.entries.orEmpty(), key = { "harvest-${it.id}" }) { entry ->
            FarmSectionCard(
                "${entry.quantity.toPlainString()} ${entry.unit}",
                subtitle = "Recorded ${entry.harvestedAtUtc} UTC"
            ) {
                    FarmStatusChip(if (entry.isVoided) "Voided" else "Valid",
                        if (entry.isVoided) FarmStatusTone.Failed else FarmStatusTone.Active)
                    entry.notes?.let { Text(it) }
                    if (entry.isVoided) {
                        Text("Voided: ${entry.voidReason.orEmpty()}")
                    } else if (overview?.canVoid == true && !state.offline) {
                        OutlinedButton(onClick = {
                            voidReason = ""
                            confirmVoidId = entry.id
                        }, enabled = !state.busy) { Text("Void entry") }
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
                    OutlinedTextField(voidReason, { voidReason = it }, enabled = !state.busy,
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

