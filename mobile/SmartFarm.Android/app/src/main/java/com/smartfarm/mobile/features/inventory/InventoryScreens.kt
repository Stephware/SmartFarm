package com.smartfarm.mobile.features.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
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
import com.smartfarm.mobile.ui.components.farmDateTime

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
    var categoryMenu by rememberSaveable(state.farmId) { mutableStateOf(false) }
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
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == "all", onClick = { filter = "all" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        label = { Text("All supplies") })
                    FilterChip(selected = filter == "low", onClick = { filter = "low" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        label = { Text("Low stock") })
                }
                Column {
                    OutlinedButton(onClick = { categoryMenu = true },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(if (filter in listOf("seeds", "fertilizers", "pesticides", "other"))
                            "Category: ${filter.replaceFirstChar { it.uppercase() }}"
                            else "Filter by supply category")
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        listOf("seeds" to "Seeds", "fertilizers" to "Fertilizers",
                            "pesticides" to "Pesticides", "other" to "Other").forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                filter = key; categoryMenu = false
                            })
                        }
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
    onRetry: () -> Unit = {},
    farmTimeZone: String? = null
) {
    val item = state.item
    var name by rememberSaveable(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var category by rememberSaveable(item?.id) { mutableStateOf(item?.category ?: 1) }
    var unit by rememberSaveable(item?.id) { mutableStateOf(item?.unit.orEmpty()) }
    var description by rememberSaveable(item?.id) { mutableStateOf(item?.description.orEmpty()) }
    var reorder by rememberSaveable(item?.id) { mutableStateOf(item?.reorderLevel?.toPlainString().orEmpty()) }
    var movementType by rememberSaveable(item?.id) { mutableStateOf(3) }
    var amount by rememberSaveable(item?.id) { mutableStateOf("") }
    var reason by rememberSaveable(item?.id) { mutableStateOf("") }
    var selectedActivity by rememberSaveable(item?.id) { mutableStateOf<String?>(null) }
    var activityMenu by rememberSaveable(item?.id) { mutableStateOf(false) }
    var showAllMovements by rememberSaveable(item?.id) { mutableStateOf(false) }
    var historyFilter by rememberSaveable(item?.id) { mutableStateOf("all") }

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
                FarmSectionHeader("Update stock",
                    "Choose what happened to this supply. Every saved change syncs from your phone.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = movementType == 3, onClick = {
                        movementType = 3; amount = ""; selectedActivity = null
                    }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        enabled = !state.busy, label = { Text("Use supplies") })
                    if (canManage) {
                        FilterChip(selected = movementType == 2, onClick = {
                            movementType = 2; amount = ""; selectedActivity = null
                        }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            enabled = !state.busy, label = { Text("Add stock") })
                    }
                }
                if (canManage) {
                    OutlinedButton(onClick = {
                        movementType = 4; amount = ""; selectedActivity = null
                    }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(if (movementType == 4) "✓ Correct stock (selected)" else "Correct stock count")
                    }
                }
                val projected = inventoryBalancePreview(item.quantityOnHand, movementType, amount)
                val delta = if (movementType == 4)
                    inventoryCorrectionDelta(item.quantityOnHand, amount) else null
                OutlinedTextField(amount, { amount = it }, enabled = !state.busy,
                    isError = amount.isNotBlank() && projected == null,
                    label = { Text(when (movementType) {
                        2 -> "Quantity received (${item.unit})"
                        3 -> "Quantity used (${item.unit})"
                        else -> "Correct total stock (${item.unit})"
                    }) },
                    placeholder = { Text(if (movementType == 4) "Count all stock on hand" else "Example: 1.5") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (amount.isNotBlank()) {
                    if (projected == null) {
                        Text(if (movementType == 3)
                            "Enter a valid amount no greater than the confirmed available stock."
                            else if (movementType == 4)
                                "Enter a different, nonnegative stock total (up to 3 decimal places)."
                            else "Enter a positive amount with at most 3 decimal places.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        FarmSectionCard("Stock balance preview",
                            subtitle = "Confirmed stock only; pending changes are not included.") {
                            Text("Current: ${item.quantityOnHand.toPlainString()} ${item.unit}",
                                style = MaterialTheme.typography.bodyMedium)
                            Text("After sync: ${projected.toPlainString()} ${item.unit}",
                                style = MaterialTheme.typography.titleMedium)
                            if (movementType == 4 && delta != null) {
                                Text("Correction: ${if (delta.signum() > 0) "+" else ""}${delta.toPlainString()} ${item.unit}")
                            }
                        }
                    }
                }
                if (movementType == 3) {
                    Text("Which recorded crop activity used this supply?",
                        style = MaterialTheme.typography.titleSmall)
                    if (state.activities.isEmpty()) {
                        FarmEmptyState("No activities to link",
                            "Record the crop activity first, then return here to use supplies.")
                    } else {
                        val chosen = state.activities.firstOrNull { it.id == selectedActivity }
                        OutlinedButton(onClick = { activityMenu = true }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                            Text(chosen?.label ?: "Choose recorded activity")
                        }
                        DropdownMenu(expanded = activityMenu,
                            onDismissRequest = { activityMenu = false }) {
                            state.activities.forEach { activity ->
                                DropdownMenuItem(text = { Text(activity.label) }, onClick = {
                                    selectedActivity = activity.id; activityMenu = false
                                })
                            }
                        }
                    }
                }
                if (movementType == 4) {
                    Text("Use this only to fix a physical count or recording mistake.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (movementType == 4 || movementType == 2) {
                    OutlinedTextField(reason, { reason = it }, enabled = !state.busy,
                        label = { Text(if (movementType == 4) "Reason for correction" else "Notes (optional)") },
                        modifier = Modifier.fillMaxWidth())
                }
                Button(onClick = {
                    val savedAmount = if (movementType == 4) delta?.toPlainString() else amount.trim()
                    if (savedAmount != null) onMovement(
                        movementType, savedAmount,
                        if (movementType == 3) selectedActivity else null,
                        if (movementType == 3) "" else reason.trim()
                    )
                }, enabled = !state.busy && projected != null &&
                    (movementType != 3 || selectedActivity != null) &&
                    (movementType != 4 || reason.trim().length in 3..300),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text(when (movementType) {
                        2 -> "Save received stock"
                        3 -> "Save supply use"
                        else -> "Save corrected count"
                    })
                }
                Text("Saved on this phone first. The balance changes only after server confirmation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            FarmSectionHeader("Stock history", "Confirmed movements are separate from records saved on this phone.")
            PendingRecords(pending, state.farmId, state.itemId,
                setOf("inventory"), onSync)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAllMovements = !showAllMovements },
                    modifier = Modifier.weight(1f)) {
                    Text(if (showAllMovements) "Show recent" else "Show all history")
                }
            }
            if (showAllMovements) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "All", "received" to "Added",
                        "used" to "Used", "corrected" to "Other").forEach { (key, label) ->
                        FilterChip(selected = historyFilter == key,
                            onClick = { historyFilter = key },
                            modifier = Modifier.weight(1f), label = { Text(label) })
                    }
                }
            }
        }
            }
        }
        if (item != null) {
            val filtered = state.transactions.filter { entry ->
                when (historyFilter) {
                    "received" -> entry.transactionType == 2
                    "used" -> entry.transactionType == 3
                    "corrected" -> entry.transactionType !in 2..3
                    else -> true
                }
            }.sortedByDescending { it.occurredAtUtc }
                .let { if (showAllMovements) it else it.take(5) }
            if (filtered.isEmpty()) item(key = "history-empty") {
                FarmEmptyState("No movements to show",
                    "Server-confirmed stock changes will appear here.")
            }
            items(filtered, key = { "transaction-${it.id}" }) { transaction ->
                var reversalReason by rememberSaveable(transaction.id) { mutableStateOf("") }
                var reviewReversal by rememberSaveable(transaction.id) { mutableStateOf(false) }
                val type = when (transaction.transactionType) {
                    1 -> "Opening stock"; 2 -> "Stock received"; 3 -> "Supplies used"
                    4 -> "Stock correction"; else -> "Reversal"
                }
                FarmSectionCard(type,
                    subtitle = farmDateTime(transaction.occurredAtUtc, farmTimeZone)) {
                    Text("${if (transaction.quantityChange.signum() > 0) "+" else ""}${transaction.quantityChange.toPlainString()} ${item.unit}",
                        style = MaterialTheme.typography.titleLarge)
                    transaction.reason?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (canManage && !state.offline && item.archivedAtUtc == null &&
                        transaction.transactionType != 5 && transaction.id !in reversedIds) {
                        TextButton(onClick = { reviewReversal = !reviewReversal }) {
                            Text(if (reviewReversal) "Close correction" else "Review reversal")
                        }
                        if (reviewReversal) {
                            OutlinedTextField(reversalReason, { reversalReason = it },
                                enabled = !state.busy, label = { Text("Reason to reverse") },
                                modifier = Modifier.fillMaxWidth())
                            ConfirmActionButton("Reverse this transaction",
                                "Reverse ${transaction.quantityChange} ${item.unit} for ${item.name}? This changes the stock balance.",
                                enabled = !state.busy && reversalReason.trim().length in 3..300,
                                onConfirm = { onReverse(transaction.id, reversalReason) })
                        }
                    }
                }
            }
        }
    }
}
