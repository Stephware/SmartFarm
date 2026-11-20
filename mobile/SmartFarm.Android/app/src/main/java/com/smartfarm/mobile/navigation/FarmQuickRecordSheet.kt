package com.smartfarm.mobile.navigation

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.ui.components.FarmLineIcon
import com.smartfarm.mobile.ui.components.FarmSymbol
import com.smartfarm.mobile.ui.components.FarmSpacing
import com.smartfarm.mobile.ui.components.FarmEmptyState

/** Quick action menu. Existing crop, monitoring, harvest and stock forms do the saving. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun FarmQuickRecordSheet(
    farmId: String,
    farmName: String,
    cropState: CropUiState,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit,
    onBrowseCrops: () -> Unit,
    onSupplies: () -> Unit,
    initialAction: String? = null
) {
    val preferred = initialAction?.takeIf { it in setOf("watering", "fertilizing", "inspection", "harvest", "other") }
    var action by rememberSaveable(farmId, preferred) { mutableStateOf(preferred) }
    var otherOptions by rememberSaveable(farmId) { mutableStateOf(false) }
    val available = cropState.farmId == farmId && !cropState.busy && cropState.error == null
    val growing = if (available) activeRecordCycles(cropState.cycles) else emptyList()

    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 680.dp)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (action == null) uiText(R.string.ui_record_bfdd510, "Record") else uiText(R.string.ui_choose_a_growing_crop_13f542d, "Choose a growing crop"),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = uiText(R.string.ui_close_record_menu_c6597db, "Close record menu") }) {
                    Text(uiText(R.string.ui_close_7d9eb7a, "Close"))
                }
            }
            Text(farmName, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action == null) {
                Text(uiText(R.string.ui_what_did_you_do_on_the_farm_fe228aa, "What did you do on the farm?"),
                    style = MaterialTheme.typography.titleMedium)
                RecordOption(uiText(R.string.ui_water_7ca7dea, "Water"), uiText(R.string.ui_log_watering_for_an_active_crop_0d8d5d3, "Log watering for an active crop")) { action = "watering" }
                RecordOption(uiText(R.string.ui_use_supplies_3304864, "Use Supplies"), uiText(R.string.ui_apply_fertilizer_with_optional_supply_use_867d8c2, "Apply fertilizer with optional supply use")) {
                    action = "fertilizing"
                }
                RecordOption(uiText(R.string.ui_inspect_e0723a8, "Inspect"), uiText(R.string.ui_note_crop_condition_and_add_a_photo_557460b, "Note crop condition and add a photo")) {
                    action = "inspection"
                }
                RecordOption(uiText(R.string.ui_harvest_c91ab8b, "Harvest"), uiText(R.string.ui_save_an_actual_harvest_quantity_4b70cd6, "Save an actual harvest quantity")) {
                    action = "harvest"
                }
                HorizontalDivider()
                TextButton(onClick = { otherOptions = !otherOptions }) {
                    Text(if (otherOptions) uiText(R.string.ui_hide_other_activities_c754e1d, "Hide other activities") else uiText(R.string.ui_other_f97e9da, "Other"))
                }
                if (otherOptions) {
                    RecordOption(uiText(R.string.ui_other_fieldwork_f4c7adc, "Other fieldwork"), uiText(R.string.ui_weeding_pesticide_application_or_other_work_49f49bb, "Weeding, pesticide application or other work")) { action = "other" }
                    RecordOption(uiText(R.string.ui_general_supply_use_43339ab, "General supply use"), uiText(R.string.ui_use_a_supply_outside_fertilizer_application_423e4c8, "Use a supply outside fertilizer application"), onSupplies)
                }
            } else {
                TextButton(onClick = { action = null }) { Text(uiText(R.string.ui_back_to_record_options_0ab4774, "← Back to record options")) }
                Text(uiText(R.string.ui_only_active_crop_cycles_can_receive_new_records_e721508, "Only active crop cycles can receive new records."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    cropState.farmId != farmId || cropState.busy -> {
                        CircularProgressIndicator()
                        Text(uiText(R.string.ui_loading_growing_crops_f614806, "Loading growing crops…"))
                    }
                    cropState.error != null -> {
                        FarmEmptyState(uiText(R.string.ui_crops_unavailable_677e7d0, "Crops unavailable"),
                            uiText(R.string.ui_reconnect_or_open_crops_to_refresh_the_farm_s_curren_a3a7c97, "Reconnect or open Crops to refresh the farm's current records."))
                        OutlinedButton(onClick = onBrowseCrops, modifier = Modifier.fillMaxWidth()) {
                            Text(uiText(R.string.ui_open_crops_d8163ec, "Open crops"))
                        }
                    }
                    growing.isEmpty() -> {
                        FarmEmptyState(uiText(R.string.ui_no_active_crops_ae6a7bc, "No active crops"),
                            uiText(R.string.ui_start_a_planting_cycle_before_recording_activities_i_d77e9cd, "Start a planting cycle before recording activities, inspections or harvests."))
                        Button(onClick = onBrowseCrops, modifier = Modifier.fillMaxWidth()) {
                            Text(uiText(R.string.ui_open_crops_d8163ec, "Open crops"))
                        }
                    }
                    else -> growing.forEach { cycle ->
                        OutlinedButton(onClick = { onSelect(requireNotNull(action), cycle.id) },
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(cycle.cropTypeName, style = MaterialTheme.typography.titleMedium)
                                    Text(cycle.plotName, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FarmLineIcon(FarmSymbol.ChevronRight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordOption(label: String, explanation: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()
        .semantics { role = Role.Button },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = FarmSpacing.medium, vertical = FarmSpacing.compact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(explanation, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FarmLineIcon(FarmSymbol.ChevronRight)
        }
    }
}
