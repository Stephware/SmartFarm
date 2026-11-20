package com.smartfarm.mobile.features.inventory

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.ui.components.UnsavedChanges
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.FarmSymbol
import com.smartfarm.mobile.ui.components.FarmLineIcon
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmLoadingState
import com.smartfarm.mobile.ui.components.FarmErrorState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
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
    if (state.busy) FarmLoadingState(
        when {
            state.itemId.isNotBlank() -> if (state.item == null) uiText(R.string.ui_loading_supply_record_f4ada4b, "Loading supply record…") else uiText(R.string.ui_updating_supply_record_0ab7c7b, "Updating supply record…")
            state.workspace.items.isEmpty() -> uiText(R.string.ui_loading_supplies_8d715c5, "Loading supplies…")
            else -> uiText(R.string.ui_updating_supplies_28be079, "Updating supplies…")
        })
    state.error?.let { FarmErrorState(com.smartfarm.mobile.localization.systemMessage(it), onRetry, canRetry = !state.busy) }
    OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
            state.notice?.let { Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
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
    val exitGuard = com.smartfarm.mobile.ui.components.LocalExitGuard.current
    UnsavedChanges("new-supply", creating && (name.isNotEmpty() || category != 1 || unit != "kg" ||
        opening != "0" || reorder.isNotEmpty()), name, category.toString(), unit, opening, reorder)
    var handledCreate by rememberSaveable(state.farmId) { mutableStateOf(state.createSaveVersion) }
    LaunchedEffect(state.createSaveVersion) {
        if (state.createSaveVersion > handledCreate) {
            handledCreate = state.createSaveVersion
            creating = false
            name = ""; category = 1; unit = "kg"; opening = "0"; reorder = ""
            exitGuard?.clear("new-supply")
        }
    }
    val visibleItems = inventoryVisibleItems(state.workspace.items, filter, search)
    var visibleLimit by rememberSaveable(state.farmId, search, filter) { mutableStateOf(20) }
    val openingValid = opening.trim().toBigDecimalOrNull()?.let {
        it >= BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 3 &&
            it <= BigDecimal("999999999999999.999")
    } == true
    val reorderValid = reorder.isBlank() || reorder.trim().toBigDecimalOrNull()?.let {
        it >= BigDecimal.ZERO && it.stripTrailingZeros().scale() <= 3 &&
            it <= BigDecimal("999999999999999.999")
    } == true
    RecordPage(title = uiText(R.string.ui_supplies_6be7560, "Supplies"), onBack = onBack) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FarmHero(uiText(R.string.ui_supplies_6be7560, "Supplies"), uiText(R.string.ui_farm_wide_stock_and_supply_history_80ebce8, "Farm-wide stock and supply history."))
                Feedback(state, onRetry)
                // Do not show a zero-stock dashboard for a new or still-loading farm.
                if (state.workspace.items.isNotEmpty()) {
                FarmSectionCard(uiText(R.string.ui_stock_overview_1c9d9a6, "Stock overview"),
                    subtitle = uiText(R.string.ui_confirmed_farm_balances_pending_changes_excluded_dd66ca3, "Confirmed farm balances; pending changes excluded.")) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("${state.workspace.items.size}", style = MaterialTheme.typography.headlineSmall)
                            Text(uiText(R.string.ui_active_supplies_5d2c7cb, "Active supplies"), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("${state.workspace.lowStockCount}", style = MaterialTheme.typography.headlineSmall,
                                color = if (state.workspace.lowStockCount > 0)
                                    MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                            Text(uiText(R.string.ui_low_stock_5546061, "Low stock"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (state.workspace.lowStockCount > 0) {
                        OutlinedButton(onClick = { filter = "low" },
                            modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_show_low_stock_supplies_57e45e0, "Show low-stock supplies")) }
                    }
                }
                }
                if (state.workspace.items.isNotEmpty()) {
                OutlinedTextField(search, { search = it },
                    label = { Text(uiText(R.string.ui_find_a_supply_345cb7c, "Find a supply")) }, singleLine = true,
                    placeholder = { Text(uiText(R.string.ui_search_by_name_4ae2b33, "Search by name")) }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == "all", onClick = { filter = "all" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        label = { Text(uiText(R.string.ui_all_supplies_a3644ee, "All supplies")) })
                    FilterChip(selected = filter == "low", onClick = { filter = "low" },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        label = { Text(uiText(R.string.ui_low_stock_5546061, "Low stock")) })
                }
                Column {
                    OutlinedButton(onClick = { categoryMenu = true },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(if (filter in listOf("seeds", "fertilizers", "pesticides", "other"))
                            uiText(R.string.ui_category_1_s_b5d4d3e, "Category: %1\$s", (filter.replaceFirstChar { it.uppercase() }))
                            else uiText(R.string.ui_filter_by_supply_category_b001388, "Filter by supply category"))
                    }
                    DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                        listOf("seeds" to uiText(R.string.ui_seeds_45a1954, "Seeds"), "fertilizers" to uiText(R.string.ui_fertilizers_1841450, "Fertilizers"),
                            "pesticides" to uiText(R.string.ui_pesticides_3172816, "Pesticides"), "other" to uiText(R.string.ui_other_f97e9da, "Other")).forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                filter = key; categoryMenu = false
                            })
                        }
                    }
                }
                Text(uiText(R.string.ui_1_s_of_2_s_matching_supplies_3_s_total_87e5504, "%1\$s of %2\$s matching supplies (%3\$s total)", (visibleItems.take(visibleLimit).size), (visibleItems.size), (state.workspace.items.size)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.workspace.items.isEmpty() && !state.busy && state.error == null) {
                    if (state.offline && state.cachedAtUtcMillis == null) {
                        FarmEmptyState(uiText(R.string.ui_supply_records_unavailable_offline_8f2c7c6, "Supply records unavailable offline"),
                            uiText(R.string.ui_reconnect_to_load_saved_stock_balances_do_not_assume_6003786, "Reconnect to load saved stock balances. Do not assume stock is zero."))
                    } else if (state.offline) {
                        FarmEmptyState(uiText(R.string.ui_no_supplies_in_saved_records_02fc6a4, "No supplies in saved records"),
                            uiText(R.string.ui_reconnect_to_check_the_latest_supply_balances_41ab18b, "Reconnect to check the latest supply balances."))
                    } else {
                        FarmEmptyState(uiText(R.string.ui_no_inventory_items_yet_7b827df, "No inventory items yet."),
                            if (canManage) uiText(R.string.ui_add_your_first_supply_to_track_farm_wide_stock_35bbb3b, "Add your first supply to track farm-wide stock.")
                            else uiText(R.string.ui_your_owner_or_manager_can_add_supplies_for_this_farm_1c638a2, "Your owner or manager can add supplies for this farm."))
                    }
                }
                if (state.workspace.items.isNotEmpty() && visibleItems.isEmpty() && !state.busy && state.error == null) {
                    FarmEmptyState(uiText(R.string.ui_no_matching_supplies_6fd89c6, "No matching supplies"), uiText(R.string.ui_change_the_search_or_category_to_see_more_supplies_0fc60a9, "Change the search or category to see more supplies."))
                    OutlinedButton(onClick = { search = ""; filter = "all" },
                        modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_clear_filters_7179ea0, "Clear filters")) }
                }
                if (canManage && !state.offline)
                    Button(onClick = {
                        if (creating) {
                            val close: () -> Unit = {
                                creating = false
                                name = ""; category = 1; unit = "kg"; opening = "0"; reorder = ""
                                exitGuard?.clear("new-supply")
                            }
                            if (exitGuard != null) exitGuard.requestForm("new-supply", close) else close()
                        } else creating = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (creating) uiText(R.string.ui_close_new_supply_form_b8b476a, "Close new supply form") else uiText(R.string.ui_add_supply_ea5451d, "Add supply"))
                    }
            }
        }
        if (creating && canManage && !state.offline) item(key = "create") {
            FarmSectionCard(uiText(R.string.ui_create_stock_item_7c5844d, "Create stock item"),
                subtitle = uiText(R.string.ui_set_the_opening_balance_and_an_optional_low_stock_th_1f6665a, "Set the opening balance and an optional low-stock threshold.")) {
                OutlinedTextField(name, { name = it }, enabled = !state.busy,
                    label = { Text(uiText(R.string.ui_supply_name_cb91cbf, "Supply name")) }, singleLine = true,
                    isError = name.isNotBlank() && name.trim().length !in 2..120,
                    modifier = Modifier.fillMaxWidth())
                if (name.isNotBlank() && name.trim().length !in 2..120)
                    Text(uiText(R.string.ui_use_a_supply_name_between_2_and_120_characters_f889670, "Use a supply name between 2 and 120 characters."),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(uiText(R.string.ui_seeds_45a1954, "Seeds"), uiText(R.string.ui_fertilizers_1841450, "Fertilizers"), uiText(R.string.ui_pesticides_3172816, "Pesticides"), uiText(R.string.ui_other_f97e9da, "Other")).forEachIndexed { index, label ->
                        FilterChip(selected = category == index + 1, onClick = { category = index + 1 },
                            enabled = !state.busy, label = { Text(label) })
                    }
                }
                OutlinedTextField(unit, { unit = it }, enabled = !state.busy,
                    isError = unit.isNotBlank() && unit.trim().length > 20,
                    label = { Text(uiText(R.string.ui_unit_kg_l_pcs_2a622d3, "Unit (kg, L, pcs)")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                if (unit.isNotBlank() && unit.trim().length > 20)
                    Text(uiText(R.string.ui_unit_must_be_20_characters_or_fewer_3c41938, "Unit must be 20 characters or fewer."),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(opening, { opening = it }, enabled = !state.busy,
                    isError = opening.isNotBlank() && !openingValid,
                    label = { Text(uiText(R.string.ui_opening_quantity_f975b8e, "Opening quantity")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (opening.isNotBlank() && !openingValid)
                    Text(uiText(R.string.ui_enter_a_nonnegative_quantity_with_up_to_3_decimal_pl_7220a25, "Enter a nonnegative quantity with up to 3 decimal places."),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(reorder, { reorder = it }, enabled = !state.busy,
                    isError = !reorderValid,
                    label = { Text(uiText(R.string.ui_low_stock_threshold_optional_542e382, "Low-stock threshold (optional)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (!reorderValid)
                    Text(uiText(R.string.ui_enter_a_nonnegative_threshold_with_up_to_3_decimal_p_e46035c, "Enter a nonnegative threshold with up to 3 decimal places."),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                Button(onClick = { onCreate(name.trim(), category, unit.trim(), opening.trim(), reorder.trim()) },
                    enabled = !state.busy && name.trim().length in 2..120 &&
                        unit.trim().length in 1..20 && openingValid && reorderValid,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text(uiText(R.string.ui_create_supply_9556b9b, "Create supply"))
                }
            }
        }
        items(visibleItems.take(visibleLimit), key = { "stock-${it.id}" }) { item ->
            OutlinedCard(onClick = { onOpen(item.id) }, modifier = Modifier.fillMaxWidth().testTag("record-card")) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FarmLineIcon(FarmSymbol.Stock)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(inventoryCategoryName(item.category), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FarmStatusChip(if (item.isLowStock) uiText(R.string.ui_low_stock_5546061, "Low stock") else uiText(R.string.ui_in_stock_f66315e, "In stock"),
                                if (item.isLowStock) FarmStatusTone.Warning else FarmStatusTone.Active)
                        }
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.quantityOnHand.toPlainString(), style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(item.unit, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (visibleItems.size > visibleLimit) item(key = "supplies-more") {
            OutlinedButton(onClick = { visibleLimit += 20 },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_show_20_more_supplies_1_s_remaining_2993829, "Show 20 more supplies (%1\$s remaining)", (visibleItems.size - visibleLimit)))
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
    var handledDetailsSave by rememberSaveable(state.farmId, state.itemId) {
        mutableStateOf(state.detailsSaveVersion)
    }
    LaunchedEffect(state.detailsSaveVersion) {
        if (state.detailsSaveVersion > handledDetailsSave) {
            handledDetailsSave = state.detailsSaveVersion
            editingDetails = false
            state.item?.let { updated ->
                name = updated.name; category = updated.category; unit = updated.unit
                description = updated.description.orEmpty()
                reorder = updated.reorderLevel?.toPlainString().orEmpty()
            }
        }
    }
    val receipt = state.saveReceipts["${state.farmId}/${state.itemId}"]
    var handled by rememberSaveable(state.farmId, state.itemId) { mutableStateOf(receipt) }
    LaunchedEffect(receipt) {
        if (receipt != null && receipt != handled) {
            amount = ""; reason = ""; selectedActivity = null; movementType = 3; handled = receipt
        }
    }
    UnsavedChanges("supply-details", item != null && (name != item.name || category != item.category ||
        unit != item.unit || description != item.description.orEmpty() ||
        reorder != item.reorderLevel?.toPlainString().orEmpty()))
    UnsavedChanges("stock-movement", amount.isNotEmpty() || reason.isNotEmpty() || selectedActivity != null || movementType != 3,
        amount, reason, selectedActivity.orEmpty(), movementType.toString())
    val reversedIds = state.transactions.mapNotNull { it.reversesTransactionId }.toSet()
    val exitGuard = com.smartfarm.mobile.ui.components.LocalExitGuard.current
    LaunchedEffect(reversedIds) {
        reversedIds.forEach { exitGuard?.clear("reversal-$it") }
    }
    RecordPage(title = uiText(R.string.ui_supply_details_734f9d1, "Supply details"), onBack = onBack, backLabel = uiText(R.string.ui_back_to_inventory_0a9d7b3, "Back to inventory")) {
        item(key = "form") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FarmHero(item?.name ?: uiText(R.string.ui_inventory_item_5b747f5, "Inventory item"), uiText(R.string.ui_review_balances_and_record_farm_stock_movement_75913eb, "Review balances and record farm stock movement."))
        Feedback(state, onRetry)
        if (item != null) {
            FarmSectionCard(uiText(R.string.ui_available_stock_8422b62, "Available stock"),
                subtitle = "${inventoryCategoryName(item.category)} · ${item.unit}") {
                Text("${item.quantityOnHand.toPlainString()} ${item.unit}",
                    style = MaterialTheme.typography.headlineMedium)
                item.reorderLevel?.let {
                    Text(uiText(R.string.ui_low_stock_threshold_1_s_2_s_d62b119, "Low-stock threshold: %1\$s %2\$s", (it.toPlainString()), (item.unit)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FarmStatusChip(
                    if (item.archivedAtUtc != null) uiText(R.string.ui_archived_read_only_8f9cf97, "Archived — read only")
                    else if (item.isLowStock) uiText(R.string.ui_low_stock_5546061, "Low stock") else uiText(R.string.ui_in_stock_f66315e, "In stock"),
                    if (item.isLowStock) FarmStatusTone.Warning else FarmStatusTone.Neutral
                )
            }
            if (item.archivedAtUtc == null && canManage && !state.offline) {
                HorizontalDivider()
                OutlinedButton(onClick = { editingDetails = !editingDetails }, enabled = !state.busy) {
                    Text(if (editingDetails) uiText(R.string.ui_close_item_details_ece4fab, "Close item details") else uiText(R.string.ui_edit_item_details_eef6677, "Edit item details"))
                }
                if (editingDetails) {
                Text(uiText(R.string.ui_item_details_e84b48b, "Item details"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, enabled = !state.busy, label = { Text(uiText(R.string.ui_name_dcd1d52, "Name")) }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(uiText(R.string.ui_seeds_45a1954, "Seeds"), uiText(R.string.ui_fertilizers_1841450, "Fertilizers"), uiText(R.string.ui_pesticides_3172816, "Pesticides"), uiText(R.string.ui_other_f97e9da, "Other")).forEachIndexed { index, label ->
                        FilterChip(selected = category == index + 1, onClick = { category = index + 1 },
                            enabled = !state.busy, label = { Text(label) })
                    }
                }
                OutlinedTextField(unit, { unit = it }, enabled = !state.busy, label = { Text(uiText(R.string.ui_unit_4e54596, "Unit")) })
                OutlinedTextField(description, { description = it }, enabled = !state.busy, label = { Text(uiText(R.string.ui_description_526e008, "Description")) })
                OutlinedTextField(reorder, { reorder = it }, enabled = !state.busy, label = { Text(uiText(R.string.ui_low_stock_threshold_812f12c, "Low-stock threshold")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Button(onClick = { onSave(name, category, unit, description, reorder) },
                    enabled = !state.busy) { Text(uiText(R.string.ui_save_item_details_2efd56b, "Save item details")) }
                if (item.quantityOnHand.signum() == 0) {
                    ConfirmActionButton(uiText(R.string.ui_archive_item_0fd57ce, "Archive item"), uiText(R.string.ui_archive_1_s_its_transaction_history_will_be_retained_4ab9320, "Archive %1\$s? Its transaction history will be retained.", (item.name)),
                        enabled = !state.busy, onConfirm = onArchive)
                }
            }
                }
            if (item.archivedAtUtc == null) {
                HorizontalDivider()
                FarmSectionHeader(uiText(R.string.ui_update_stock_9d8db24, "Update stock"),
                    uiText(R.string.ui_choose_what_happened_to_this_supply_every_saved_chan_e594d82, "Choose what happened to this supply. Every saved change syncs from your phone."))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = movementType == 3, onClick = {
                        movementType = 3; amount = ""; selectedActivity = null
                    }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        enabled = !state.busy, label = { Text(uiText(R.string.ui_use_supplies_86b8613, "Use supplies")) })
                    if (canManage) {
                        FilterChip(selected = movementType == 2, onClick = {
                            movementType = 2; amount = ""; selectedActivity = null
                        }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            enabled = !state.busy, label = { Text(uiText(R.string.ui_add_stock_0afe91c, "Add stock")) })
                    }
                }
                if (canManage) {
                    OutlinedButton(onClick = {
                        movementType = 4; amount = ""; selectedActivity = null
                    }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                        Text(if (movementType == 4) uiText(R.string.ui_correct_stock_selected_2bce023, "✓ Correct stock (selected)") else uiText(R.string.ui_correct_stock_count_2751037, "Correct stock count"))
                    }
                }
                val projected = inventoryBalancePreview(item.quantityOnHand, movementType, amount)
                val delta = if (movementType == 4)
                    inventoryCorrectionDelta(item.quantityOnHand, amount) else null
                OutlinedTextField(amount, { amount = it }, enabled = !state.busy,
                    isError = amount.isNotBlank() && projected == null,
                    label = { Text(when (movementType) {
                        2 -> uiText(R.string.ui_quantity_received_1_s_eeb9f3e, "Quantity received (%1\$s)", (item.unit))
                        3 -> uiText(R.string.ui_quantity_used_1_s_7f3de5d, "Quantity used (%1\$s)", (item.unit))
                        else -> uiText(R.string.ui_correct_total_stock_1_s_c81d711, "Correct total stock (%1\$s)", (item.unit))
                    }) },
                    placeholder = { Text(if (movementType == 4) uiText(R.string.ui_count_all_stock_on_hand_aab7967, "Count all stock on hand") else uiText(R.string.ui_example_1_5_c9897d6, "Example: 1.5")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (amount.isNotBlank()) {
                    if (projected == null) {
                        Text(if (movementType == 3)
                            uiText(R.string.ui_enter_a_valid_amount_no_greater_than_the_confirmed_a_99a7ff4, "Enter a valid amount no greater than the confirmed available stock.")
                            else if (movementType == 4)
                                uiText(R.string.ui_enter_a_different_nonnegative_stock_total_up_to_3_de_4e4965e, "Enter a different, nonnegative stock total (up to 3 decimal places).")
                            else uiText(R.string.ui_enter_a_positive_amount_with_at_most_3_decimal_place_7457b47, "Enter a positive amount with at most 3 decimal places."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    } else {
                        FarmSectionCard(uiText(R.string.ui_stock_balance_preview_c8db84d, "Stock balance preview"),
                            subtitle = uiText(R.string.ui_confirmed_stock_only_pending_changes_are_not_include_3abeddc, "Confirmed stock only; pending changes are not included.")) {
                            Text(uiText(R.string.ui_current_1_s_2_s_cbd952a, "Current: %1\$s %2\$s", (item.quantityOnHand.toPlainString()), (item.unit)),
                                style = MaterialTheme.typography.bodyMedium)
                            Text(uiText(R.string.ui_after_sync_1_s_2_s_71d63e8, "After sync: %1\$s %2\$s", (projected.toPlainString()), (item.unit)),
                                style = MaterialTheme.typography.titleMedium)
                            if (movementType == 4 && delta != null) {
                                Text(uiText(R.string.ui_correction_1_s_2_s_3_s_a63ffa8, "Correction: %1\$s%2\$s %3\$s", (if (delta.signum() > 0) "+" else ""), (delta.toPlainString()), (item.unit)))
                            }
                        }
                    }
                }
                if (movementType == 3) {
                    Text(uiText(R.string.ui_which_recorded_crop_activity_used_this_supply_e182676, "Which recorded crop activity used this supply?"),
                        style = MaterialTheme.typography.titleSmall)
                    if (state.activities.isEmpty()) {
                        FarmEmptyState(uiText(R.string.ui_no_activities_to_link_e08f1fd, "No activities to link"),
                            uiText(R.string.ui_record_the_crop_activity_first_then_return_here_to_u_e4f08b3, "Record the crop activity first, then return here to use supplies."))
                    } else {
                        val chosen = state.activities.firstOrNull { it.id == selectedActivity }
                        OutlinedButton(onClick = {
                            // Close the quantity keyboard before opening the activity menu.
                            // Otherwise the popup can be obscured or immediately dismissed.
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            activityMenu = true
                        }, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                            Text(chosen?.label ?: uiText(R.string.ui_choose_recorded_activity_552da25, "Choose recorded activity"))
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
                    Text(uiText(R.string.ui_use_this_only_to_fix_a_physical_count_or_recording_m_fad7a50, "Use this only to fix a physical count or recording mistake."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (movementType == 4 || movementType == 2) {
                    OutlinedTextField(reason, { reason = it }, enabled = !state.busy,
                        label = { Text(if (movementType == 4) uiText(R.string.ui_reason_for_correction_5dede18, "Reason for correction") else uiText(R.string.ui_notes_optional_6239b93, "Notes (optional)")) },
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
                        2 -> uiText(R.string.ui_save_received_stock_2ba937c, "Save received stock")
                        3 -> uiText(R.string.ui_save_supply_use_c8cf5f2, "Save supply use")
                        else -> uiText(R.string.ui_save_corrected_count_95207d2, "Save corrected count")
                    })
                }
                Text(uiText(R.string.ui_saved_on_this_phone_first_the_balance_changes_only_a_1d41cdd, "Saved on this phone first. The balance changes only after server confirmation."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            FarmSectionHeader(uiText(R.string.ui_stock_history_e4dedf7, "Stock history"), uiText(R.string.ui_confirmed_movements_are_separate_from_records_saved__c661602, "Confirmed movements are separate from records saved on this phone."))
            PendingRecords(pending, state.farmId, state.itemId,
                setOf("inventory"), onSync)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAllMovements = !showAllMovements },
                    modifier = Modifier.weight(1f)) {
                    Text(if (showAllMovements) uiText(R.string.ui_show_recent_930df3d, "Show recent") else uiText(R.string.ui_show_all_history_f31e853, "Show all history"))
                }
            }
            if (showAllMovements) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to uiText(R.string.ui_all_a52ace4, "All"), "received" to uiText(R.string.ui_added_6b02e0d, "Added"),
                        "used" to uiText(R.string.ui_used_ae7d8df, "Used"), "corrected" to uiText(R.string.ui_other_f97e9da, "Other")).forEach { (key, label) ->
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
                FarmEmptyState(uiText(R.string.ui_no_movements_to_show_6eb2cd7, "No movements to show"),
                    uiText(R.string.ui_server_confirmed_stock_changes_will_appear_here_11d60c2, "Server-confirmed stock changes will appear here."))
            }
            items(filtered, key = { "transaction-${it.id}" }) { transaction ->
                var reversalReason by rememberSaveable(transaction.id) { mutableStateOf("") }
                var reviewReversal by rememberSaveable(transaction.id) { mutableStateOf(false) }
                UnsavedChanges("reversal-${transaction.id}", reversalReason.isNotEmpty() && transaction.id !in reversedIds)
                val type = when (transaction.transactionType) {
                    1 -> uiText(R.string.ui_opening_stock_21b2ac4, "Opening stock"); 2 -> uiText(R.string.ui_stock_received_28ef913, "Stock received"); 3 -> uiText(R.string.ui_supplies_used_487844c, "Supplies used")
                    4 -> uiText(R.string.ui_stock_correction_e181919, "Stock correction"); else -> uiText(R.string.ui_reversal_7fee49d, "Reversal")
                }
                FarmSectionCard(type,
                    subtitle = farmDateTime(transaction.occurredAtUtc, farmTimeZone)) {
                    Text("${if (transaction.quantityChange.signum() > 0) "+" else ""}${transaction.quantityChange.toPlainString()} ${item.unit}",
                        style = MaterialTheme.typography.titleLarge)
                    transaction.reason?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (canManage && !state.offline && item.archivedAtUtc == null &&
                        transaction.transactionType != 5 && transaction.id !in reversedIds) {
                        TextButton(onClick = { reviewReversal = !reviewReversal }) {
                            Text(if (reviewReversal) uiText(R.string.ui_close_correction_3e4dbce, "Close correction") else uiText(R.string.ui_review_reversal_c89e12d, "Review reversal"))
                        }
                        if (reviewReversal) {
                            OutlinedTextField(reversalReason, { reversalReason = it },
                                enabled = !state.busy, label = { Text(uiText(R.string.ui_reason_to_reverse_ddaaddd, "Reason to reverse")) },
                                modifier = Modifier.fillMaxWidth())
                            ConfirmActionButton(uiText(R.string.ui_reverse_this_transaction_36f2dd8, "Reverse this transaction"),
                                uiText(R.string.ui_reverse_1_s_2_s_for_3_s_this_changes_the_stock_balan_eead121, "Reverse %1\$s %2\$s for %3\$s? This changes the stock balance.", (transaction.quantityChange), (item.unit), (item.name)),
                                enabled = !state.busy && reversalReason.trim().length in 3..300,
                                onConfirm = { onReverse(transaction.id, reversalReason) })
                        }
                    }
                }
            }
        }
    }
}
