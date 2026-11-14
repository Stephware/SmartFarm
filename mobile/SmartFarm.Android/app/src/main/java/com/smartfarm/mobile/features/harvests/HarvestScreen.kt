package com.smartfarm.mobile.features.harvests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.OfflineDataNotice
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.farmDateTime
import java.math.BigDecimal

/** Harvest entries remain separate partial records. Only the server confirms totals. */
@Composable
fun HarvestScreen(
    state: HarvestUiState,
    onBack: () -> Unit,
    onRecord: (String, String) -> Unit,
    onVoid: (String, String) -> Unit,
    pending: List<PendingOperation> = emptyList(),
    onSync: () -> Unit = {},
    onRetry: () -> Unit = {},
    farmTimeZone: String? = null
) {
    var quantity by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var notes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var view by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("record") }
    var historyFilter by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("all") }
    var confirmVoidId by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    var voidReason by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }

    val receipt = state.saveReceipts["${state.farmId}/${state.cycleId}"]
    var handled by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(receipt) }
    LaunchedEffect(receipt) {
        if (receipt != null && receipt != handled) {
            quantity = ""
            notes = ""
            handled = receipt
        }
    }

    val overview = state.overview
    val canRecord = overview?.canRecord == true
    val selectedView = if (!canRecord) "history" else view
    val validQuantity = quantity.trim().toBigDecimalOrNull()?.let {
        it > BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 3 &&
            it <= BigDecimal("999999999999999.999")
    } == true
    val validNotes = notes.length <= 1000
    val visibleEntries = overview?.entries.orEmpty().filter { entry ->
        when (historyFilter) {
            "valid" -> !entry.isVoided
            "voided" -> entry.isVoided
            else -> true
        }
    }.sortedByDescending { it.harvestedAtUtc }

    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                FarmHero(if (canRecord) "Record harvest" else "Harvest records",
                    "Save each harvest amount separately. Check confirmed totals and history.")
                if (state.busy) CircularProgressIndicator()
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
                }
                state.notice?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                OfflineDataNotice(state.offline, state.cachedAtUtcMillis)

                if (overview != null) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(overview.cropName, style = MaterialTheme.typography.titleLarge)
                            Text(overview.plotName, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (canRecord) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = selectedView == "record",
                                onClick = { view = "record" },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                label = { Text("Record harvest") })
                            FilterChip(selected = selectedView == "history",
                                onClick = { view = "history" },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                label = { Text("History & totals") })
                        }
                    }

                    if (selectedView == "record") {
                        FarmSectionCard("Harvested amount",
                            subtitle = "Record only what was actually harvested today.") {
                            OutlinedTextField(quantity, { quantity = it },
                                enabled = !state.busy,
                                isError = quantity.isNotBlank() && !validQuantity,
                                label = { Text("Harvested quantity (${overview.unit})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            if (quantity.isNotBlank() && !validQuantity) {
                                Text("Use a positive quantity, with at most 3 decimal places.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            OutlinedTextField(notes, { notes = it }, enabled = !state.busy,
                                isError = !validNotes, label = { Text("Notes (optional)") },
                                minLines = 2, modifier = Modifier.fillMaxWidth())
                            Text("This is one partial harvest. It does not finish the crop cycle.",
                                style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { onRecord(quantity.trim(), notes) },
                                enabled = !state.busy && validQuantity && validNotes,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text("Save harvested amount")
                            }
                            Text("Saved on this phone first. The server sets the harvest date and confirms totals when sent.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    PendingRecords(pending, state.farmId, state.cycleId, setOf("harvest"), onSync)

                    FarmSectionCard("Confirmed harvest total",
                        subtitle = "Only server-confirmed, non-voided harvests are included.") {
                        Text("${overview.totalQuantity.toPlainString()} ${overview.unit}",
                            style = MaterialTheme.typography.headlineMedium)
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FarmStatusChip("${overview.validHarvestCount} valid", FarmStatusTone.Active)
                            FarmStatusChip("${overview.voidedHarvestCount} voided", FarmStatusTone.Neutral)
                        }
                        if (selectedView == "record") {
                            OutlinedButton(onClick = { view = "history" },
                                modifier = Modifier.fillMaxWidth()) {
                                Text("View harvest history")
                            }
                        }
                    }

                    if (selectedView == "history") {
                        FarmSectionHeader("Harvest history",
                            "Valid harvests count toward the total. Voided entries remain for auditing.")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("all" to "All", "valid" to "Valid",
                                "voided" to "Voided").forEach { (key, label) ->
                                FilterChip(selected = historyFilter == key,
                                    onClick = { historyFilter = key },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                    label = { Text(label) })
                            }
                        }
                        if (visibleEntries.isEmpty()) {
                            FarmEmptyState("No harvest records to show",
                                "Confirmed harvest entries will appear here once synced.")
                        }
                        if (!canRecord) {
                            Text("This crop is no longer active. Its confirmed harvest history remains available.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (selectedView == "history") {
            items(visibleEntries, key = { "harvest-${it.id}" }) { entry ->
                FarmSectionCard("${entry.quantity.toPlainString()} ${entry.unit}",
                    subtitle = farmDateTime(entry.harvestedAtUtc, farmTimeZone)) {
                    FarmStatusChip(if (entry.isVoided) "Voided" else "Valid",
                        if (entry.isVoided) FarmStatusTone.Failed else FarmStatusTone.Active)
                    entry.notes?.let { Text(it) }
                    if (entry.isVoided) {
                        Text("Voided: ${entry.voidReason.orEmpty()}")
                    } else if (overview?.canVoid == true && !state.offline) {
                        TextButton(onClick = {
                            voidReason = ""
                            confirmVoidId = entry.id
                        }, enabled = !state.busy) { Text("Review voiding this harvest") }
                    }
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
                    Text("The original entry stays in history but will be excluded from totals. This requires an online confirmation.")
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
