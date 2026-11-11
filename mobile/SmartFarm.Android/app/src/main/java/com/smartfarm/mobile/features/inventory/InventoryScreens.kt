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
import com.smartfarm.mobile.ui.components.FarmSymbol
import com.smartfarm.mobile.ui.components.FarmLineIcon
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
import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import java.math.BigDecimal
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
    var search by rememberSaveable(state.farmId) { mutableStateOf("") }
    var filter by rememberSaveable(state.farmId) { mutableStateOf("all") }
    val visibleItems = inventoryVisibleItems(state.workspace.items, filter, search)
    val openingValid = opening.trim().toBigDecimalOrNull()?.let {
        it >= BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 3 &&
            it <= BigDecimal("999999999999999.999")
    } == true
    val reorderValid = reorder.isBlank() || reorder.trim().toBigDecimalOrNull()?.let {
        it >= BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 3 &&
            it <= BigDecimal("999999999999999.999")
    } == true
    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero("Inventory", "Track farm-wide supplies and stock movements.")
                OutlinedButton(onClick = onBack) { Text("Back to farm") }
                Feedback(state, onRetry)
                FarmSectionCard("Stock overview",
                    subtitle = "Farm-wide balances, excluding changes awaiting server confirmation.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${state.workspace.items.size}", style = MaterialTheme.typography.headlineSmall)
                            Text("Active supplies", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${state.workspace.lowStockCount}", style = MaterialTheme.typography.headlineSmall,
                                color = if (state.workspace.lowStockCount > 0)
                                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                            Text("Low stock", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.workspace.lowStockCount > 0) {
                        FarmStatusChip("Review supplies needing replenishment", FarmStatusTone.Warning)
                        OutlinedButton(onClick = { filter = "low" },
                            modifier = Modifier.fillMaxWidth()) { Text("Show low-stock supplies") }
                    }
                }
                OutlinedTextField(search, { search = it },
                    label = { Text("Find a supply") }, singleLine = true,
                    placeholder = { Text("Search by name") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All", "low" to "Low stock", "seeds" to "Seeds",
                        "fertilizers" to "Fertilizers", "pesticides" to "Pesticides", "other" to "Other")
                        .forEach { (key, label) ->
                            FilterChip(selected = filter == key, onClick = { filter = key },
                                label = { Text(label) })
                        }
                }
                Text("${visibleItems.size} of ${state.workspace.items.size} supplies shown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.workspace.items.isEmpty() && !state.busy && state.error == null)
                    FarmEmptyState("No inventory items yet.", "Create a supply record to start tracking farm stock.")
                if (state.workspace.items.isNotEmpty() && visibleItems.isEmpty() && !state.busy) {
                    FarmEmptyState("No matching supplies", "Change the search or category to see more supplies.")
                    OutlinedButton(onClick = { search = ""; filter = "all" },
                        modifier = Modifier.fillMaxWidth()) { Text("Clear filters") }
                }
                if (canManage && !state.offline)
                    Button(onClick = { creating = !creating }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (creating) "Close new supply form" else "Add supply")
                    }
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
                    enabled = !state.busy && name.trim().length in 2..120 &&
                        unit.isNotBlank() && unit.length <= 20 && openingValid && reorderValid) {
                    Text("Create supply")
                }
            }
        }
        items(visibleItems, key = { "stock-${it.id}" }) { item ->
            OutlinedCard(onClick = { onOpen(item.id) }, modifier = Modifier.fillMaxWidth().testTag("record-card")) {
                Row(Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FarmLineIcon(FarmSymbol.Stock)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(inventoryCategoryName(item.category), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FarmStatusChip(if (item.isLowStock) "Low stock" else "In stock",
                            if (item.isLowStock) FarmStatusTone.Warning else FarmStatusTone.Active)
                    }
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.quantityOnHand.toPlainString(), style = MaterialTheme.typography.titleLarge)
                        Text(item.unit, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
            FarmSectionCard("Available stock",
                subtitle = "${inventoryCategoryName(item.category)} · ${item.unit}") {
                Text("${item.quantityOnHand.toPlainString()} ${item.unit}",
                    style = MaterialTheme.typography.headlineMedium)
                item.reorderLevel?.let {
                    Text("Low-stock threshold: ${it.toPlainString()} ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if (canManage) listOf(2 to "Receipt +", 3 to "Consumption −", 4 to "Adjustment ±")
                    else listOf(3 to "Consumption −")).forEach { (type, label) ->
                        FilterChip(selected = movementType == type, onClick = {
                            movementType = type
                            selectedActivity = null
                            amount = ""
                        }, enabled = !state.busy, label = { Text(label) })
                    }
                }
                val amountValid = runCatching { inventoryMovementQuantity(movementType, amount.trim()) }.isSuccess
                OutlinedTextField(amount, { amount = it }, enabled = !state.busy,
                    isError = amount.isNotBlank() && !amountValid,
                    label = { Text(if (movementType == 4) "Adjustment (+/- ${item.unit})" else "Quantity (${item.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (amount.isNotBlank() && !amountValid)
                    Text("Enter a valid nonzero quantity with no more than 3 decimals.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                }, enabled = !state.busy && amountValid &&
                    (movementType != 3 || selectedActivity != null) &&
                    (movementType != 4 || reason.trim().length in 3..300),
                    modifier = Modifier.fillMaxWidth()) {
                    Text(when (movementType) {
                        2 -> "Record received stock"
                        3 -> "Record supply use"
                        else -> "Save stock adjustment"
                    })
                }
                Text("Saved on this device first. Stock totals update after server confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
