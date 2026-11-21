package com.smartfarm.mobile.features.crops

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.ui.components.UnsavedChanges
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.smartfarm.mobile.ui.components.DateInput
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.ui.components.OfflineDataNotice

@Composable
fun CropScreen(
    state: CropUiState,
    canManage: Boolean,
    onBack: () -> Unit,
    onCreateType: (String, String, String, String) -> Unit,
    onPlan: (String, String, String, String) -> Unit,
    onUpdatePlan: (CropCycleDto, String, String) -> Unit,
    onMonitoring: (CropCycleDto) -> Unit,
    onHarvests: (CropCycleDto) -> Unit,
    onTransition: (CropCycleDto, Int, String, String) -> Unit,
    onRetry: () -> Unit = {},
    onQuickMonitoring: (CropCycleDto, String) -> Unit = { cycle, _ -> onMonitoring(cycle) },
    onPlots: () -> Unit = {}
) {
    var selectedView by rememberSaveable(state.farmId) { mutableStateOf("growing") }
    var selectedCycleId by rememberSaveable(state.farmId) { mutableStateOf<String?>(null) }
    var typeName by rememberSaveable(state.farmId) { mutableStateOf("") }
    var unit by rememberSaveable(state.farmId) { mutableStateOf("kg") }
    var growth by rememberSaveable(state.farmId) { mutableStateOf("") }
    var watering by rememberSaveable(state.farmId) { mutableStateOf("") }
    var selectedPlot by rememberSaveable(state.farmId) { mutableStateOf("") }
    var selectedType by rememberSaveable(state.farmId) { mutableStateOf("") }
    var plannedDate by rememberSaveable(state.farmId) { mutableStateOf("") }
    var harvestDate by rememberSaveable(state.farmId) { mutableStateOf("") }
    var choosePlot by rememberSaveable(state.farmId) { mutableStateOf(false) }
    var chooseCrop by rememberSaveable(state.farmId) { mutableStateOf(false) }
    var addingType by rememberSaveable(state.farmId) { mutableStateOf(false) }

    UnsavedChanges("crop-type", typeName.isNotEmpty() || unit != "kg" || growth.isNotEmpty() || watering.isNotEmpty(),
        typeName, unit, growth, watering)
    UnsavedChanges("crop-plan", selectedPlot.isNotEmpty() || selectedType.isNotEmpty() ||
        plannedDate.isNotEmpty() || harvestDate.isNotEmpty(), selectedPlot, selectedType, plannedDate, harvestDate)
    val formState = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val exitGuard = com.smartfarm.mobile.ui.components.LocalExitGuard.current
    var handledTypeSave by rememberSaveable(state.farmId) { mutableStateOf(state.typeSaveVersion) }
    var handledPlanSave by rememberSaveable(state.farmId) { mutableStateOf(state.planSaveVersion) }
    var handledPlanEdit by rememberSaveable(state.farmId) { mutableStateOf(state.planEditSaveVersion) }
    LaunchedEffect(state.typeSaveVersion) {
        if (state.typeSaveVersion > handledTypeSave) {
            handledTypeSave = state.typeSaveVersion
            addingType = false
            typeName = ""; unit = "kg"; growth = ""; watering = ""
            exitGuard?.clear("crop-type")
        }
    }
    LaunchedEffect(state.planSaveVersion) {
        if (state.planSaveVersion > handledPlanSave) {
            handledPlanSave = state.planSaveVersion
            selectedPlot = ""; selectedType = ""; plannedDate = ""; harvestDate = ""
            choosePlot = false; chooseCrop = false
            selectedView = "planned"
            exitGuard?.clear("crop-plan")
        }
    }
    LaunchedEffect(state.planEditSaveVersion) {
        if (state.planEditSaveVersion > handledPlanEdit) {
            handledPlanEdit = state.planEditSaveVersion
            selectedCycleId?.let { exitGuard?.clear("cycle-$it") }
            selectedCycleId = null
        }
    }
    val active = state.cycles.filter { it.status == 1 }
        .sortedWith(compareBy<CropCycleDto>({ it.plotName }, { it.cropTypeName }))
    val planned = state.cycles.filter { it.status == 0 }
        .sortedWith(compareBy<CropCycleDto>({ it.plannedPlantingDate }, { it.plotName }))
    val previous = state.cycles.filter { it.status !in 0..1 }
        .sortedByDescending { it.plantedDate ?: it.plannedPlantingDate }
    val displayed = when (selectedView) {
        "growing" -> active
        "planned" -> planned
        "history" -> previous
        else -> emptyList()
    }
    val browse = selectedView in setOf("growing", "planned", "history")
    var displayedLimit by rememberSaveable(state.farmId, selectedView) { mutableStateOf(20) }
    RecordPage(title = uiText(R.string.ui_crops_0abd7c5, "Crops"), onBack = onBack) {
        item(key = "overview") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero(uiText(R.string.ui_crops_0abd7c5, "Crops"), uiText(R.string.ui_growing_crops_planting_plans_and_history_3fe33f4, "Growing crops, planting plans and history."))
                if (state.busy) FarmLoadingState(
                    if (state.cycles.isEmpty()) uiText(R.string.ui_loading_crop_records_268b8fe, "Loading crop records…") else uiText(R.string.ui_updating_crop_records_1119ae6, "Updating crop records…"))
                state.error?.let { FarmErrorState(com.smartfarm.mobile.localization.systemMessage(it), onRetry, canRetry = !state.busy) }
                OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("growing" to uiText(R.string.ui_growing_52c8c11, "Growing"), "planned" to uiText(R.string.ui_planned_fb6f7c9, "Planned"), "history" to uiText(R.string.ui_history_0e76960, "History"))
                        .forEach { (key, label) ->
                            FilterChip(selected = selectedView == key,
                                onClick = { selectedView = key; selectedCycleId = null },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                label = { Text(label) })
                        }
                }
                if (canManage) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = {
                            selectedView = if (selectedView == "plan") "planned" else "plan"
                            selectedCycleId = null
                        }, enabled = !state.offline && !state.busy,
                            modifier = Modifier.weight(1f).widthIn(min = 140.dp).defaultMinSize(minHeight = 48.dp)) {
                            Text(if (selectedView == "plan") uiText(R.string.ui_close_plan_f67e5af, "Close plan") else uiText(R.string.ui_plan_a_crop_b56f05c, "Plan a crop"))
                        }
                        TextButton(onClick = {
                            selectedView = if (selectedView == "types") "growing" else "types"
                            selectedCycleId = null
                        }, modifier = Modifier.weight(1f).widthIn(min = 140.dp).defaultMinSize(minHeight = 48.dp)) {
                            Text(uiText(R.string.ui_crop_types_2483e61, "Crop types"))
                        }
                    }
                }
                if (browse) {
                    val title = when (selectedView) {
                        "growing" -> uiText(R.string.ui_growing_now_55183dc, "Growing now")
                        "planned" -> uiText(R.string.ui_ready_for_planting_2e25e3f, "Ready for planting")
                        else -> uiText(R.string.ui_past_crops_7256c67, "Past crops")
                    }
                    FarmSectionHeader(title, when (selectedView) {
                        "growing" -> uiText(R.string.ui_tap_a_crop_to_record_fieldwork_take_photos_or_view_h_5cdf4e4, "Tap a crop to record fieldwork, take photos, or view harvests.")
                        "planned" -> uiText(R.string.ui_tap_a_plan_to_review_it_or_mark_the_planting_as_star_1cd8c0c, "Tap a plan to review it or mark the planting as started.")
                        else -> uiText(R.string.ui_completed_failed_and_cancelled_crop_cycles_b127fce, "Completed, failed and cancelled crop cycles.")
                    })
                    if (!state.busy && state.error == null && displayed.isEmpty()) {
                        if (state.offline && state.cachedAtUtcMillis == null) {
                            FarmEmptyState(uiText(R.string.ui_crop_records_unavailable_offline_6e01d64, "Crop records unavailable offline"),
                                uiText(R.string.ui_reconnect_to_load_crop_records_no_crop_status_is_ass_4a70d70, "Reconnect to load crop records. No crop status is assumed."))
                        } else if (state.offline) {
                            FarmEmptyState(when (selectedView) {
                                "growing" -> uiText(R.string.ui_no_growing_crops_in_saved_records_ae47a35, "No growing crops in saved records")
                                "planned" -> uiText(R.string.ui_no_planting_plans_in_saved_records_303f722, "No planting plans in saved records")
                                else -> uiText(R.string.ui_no_past_crops_in_saved_records_5c040b6, "No past crops in saved records")
                            }, uiText(R.string.ui_reconnect_to_check_the_latest_crop_records_3463baa, "Reconnect to check the latest crop records."))
                        } else {
                            FarmEmptyState(when (selectedView) {
                                "growing" -> uiText(R.string.ui_nothing_growing_yet_b390f35, "Nothing growing yet")
                                "planned" -> uiText(R.string.ui_no_planting_plans_a06855c, "No planting plans")
                                else -> uiText(R.string.ui_no_crop_history_yet_abbb3f0, "No crop history yet")
                            }, when (selectedView) {
                                "growing" -> if (canManage)
                                    uiText(R.string.ui_make_a_planting_plan_then_mark_it_as_planted_to_star_b807e31, "Make a planting plan, then mark it as planted to start growing.")
                                    else uiText(R.string.ui_ask_the_farm_manager_to_start_a_crop_in_one_of_your__71873bf, "Ask the farm manager to start a crop in one of your plots.")
                                "planned" -> if (canManage)
                                    uiText(R.string.ui_choose_plan_a_crop_to_schedule_your_first_planting_4d4ec43, "Choose Plan a crop to schedule your first planting.")
                                    else uiText(R.string.ui_upcoming_planting_plans_will_appear_here_ecccab1, "Upcoming planting plans will appear here.")
                                else -> uiText(R.string.ui_previous_crop_cycles_will_appear_once_they_are_close_a4b9b24, "Previous crop cycles will appear once they are closed.")
                            })
                        }
                        if (selectedView == "growing" && planned.isNotEmpty()) {
                            OutlinedButton(onClick = { selectedView = "planned" },
                                modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_view_planting_plans_8c0d527, "View planting plans")) }
                        }
                        if (selectedView != "history" && canManage) {
                            OutlinedButton(onClick = onPlots, modifier = Modifier.fillMaxWidth()) {
                                Text(uiText(R.string.ui_view_farm_plots_becce47, "View farm plots"))
                            }
                        }
                    }
                }
            }
        }

        if (browse) {
            items(displayed.take(displayedLimit), key = { "crop-${it.id}" }) { cycle ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(onClick = {
                        selectedCycleId = if (selectedCycleId == cycle.id) null else cycle.id
                    }, modifier = Modifier.fillMaxWidth().testTag("crop-summary-${cycle.id}")) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FlowRow(Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(cycle.cropTypeName, style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f).widthIn(min = 140.dp))
                                    FarmStatusChip(when (cycle.status) {
                                        1 -> uiText(R.string.ui_growing_52c8c11, "Growing"); else -> cycle.statusName
                                    }, if (cycle.status == 1) FarmStatusTone.Active
                                        else if (cycle.status == 0) FarmStatusTone.Planned
                                        else FarmStatusTone.Neutral)
                                }
                                Text(cycle.plotName, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                cycle.expectedHarvestDate?.let {
                                    Text(uiText(R.string.ui_expected_harvest_1_s_d9f0c2c, "Expected harvest: %1\$s", (it)), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(if (selectedCycleId == cycle.id) "−" else "›",
                                style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    if (selectedCycleId == cycle.id) {
                        TextButton(onClick = {
                            val dismiss: () -> Unit = {
                                exitGuard?.clear("cycle-${cycle.id}")
                                selectedCycleId = null
                            }
                            if (exitGuard != null) exitGuard.requestForm("cycle-${cycle.id}", dismiss)
                            else dismiss()
                        }) { Text(uiText(R.string.ui_close_crop_details_84955ca, "Close crop details")) }
                        formState.SaveableStateProvider("cycle-${cycle.id}") {
                            CycleCard(cycle, canManage && !state.offline, state.busy,
                                onUpdatePlan, onTransition, onMonitoring, onHarvests, onQuickMonitoring)
                        }
                    }
                }
            }
            if (displayed.size > displayedLimit) item(key = "crops-more") {
                OutlinedButton(onClick = { displayedLimit += 20 },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text(uiText(R.string.ui_show_20_more_crop_records_1_s_remaining_39bd9b5, "Show 20 more crop records (%1\$s remaining)", (displayed.size - displayedLimit)))
                }
            }
        }

        if (selectedView == "plan") {
            item(key = "planting") {
                FarmSectionCard(uiText(R.string.ui_plan_a_crop_b56f05c, "Plan a crop"),
                    subtitle = uiText(R.string.ui_choose_a_plot_a_crop_and_the_planting_date_2bed098, "Choose a plot, a crop and the planting date.")) {
                    if (state.offline) {
                        FarmEmptyState(uiText(R.string.ui_connection_required_081db50, "Connection required"),
                            uiText(R.string.ui_planting_plans_must_be_confirmed_by_the_farm_server_8a24e1e, "Planting plans must be confirmed by the farm server."))
                    } else {
                        if (state.plots.isEmpty()) {
                            FarmEmptyState(uiText(R.string.ui_add_a_plot_first_c8558db, "Add a plot first"),
                                uiText(R.string.ui_choose_a_growing_area_before_making_a_planting_plan_85abc6e, "Choose a growing area before making a planting plan."))
                            OutlinedButton(onClick = onPlots,
                                modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_open_plots_71959df, "Open plots")) }
                        } else {
                            Text(uiText(R.string.ui_growing_area_9c354c5, "Growing area"), style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { choosePlot = true },
                                enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                Text(state.plots.firstOrNull { it.id == selectedPlot }?.name ?: uiText(R.string.ui_choose_plot_dd5f814, "Choose plot"))
                            }
                            DropdownMenu(expanded = choosePlot, onDismissRequest = { choosePlot = false }) {
                                state.plots.forEach { plot ->
                                    DropdownMenuItem(text = { Text(plot.name) }, onClick = {
                                        selectedPlot = plot.id; choosePlot = false
                                    })
                                }
                            }
                        }
                        if (state.types.isEmpty()) {
                            FarmEmptyState(uiText(R.string.ui_add_a_crop_type_first_2376310, "Add a crop type first"),
                                uiText(R.string.ui_open_crop_types_to_add_a_crop_before_planning_0174a08, "Open Crop types to add a crop before planning."))
                            OutlinedButton(onClick = { selectedView = "types" },
                                modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_open_crop_types_7c32f7a, "Open crop types")) }
                        } else {
                            Text(uiText(R.string.ui_crop_98caf7c, "Crop"), style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { chooseCrop = true },
                                enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                Text(state.types.firstOrNull { it.id == selectedType }?.name ?: uiText(R.string.ui_choose_crop_23ef550, "Choose crop"))
                            }
                            DropdownMenu(expanded = chooseCrop, onDismissRequest = { chooseCrop = false }) {
                                state.types.forEach { type ->
                                    DropdownMenuItem(text = { Text(type.name) }, onClick = {
                                        selectedType = type.id; chooseCrop = false
                                    })
                                }
                            }
                        }
                        if (state.plots.isNotEmpty() && state.types.isNotEmpty()) {
                            DateInput(plannedDate, uiText(R.string.ui_planting_date_b4f103e, "Planting date"), { plannedDate = it },
                                enabled = !state.busy)
                            DateInput(harvestDate, uiText(R.string.ui_expected_harvest_optional_627cd66, "Expected harvest (optional)"), { harvestDate = it },
                                enabled = !state.busy)
                            val valid = runCatching {
                                val planting = java.time.LocalDate.parse(plannedDate)
                                harvestDate.isBlank() ||
                                    !java.time.LocalDate.parse(harvestDate).isBefore(planting)
                            }.getOrDefault(false)
                            if (!valid && (plannedDate.isNotBlank() || harvestDate.isNotBlank())) {
                                val plantingValid = runCatching {
                                    java.time.LocalDate.parse(plannedDate)
                                }.isSuccess
                                Text(if (!plantingValid) uiText(R.string.ui_enter_a_valid_planting_date_3698e76, "Enter a valid planting date.")
                                     else uiText(R.string.ui_expected_harvest_cannot_be_before_planting_date_dcd5e7d, "Expected harvest cannot be before planting date."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = { onPlan(selectedPlot, selectedType, plannedDate, harvestDate) },
                                enabled = !state.busy && selectedPlot.isNotBlank() &&
                                    selectedType.isNotBlank() && valid,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text(uiText(R.string.ui_save_planting_plan_51733fa, "Save planting plan"))
                            }
                        }
                    }
                }
            }
        }

        if (selectedView == "types") {
            item(key = "types-heading") {
                FarmSectionHeader(uiText(R.string.ui_crop_types_2483e61, "Crop types"),
                    uiText(R.string.ui_available_crops_and_their_default_harvest_units_7cb716d, "Available crops and their default harvest units."))
            }
            if (state.types.isEmpty() && state.error == null) {
                item(key = "types-empty") {
                    FarmEmptyState(uiText(R.string.ui_no_crop_types_yet_c44fb3c, "No crop types yet"),
                        uiText(R.string.ui_add_a_crop_type_before_you_plan_the_first_planting_ce6c404, "Add a crop type before you plan the first planting."))
                }
            }
            items(state.types, key = { "type-${it.id}" }) { type ->
                FarmSectionCard(type.name, subtitle = uiText(R.string.ui_harvest_unit_1_s_18da5b5, "Harvest unit: %1\$s", (type.defaultHarvestUnit))) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        type.defaultGrowthDays?.let { Text(uiText(R.string.ui_typical_growth_1_s_days_d2317f3, "Typical growth: %1\$s days", (it))) }
                    }
                }
            }
            if (canManage && !state.offline) item(key = "new-type") {
                FarmSectionCard(uiText(R.string.ui_add_a_crop_type_59049fd, "Add a crop type")) {
                    TextButton(onClick = { addingType = !addingType }) {
                        Text(if (addingType) uiText(R.string.ui_close_form_00d7d53, "Close form") else uiText(R.string.ui_add_another_crop_f293c1f, "Add another crop"))
                    }
                    if (addingType) {
                        OutlinedTextField(typeName, { typeName = it },
                            label = { Text(uiText(R.string.ui_crop_name_fcdb6d3, "Crop name")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(unit, { unit = it },
                            label = { Text(uiText(R.string.ui_harvest_unit_e_g_kg_a7d4100, "Harvest unit (e.g., kg)")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(growth, { growth = it },
                            label = { Text(uiText(R.string.ui_growth_days_optional_b35c30d, "Growth days (optional)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(watering, { watering = it },
                            label = { Text(uiText(R.string.ui_watering_interval_optional_9ab46cb, "Watering interval (optional)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        val growthValid = growth.isBlank() || growth.toIntOrNull()?.let { it > 0 } == true
                        val wateringValid = watering.isBlank() ||
                            watering.toIntOrNull()?.let { it > 0 } == true
                        Button(onClick = { onCreateType(typeName, unit, growth, watering) },
                            enabled = !state.busy && typeName.isNotBlank() &&
                                unit.isNotBlank() && growthValid && wateringValid,
                            modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_add_crop_type_aa40b99, "Add crop type")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleCard(
    cycle: CropCycleDto,
    canManage: Boolean,
    busy: Boolean,
    onUpdatePlan: (CropCycleDto, String, String) -> Unit,
    onTransition: (CropCycleDto, Int, String, String) -> Unit,
    onMonitoring: (CropCycleDto) -> Unit,
    onHarvests: (CropCycleDto) -> Unit,
    onQuickMonitoring: (CropCycleDto, String) -> Unit
) {
    var editDate by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf(cycle.plannedPlantingDate) }
    var editHarvest by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf(cycle.expectedHarvestDate.orEmpty()) }
    var plantedDate by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf("") }
    var reason by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf("") }
    var confirmation by rememberSaveable(cycle.id) { mutableStateOf<Int?>(null) }
    UnsavedChanges("cycle-${cycle.id}", editDate != cycle.plannedPlantingDate ||
        editHarvest != cycle.expectedHarvestDate.orEmpty() || plantedDate.isNotEmpty() || reason.isNotEmpty(),
        editDate, editHarvest, plantedDate, reason)
    var advanced by rememberSaveable(cycle.id) { mutableStateOf(false) }

    FarmSectionCard("${cycle.cropTypeName} · ${cycle.plotName}",
        subtitle = uiText(R.string.ui_planned_planting_1_s_64bee88, "Planned planting: %1\$s", (cycle.plannedPlantingDate))) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            FarmStatusChip(cycle.statusName, when (cycle.status) {
                0 -> FarmStatusTone.Planned
                1 -> FarmStatusTone.Active
                3 -> FarmStatusTone.Failed
                else -> FarmStatusTone.Neutral
            })
            Text(uiText(R.string.ui_plot_1e7e497, "Plot: ") + cycle.plotName, style = MaterialTheme.typography.bodyMedium)
            Text(uiText(R.string.ui_expected_harvest_957e91b, "Expected harvest: ") + (cycle.expectedHarvestDate ?: uiText(R.string.ui_not_set_4895f73, "Not set")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (cycle.status == 1) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onQuickMonitoring(cycle, "watering") },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_log_watering_f52ed64, "Log watering"))
            }
            OutlinedButton(onClick = { onQuickMonitoring(cycle, "inspection") },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_inspect_crop_a790b90, "Inspect crop"))
            }
        }
        Button(onClick = { onHarvests(cycle) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text(uiText(R.string.ui_record_harvest_f027857, "Record harvest"))
        }
        OutlinedButton(onClick = { onMonitoring(cycle) }, modifier = Modifier.fillMaxWidth()) {
            Text(uiText(R.string.ui_activities_inspections_photos_8efa15b, "Activities, inspections & photos"))
        }
    } else {
        OutlinedButton(onClick = { onMonitoring(cycle) }, modifier = Modifier.fillMaxWidth()) {
            Text(uiText(R.string.ui_view_activities_photos_6ab6228, "View activities & photos"))
        }
        OutlinedButton(onClick = { onHarvests(cycle) }, modifier = Modifier.fillMaxWidth()) {
            Text(uiText(R.string.ui_view_harvest_records_2463a84, "View harvest records"))
        }
    }
    cycle.plantedDate?.let { Text(uiText(R.string.ui_planted_1_s_b58f675, "Planted: %1\$s", (it))) }
    cycle.terminationReason?.let { Text(uiText(R.string.ui_reason_1_s_4a4671a, "Reason: %1\$s", (it))) }

    if (canManage && cycle.status == 0) {
        FarmSectionHeader(uiText(R.string.ui_start_growing_1045593, "Start growing"), uiText(R.string.ui_enter_the_actual_planting_date_fc724f4, "Enter the actual planting date."))
        DateInput(plantedDate, uiText(R.string.ui_date_planted_86fb1c1, "Date planted"), { plantedDate = it }, enabled = !busy)
        Button(onClick = { confirmation = 1 }, enabled = !busy && plantedDate.isNotBlank(),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text(uiText(R.string.ui_mark_as_planted_2859939, "Mark as planted"))
        }
    }
    if (canManage && cycle.status in 0..1) {
        TextButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) uiText(R.string.ui_close_crop_controls_883193d, "Close crop controls") else uiText(R.string.ui_more_crop_controls_1b58a1f, "More crop controls"))
        }
        if (advanced) {
            if (cycle.status == 0) {
                DateInput(editDate, uiText(R.string.ui_planned_date_37b3b31, "Planned date"), { editDate = it }, enabled = !busy)
                DateInput(editHarvest, uiText(R.string.ui_expected_harvest_optional_627cd66, "Expected harvest (optional)"),
                    { editHarvest = it }, enabled = !busy)
                val planValid = runCatching {
                    val planting = java.time.LocalDate.parse(editDate)
                    editHarvest.isBlank() ||
                        !java.time.LocalDate.parse(editHarvest).isBefore(planting)
                }.getOrDefault(false)
                if (!planValid && (editDate.isNotBlank() || editHarvest.isNotBlank())) {
                    val plantingValid = runCatching { java.time.LocalDate.parse(editDate) }.isSuccess
                    Text(if (!plantingValid) uiText(R.string.ui_enter_a_valid_planting_date_3698e76, "Enter a valid planting date.")
                         else uiText(R.string.ui_expected_harvest_cannot_be_before_planting_date_dcd5e7d, "Expected harvest cannot be before planting date."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = { onUpdatePlan(cycle, editDate, editHarvest) },
                    enabled = !busy && planValid, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_update_planting_plan_12c26b1, "Update planting plan"))
                }
            }
            if (cycle.status == 1) {
                OutlinedButton(onClick = { confirmation = 2 }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth()) { Text(uiText(R.string.ui_complete_crop_cycle_30ebf71, "Complete crop cycle")) }
            }
            OutlinedTextField(reason, { reason = it },
                label = { Text(uiText(R.string.ui_reason_required_for_cancellation_or_failure_3fdb6c8, "Reason (required for cancellation or failure)")) },
                modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { confirmation = 4 },
                enabled = !busy && reason.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(uiText(R.string.ui_cancel_crop_cycle_394ea29, "Cancel crop cycle"))
            }
            if (cycle.status == 1) {
                OutlinedButton(onClick = { confirmation = 3 },
                    enabled = !busy && reason.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_mark_crop_as_failed_c98b746, "Mark crop as failed"))
                }
            }
        }
    }
    confirmation?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(uiText(R.string.ui_confirm_status_change_c1de42e, "Confirm status change")) },
            text = { Text(uiText(R.string.ui_change_1_s_from_2_s_to_c619249, "Change %1\$s from %2\$s to ", (cycle.cropTypeName), (cycle.statusName)) +
                when (target) { 1 -> uiText(R.string.ui_active_9234069, "Active"); 2 -> uiText(R.string.ui_completed_22a970d, "Completed"); 3 -> uiText(R.string.ui_failed_031a8f0, "Failed"); else -> uiText(R.string.ui_cancelled_d353a99, "Cancelled") } + "?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    onTransition(cycle, target, plantedDate, reason)
                }) { Text(uiText(R.string.ui_confirm_eebdd24, "Confirm")) }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text(uiText(R.string.ui_back_76900f1, "Back")) } }
        )
    }
    }
}
