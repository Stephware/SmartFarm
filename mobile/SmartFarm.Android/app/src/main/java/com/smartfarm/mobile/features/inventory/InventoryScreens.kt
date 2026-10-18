package com.smartfarm.mobile.features.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.dto.InventoryTransactionDto

@Composable
private fun InventoryPage(title: String, content: @Composable () -> Unit) {
    Scaffold { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            content()
        }
    }
}

@Composable
private fun Feedback(state: InventoryUiState) {
    if (state.busy) CircularProgressIndicator()
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
fun InventoryListScreen(
    state: InventoryUiState,
    canManage: Boolean,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onCreate: (String, Int, String, String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(1) }
    var unit by rememberSaveable { mutableStateOf("kg") }
    var opening by rememberSaveable { mutableStateOf("0") }
    var reorder by rememberSaveable { mutableStateOf("") }

    InventoryPage("Inventory") {
        OutlinedButton(onClick = onBack) { Text("Back to farm") }
        Feedback(state)
        Text("Low-stock items: ${state.workspace.lowStockCount}")
        if (state.workspace.items.isEmpty()) Text("No inventory items yet.")
        state.workspace.items.forEach { item ->
            Button(onClick = { onOpen(item.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("${item.name} · ${item.quantityOnHand} ${item.unit}" +
                    if (item.isLowStock) " · Low stock" else "")
            }
        }
        if (canManage) {
            HorizontalDivider()
            Text("Create stock item", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            listOf("Seeds", "Fertilizers", "Pesticides", "Other").forEachIndexed { index, label ->
                OutlinedButton(onClick = { category = index + 1 }) {
                    Text("${if (category == index + 1) "✓ " else ""}$label")
                }
            }
            OutlinedTextField(unit, { unit = it }, label = { Text("Unit (kg, L, pcs)") },
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(opening, { opening = it }, label = { Text("Opening quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(reorder, { reorder = it }, label = { Text("Low-stock threshold (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Button(onClick = { onCreate(name, category, unit, opening, reorder) },
                enabled = !state.busy && name.isNotBlank() && unit.isNotBlank()
                    && opening.isNotBlank()) { Text("Add inventory item") }
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
    onReverse: (String, String) -> Unit
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
    var reversalReason by rememberSaveable { mutableStateOf("") }

    InventoryPage(item?.name ?: "Inventory item") {
        OutlinedButton(onClick = onBack) { Text("Inventory list") }
        Feedback(state)
        if (item != null) {
            Text("${item.quantityOnHand} ${item.unit}", style = MaterialTheme.typography.headlineSmall)
            if (item.isLowStock) Text("Low stock", color = MaterialTheme.colorScheme.error)
            if (item.archivedAtUtc != null) Text("Archived — read only")
            if (item.archivedAtUtc == null && canManage) {
                HorizontalDivider()
                Text("Item details", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                listOf("Seeds", "Fertilizers", "Pesticides", "Other").forEachIndexed { index, label ->
                    OutlinedButton(onClick = { category = index + 1 }) {
                        Text("${if (category == index + 1) "✓ " else ""}$label")
                    }
                }
                OutlinedTextField(unit, { unit = it }, label = { Text("Unit") })
                OutlinedTextField(description, { description = it }, label = { Text("Description") })
                OutlinedTextField(reorder, { reorder = it }, label = { Text("Low-stock threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Button(onClick = { onSave(name, category, unit, description, reorder) },
                    enabled = !state.busy) { Text("Save item details") }
                if (item.quantityOnHand.signum() == 0) {
                    OutlinedButton(onClick = onArchive, enabled = !state.busy) { Text("Archive item") }
                }
            }
            if (item.archivedAtUtc == null) {
                HorizontalDivider()
                Text("Stock movement", style = MaterialTheme.typography.titleMedium)
                if (canManage) {
                    listOf(2 to "Receipt", 4 to "Adjustment").forEach { (type, label) ->
                        OutlinedButton(onClick = { movementType = type }) {
                            Text("${if (movementType == type) "✓ " else ""}$label")
                        }
                    }
                }
                OutlinedButton(onClick = { movementType = 3 }) {
                    Text("${if (movementType == 3) "✓ " else ""}Consumption")
                }
                OutlinedTextField(amount, { amount = it }, label = { Text("Signed quantity (+/-)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (movementType == 3) {
                    Text("Choose the related active-crop activity:")
                    state.activities.forEach { activity ->
                        OutlinedButton(onClick = { selectedActivity = activity.id }) {
                            Text("${if (selectedActivity == activity.id) "✓ " else ""}${activity.label}")
                        }
                    }
                }
                OutlinedTextField(reason, { reason = it }, label = { Text("Reason (required for adjustments)") })
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
            Text("Transaction history", style = MaterialTheme.typography.titleLarge)
            val reversedIds = state.transactions.mapNotNull { it.reversesTransactionId }.toSet()
            state.transactions.forEach { transaction ->
                val type = when (transaction.transactionType) {
                    1 -> "Opening"; 2 -> "Receipt"; 3 -> "Consumption"
                    4 -> "Adjustment"; else -> "Reversal"
                }
                Text("$type · ${transaction.quantityChange} ${item.unit} · ${transaction.occurredAtUtc}")
                transaction.reason?.let { Text(it) }
                if (canManage && item.archivedAtUtc == null &&
                    transaction.transactionType != 5 && transaction.id !in reversedIds) {
                    OutlinedTextField(reversalReason, { reversalReason = it },
                        label = { Text("Reversal reason") })
                    OutlinedButton(onClick = { onReverse(transaction.id, reversalReason) },
                        enabled = !state.busy && reversalReason.trim().length >= 3) {
                        Text("Reverse this transaction")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

