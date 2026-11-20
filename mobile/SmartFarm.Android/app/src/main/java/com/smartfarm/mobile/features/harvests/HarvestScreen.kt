package com.smartfarm.mobile.features.harvests

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.ui.components.UnsavedChanges
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
    var voidSubmitting by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(false) }

    val receipt = state.saveReceipts["${state.farmId}/${state.cycleId}"]
    var handled by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(receipt) }
    LaunchedEffect(receipt) {
        if (receipt != null && receipt != handled) {
            quantity = ""
            notes = ""
            handled = receipt
        }
    }

    UnsavedChanges("harvest", quantity.isNotEmpty() || notes.isNotEmpty())
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

    RecordPage(title = uiText(R.string.ui_harvests_a2b3922, "Harvests"), onBack = onBack) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FarmHero(if (canRecord) uiText(R.string.ui_record_harvest_f027857, "Record harvest") else uiText(R.string.ui_harvest_records_cfad96b, "Harvest records"),
                    uiText(R.string.ui_record_what_you_picked_then_review_confirmed_totals_b716f93, "Record what you picked, then review confirmed totals."))
                if (state.busy) CircularProgressIndicator()
                state.error?.let {
                    Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text(uiText(R.string.ui_try_again_d8b8392, "Try again")) }
                }
                state.notice?.let {
                    Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.primary,
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
                                label = { Text(uiText(R.string.ui_record_harvest_f027857, "Record harvest")) })
                            FilterChip(selected = selectedView == "history",
                                onClick = { view = "history" },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                label = { Text(uiText(R.string.ui_history_totals_43e1694, "History & totals")) })
                        }
                    }



                    PendingRecords(pending, state.farmId, state.cycleId, setOf("harvest"), onSync)

                    FarmSectionCard(uiText(R.string.ui_confirmed_harvest_total_ea509f7, "Confirmed harvest total"),
                        subtitle = uiText(R.string.ui_only_server_confirmed_non_voided_harvests_are_includ_b4f74d8, "Only server-confirmed, non-voided harvests are included.")) {
                        Text("${overview.totalQuantity.toPlainString()} ${overview.unit}",
                            style = MaterialTheme.typography.headlineMedium)
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FarmStatusChip(uiText(R.string.ui_1_s_valid_5cd02d9, "%1\$s valid", (overview.validHarvestCount)), FarmStatusTone.Active)
                            FarmStatusChip(uiText(R.string.ui_1_s_voided_9bc95a3, "%1\$s voided", (overview.voidedHarvestCount)), FarmStatusTone.Neutral)
                        }
                        if (selectedView == "record") {
                            OutlinedButton(onClick = { view = "history" },
                                modifier = Modifier.fillMaxWidth()) {
                                Text(uiText(R.string.ui_view_harvest_history_83d95b7, "View harvest history"))
                            }
                        }
                    }

                    if (selectedView == "history") {
                        FarmSectionHeader(uiText(R.string.ui_harvest_history_e5ce3cf, "Harvest history"),
                            uiText(R.string.ui_valid_harvests_count_toward_the_total_voided_entries_7d368fa, "Valid harvests count toward the total. Voided entries remain for auditing."))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("all" to uiText(R.string.ui_all_a52ace4, "All"), "valid" to uiText(R.string.ui_valid_f516220, "Valid"),
                                "voided" to uiText(R.string.ui_voided_9f9ae2c, "Voided")).forEach { (key, label) ->
                                FilterChip(selected = historyFilter == key,
                                    onClick = { historyFilter = key },
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                    label = { Text(label) })
                            }
                        }
                        if (visibleEntries.isEmpty()) {
                            FarmEmptyState(uiText(R.string.ui_no_harvest_records_to_show_7deb7d7, "No harvest records to show"),
                                uiText(R.string.ui_confirmed_harvest_entries_will_appear_here_once_sync_1a81da2, "Confirmed harvest entries will appear here once synced."))
                        }
                        if (!canRecord) {
                            Text(uiText(R.string.ui_this_crop_is_no_longer_active_its_confirmed_harvest__ebfe06f, "This crop is no longer active. Its confirmed harvest history remains available."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        // Keep the harvest form in its own lazy item. A tall header/summary item
        // prevents Compose and TalkBack from reliably revealing the Save action.
        if (overview != null && selectedView == "record") {
            item(key = "harvest-form") {
                        FarmSectionCard(uiText(R.string.ui_harvested_amount_fb69d8b, "Harvested amount"),
                            subtitle = uiText(R.string.ui_record_only_what_was_actually_harvested_today_f132a6d, "Record only what was actually harvested today.")) {
                            OutlinedTextField(quantity, { quantity = it },
                                enabled = !state.busy,
                                isError = quantity.isNotBlank() && !validQuantity,
                                label = { Text(uiText(R.string.ui_harvested_quantity_1_s_1a42401, "Harvested quantity (%1\$s)", (overview.unit))) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            if (quantity.isNotBlank() && !validQuantity) {
                                Text(uiText(R.string.ui_use_a_positive_quantity_with_at_most_3_decimal_place_d538afc, "Use a positive quantity, with at most 3 decimal places."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            OutlinedTextField(notes, { notes = it }, enabled = !state.busy,
                                isError = !validNotes, label = { Text(uiText(R.string.ui_notes_optional_6239b93, "Notes (optional)")) },
                                minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                            if (!validNotes)
                                Text(uiText(R.string.ui_harvest_notes_must_be_1_000_characters_or_fewer_07cc656, "Harvest notes must be 1,000 characters or fewer."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            Text(uiText(R.string.ui_partial_harvest_the_crop_stays_active_fb813eb, "Partial harvest · The crop stays active."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { onRecord(quantity.trim(), notes) },
                                enabled = !state.busy && validQuantity && validNotes,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text(uiText(R.string.ui_save_harvested_amount_aefb9bd, "Save harvested amount"))
                            }
                            Text(uiText(R.string.ui_saved_on_this_phone_first_totals_update_after_server_aa94783, "Saved on this phone first. Totals update after server confirmation."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
            }
        }
        if (selectedView == "history") {
            items(visibleEntries, key = { "harvest-${it.id}" }) { entry ->
                FarmSectionCard("${entry.quantity.toPlainString()} ${entry.unit}",
                    subtitle = farmDateTime(entry.harvestedAtUtc, farmTimeZone)) {
                    FarmStatusChip(if (entry.isVoided) uiText(R.string.ui_voided_9f9ae2c, "Voided") else uiText(R.string.ui_valid_f516220, "Valid"),
                        if (entry.isVoided) FarmStatusTone.Failed else FarmStatusTone.Active)
                    entry.notes?.let { Text(it) }
                    if (entry.isVoided) {
                        Text(uiText(R.string.ui_voided_1_s_de98f56, "Voided: %1\$s", (entry.voidReason.orEmpty())))
                    } else if (overview?.canVoid == true && !state.offline) {
                        TextButton(onClick = {
                            voidReason = ""
                            confirmVoidId = entry.id
                        }, enabled = !state.busy) { Text(uiText(R.string.ui_review_voiding_this_harvest_13312e1, "Review voiding this harvest")) }
                    }
                }
            }
        }
    }

    // Keep the reason and dialog until the server confirms the action. An unsuccessful
    // request stays recoverable, with one retry allowed after the busy state ends.
    LaunchedEffect(confirmVoidId, voidSubmitting, state.busy, state.error, state.notice, overview) {
        if (!voidSubmitting || confirmVoidId == null) return@LaunchedEffect
        val confirmedEntry = overview?.entries?.firstOrNull { it.id == confirmVoidId }
        if (confirmedEntry?.isVoided == true ||
            (!state.busy && state.notice != null && state.error == null)) {
            voidSubmitting = false
            confirmVoidId = null
            voidReason = ""
        } else if (!state.busy && state.error != null) {
            voidSubmitting = false
        }
    }

    confirmVoidId?.let { harvestId ->
        val exit = com.smartfarm.mobile.ui.components.rememberExitGuard()
        androidx.compose.runtime.SideEffect { exit.update("void", voidReason.isNotEmpty()) }
        val dismiss = { if (!voidSubmitting && !state.busy) exit.request { confirmVoidId = null; voidReason = "" } }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(uiText(R.string.ui_void_harvest_record_6c7c1d9, "Void harvest record?")) },
            text = {
                Column {
                    Text(uiText(R.string.ui_the_original_entry_stays_in_history_but_will_be_excl_4f9491d, "The original entry stays in history but will be excluded from totals. This requires an online confirmation."))
                    OutlinedTextField(voidReason, { voidReason = it }, enabled = !state.busy && !voidSubmitting,
                        label = { Text(uiText(R.string.ui_reason_required_d976f5d, "Reason (required)")) }, modifier = Modifier.fillMaxWidth())
                    if (state.error != null && !state.busy) {
                        Text(com.smartfarm.mobile.localization.systemMessage(state.error),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !state.busy && !voidSubmitting && voidReason.trim().length in 3..300,
                    onClick = {
                        if (!voidSubmitting) {
                            voidSubmitting = true
                            onVoid(harvestId, voidReason.trim())
                        }
                    }) { Text(uiText(R.string.ui_void_harvest_ce44130, "Void harvest")) }
            },
            dismissButton = {
                TextButton(onClick = dismiss, enabled = !state.busy && !voidSubmitting) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) }
            }
        )
        com.smartfarm.mobile.ui.components.DiscardChangesDialog(exit)
    }
}
