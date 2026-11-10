package com.smartfarm.mobile.features.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.platform.testTag
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmEmptyState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.smartfarm.mobile.data.remote.dto.InventoryTransactionDto
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.ui.components.OfflineDataNotice
import com.smartfarm.mobile.ui.components.ConfirmActionButton

@Composable
private fun Feedback(state: InventoryUiState, onRetry: () -> Unit) {
    if (state.busy) CircularProgressIndicator()
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
    }
    OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}

@Composable
fun InventoryListScreen(
    state: InventoryUiState,
    canManage: Boolean,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onCreate: (String, Int, String, String, String) -> Unit,
    onRetry: () -> Unit = {}
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(1) }
    var unit by rememberSaveable { mutableStateOf("kg") }
    var opening by rememberSaveable { mutableStateOf("0") }
    var reorder by rememberSaveable { mutableStateOf("") }

    var creating by rememberSaveable(state.farmId) { mutableStateOf(false) }
    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero("Inventory", "Track farm-wide supplies and stock movements.")
                OutlinedButton(onClick = onBack) { Text("Back to farm") }
                Feedback(state, onRetry)
                FarmSectionCard("Stock overview") {
                    FarmStatusChip("Low-stock items: ${state.workspace.lowStockCount}",
                        if (state.workspace.lowStockCount > 0) FarmStatusTone.Warning else FarmStatusTone.Active)
                    Text("${state.workspace.items.size} available supply items",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.workspace.items.isEmpty() && !state.busy && state.error == null)
                    FarmEmptyState("No inventory items yet.", "Create a supply record to start tracking farm stock.")
                if (canManage && !state.offline)
                    Button(onClick = { creating = !creating }) { Text(if (creating) "Close item form" else "Add stock item") }
            }
        }
        if (creating && canManage && !state.offline) item(key = "create") {
            FarmSectionCard("Create stock item",
                subtitle = "Set the opening balance and an optional low-stock threshold.") {
                OutlinedTextField(name, { name = it }, enabled = !state.busy, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Seeds", "Fertilizers", "Pesticides", "Other").forEachIndexed { index, label ->
                        FilterChip(selected = category == index + 1, onClick = { category = index + 1 },
                            enabled = !state.busy, label = { Text(label) })
                    }
                }
                OutlinedTextField(unit, { unit = it }, enabled = !state.busy, label = { Text("Unit (kg, L, pcs)") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(opening, { opening = it }, enabled = !state.busy, label = { Text("Opening quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(reorder, { reorder = it }, enabled = !state.busy, label = { Text("Low-stock threshold (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Button(onClick = { onCreate(name, category, unit, opening, reorder) },
                    enabled = !state.busy && name.isNotBlank() && unit.isNotBlank()
                        && opening.isNotBlank()) { Text("Add inventory item") }
            }
        }
        items(state.workspace.items, key = { "stock-${it.id}" }) { item ->
            OutlinedCard(onClick = { onOpen(item.id) }, modifier = Modifier.fillMaxWidth().testTag("record-card")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleLarge)
                    Text("${item.quantityOnHand} ${item.unit}",
                        style = MaterialTheme.typography.headlineSmall)
                    FarmStatusChip(if (item.isLowStock) "Low stock" else "In stock",
                        if (item.isLowStock) FarmStatusTone.Warning else FarmStatusTone.Active)
                    Text("View stock history →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }

    }
}

@Composable
fun InventoryItemScreen(
    state: InventoryUiState,
    canManage: Boolean,
    onBack: () -> Unit,
    onSave: (String, Int, String, String, String) -> Unit,
    onArchive: () -> Unit,
    onMovement: (Int, String, String?, String) -> Unit,
    onReverse: (String, String) -> Unit,
    pending: List<PendingOperation> = emptyList(),
    onSync: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val item = state.item
    var name by rememberSaveable(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var category by rememberSaveable(item?.id) { mutableStateOf(item?.category ?: 1) }
    var unit by rememberSaveable(item?.id) { mutableStateOf(item?.unit.orEmpty()) }
    var description by rememberSaveable(item?.id) { mutableStateOf(item?.description.orEmpty()) }
    var reorder by rememberSaveable(item?.id) { mutableStateOf(item?.reorderLevel?.toPlainString().orEmpty()) }
    var movementType by rememberSaveable { mutableStateOf(if (canManage) 2 else 3) }
    var amount by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var selectedActivity by rememberSaveable { mutableStateOf<String?>(null) }

    var editingDetails by rememberSaveable(item?.id) { mutableStateOf(false) }
    val receipt = state.saveReceipts["${state.farmId}/${state.itemId}"]
    var handled by rememberSaveable(state.farmId, state.itemId) { mutableStateOf(receipt) }
    LaunchedEffect(receipt) {
        if (receipt != null && receipt != handled) {
            amount = ""; reason = ""; selectedActivity = null; handled = receipt
        }
    }
    val reversedIds = state.transactions.mapNotNull { it.reversesTransactionId }.toSet()
    RecordPage {
        item(key = "form") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero(item?.name ?: "Inventory item", "Review balances and record farm stock movement.")
        OutlinedButton(onClick = onBack) { Text("Inventory list") }
        Feedback(state, onRetry)
        if (item != null) {
            FarmSectionCard("Available stock") {
                Text("${item.quantityOnHand} ${item.unit}",
                    style = MaterialTheme.typography.headlineSmall)
                FarmStatusChip(
                    if (item.archivedAtUtc != null) "Archived — read only"
                    else if (item.isLowStock) "Low stock" else "In stock",
                    if (item.isLowStock) FarmStatusTone.Warning else FarmStatusTone.Neutral
                )
            }
            if (item.archivedAtUtc == null && canManage && !state.offline) {
                HorizontalDivider()
                OutlinedButton(onClick = { editingDetails = !editingDetails }, enabled = !state.busy) {
                    Text(if (editingDetails) "Close item details" else "Edit item details")
                }
                if (editingDetails) {
                Text("Item details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, enabled = !state.busy, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Seeds", "Fertilizers", "Pesticides", "Other").forEachIndexed { index, label ->
                        FilterChip(selected = category == index + 1, onClick = { category = index + 1 },
                            enabled = !state.busy, label = { Text(label) })
                    }
                }
                OutlinedTextField(unit, { unit = it }, enabled = !state.busy, label = { Text("Unit") })
                OutlinedTextField(description, { description = it }, enabled = !state.busy, label = { Text("Description") })
                OutlinedTextField(reorder, { reorder = it }, enabled = !state.busy, label = { Text("Low-stock threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Button(onClick = { onSave(name, category, unit, description, reorder) },
                    enabled = !state.busy) { Text("Save item details") }
                if (item.quantityOnHand.signum() == 0) {
                    ConfirmActionButton("Archive item", "Archive ${item.name}? Its transaction history will be retained.",
                        enabled = !state.busy, onConfirm = onArchive)
                }
            }
                }
            if (item.archivedAtUtc == null) {
                HorizontalDivider()
                FarmSectionHeader("Stock movement",
                    "Record a receipt, supply use, or adjustment. Saved offline entries are not yet confirmed by the server.")
                if (canManage) {
                    listOf(2 to "Receipt", 4 to "Adjustment").forEach { (type, label) ->
                        FilterChip(selected = movementType == type, onClick = { movementType = type },
                        enabled = !state.busy, label = { Text(label) })
                    }
                }
                FilterChip(selected = movementType == 3, onClick = { movementType = 3 },
                        enabled = !state.busy, label = { Text("Consumption") })
                OutlinedTextField(amount, { amount = it }, enabled = !state.busy, label = { Text(if (movementType == 4) "Adjustment quantity (+/- ${item.unit})" else "Quantity (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text(if (movementType == 3) "Enter the positive amount used; it will be subtracted from stock." else if (movementType == 2) "Enter the positive amount received." else "Positive adds stock; negative removes stock.")
                if (movementType == 3) {
                    Text("Choose the related active-crop activity:")
                    state.activities.forEach { activity ->
                        FilterChip(selected = selectedActivity == activity.id, onClick = { selectedActivity = activity.id },
                        enabled = !state.busy, label = { Text(activity.label) })
                    }
                }
                OutlinedTextField(reason, { reason = it }, enabled = !state.busy, label = { Text("Reason (required for adjustments)") })
                Button(onClick = {
                    onMovement(movementType, amount,
                        if (movementType == 3) selectedActivity else null, reason)
                }, enabled = !state.busy && amount.isNotBlank() &&
                    (movementType != 3 || selectedActivity != null) &&
                    (movementType != 4 || reason.isNotBlank())) {
                    Text("Record stock movement")
                }
            }
            HorizontalDivider()
            FarmSectionHeader("Transaction history", "Existing stock movements and queued local changes.")
            PendingRecords(pending, state.farmId, state.itemId,
                setOf("inventory"), onSync)
        }
            }
        }
        if (item != null) items(state.transactions, key = { "transaction-${it.id}" }) { transaction ->
            var reversalReason by rememberSaveable(transaction.id) { mutableStateOf("") }
                val type = when (transaction.transactionType) {
                    1 -> "Opening"; 2 -> "Receipt"; 3 -> "Consumption"
                    4 -> "Adjustment"; else -> "Reversal"
                }
                FarmSectionCard(type, subtitle = "Recorded ${transaction.occurredAtUtc} UTC") {
                Text("${transaction.quantityChange} ${item.unit}", style = MaterialTheme.typography.titleLarge)
                transaction.reason?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (canManage && !state.offline && item.archivedAtUtc == null &&
                    transaction.transactionType != 5 && transaction.id !in reversedIds) {
                    OutlinedTextField(reversalReason, { reversalReason = it }, enabled = !state.busy,
                        label = { Text("Reversal reason") })
                    ConfirmActionButton("Reverse this transaction",
                        "Reverse ${transaction.quantityChange} ${item.unit} for ${item.name}? This changes the stock balance.",
                        enabled = !state.busy && reversalReason.trim().length >= 3,
                        onConfirm = { onReverse(transaction.id, reversalReason) })
                }
            }
        }
    }
}
