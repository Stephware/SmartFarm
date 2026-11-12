package com.smartfarm.mobile.features.crops

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
import com.smartfarm.mobile.ui.components.CropArtworkImage
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
import androidx.compose.material3.TextButton
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
    RecordPage {
        item(key = "overview") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero("Crops", "See what's growing, plan planting, and check previous crops.")
                if (state.busy) CircularProgressIndicator()
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
                }
                OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("growing" to "Growing", "planned" to "Planned", "history" to "History")
                        .forEach { (key, label) ->
                            FilterChip(selected = selectedView == key,
                                onClick = { selectedView = key; selectedCycleId = null },
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                label = { Text(label) })
                        }
                }
                if (canManage) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            selectedView = if (selectedView == "plan") "planned" else "plan"
                            selectedCycleId = null
                        }, enabled = !state.offline && !state.busy,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                            Text(if (selectedView == "plan") "Close plan" else "Plan a crop")
                        }
                        OutlinedButton(onClick = {
                            selectedView = if (selectedView == "types") "growing" else "types"
                            selectedCycleId = null
                        }, modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                            Text("Crop types")
                        }
                    }
                }
                if (browse) {
                    val title = when (selectedView) {
                        "growing" -> "Growing now"
                        "planned" -> "Ready for planting"
                        else -> "Past crops"
                    }
                    FarmSectionHeader(title, when (selectedView) {
                        "growing" -> "Tap a crop to record fieldwork, take photos, or view harvests."
                        "planned" -> "Tap a plan to review it or mark the planting as started."
                        else -> "Completed, failed and cancelled crop cycles."
                    })
                    if (!state.busy && state.error == null && displayed.isEmpty()) {
                        FarmEmptyState(when (selectedView) {
                            "growing" -> "Nothing growing yet"
                            "planned" -> "No planting plans"
                            else -> "No crop history yet"
                        }, when (selectedView) {
                            "growing" -> if (canManage)
                                "Make a planting plan, then mark it as planted to start growing."
                                else "Ask the farm manager to start a crop in one of your plots."
                            "planned" -> if (canManage)
                                "Choose Plan a crop to schedule your first planting."
                                else "Upcoming planting plans will appear here."
                            else -> "Previous crop cycles will appear once they are closed."
                        })
                        if (selectedView == "growing" && planned.isNotEmpty()) {
                            OutlinedButton(onClick = { selectedView = "planned" },
                                modifier = Modifier.fillMaxWidth()) { Text("View planting plans") }
                        }
                        if (selectedView != "history" && canManage) {
                            OutlinedButton(onClick = onPlots, modifier = Modifier.fillMaxWidth()) {
                                Text("View farm plots")
                            }
                        }
                    }
                }
            }
        }

        if (browse) {
            items(displayed, key = { "crop-${it.id}" }) { cycle ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(onClick = {
                        selectedCycleId = if (selectedCycleId == cycle.id) null else cycle.id
                    }, modifier = Modifier.fillMaxWidth().testTag("crop-summary-${cycle.id}")) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer) {
                                CropArtworkImage(cycle.cropTypeName,
                                    Modifier.padding(6.dp).size(70.dp))
                            }
                            Column(Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(cycle.cropTypeName, style = MaterialTheme.typography.titleMedium)
                                Text(cycle.plotName, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                FarmStatusChip(when (cycle.status) {
                                    1 -> "Growing"; else -> cycle.statusName
                                }, if (cycle.status == 1) FarmStatusTone.Active
                                    else if (cycle.status == 0) FarmStatusTone.Planned
                                    else FarmStatusTone.Neutral)
                                cycle.expectedHarvestDate?.let {
                                    Text("Expected harvest: $it", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(if (selectedCycleId == cycle.id) "−" else "›",
                                style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    if (selectedCycleId == cycle.id) {
                        TextButton(onClick = { selectedCycleId = null }) { Text("Close crop details") }
                        CycleCard(cycle, canManage && !state.offline, state.busy,
                            onUpdatePlan, onTransition, onMonitoring, onHarvests, onQuickMonitoring)
                    }
                }
            }
        }

        if (selectedView == "plan") {
            item(key = "planting") {
                FarmSectionCard("Plan a crop",
                    subtitle = "Choose a plot, a crop and the planting date.") {
                    if (state.offline) {
                        FarmEmptyState("Connection required",
                            "Planting plans must be confirmed by the farm server.")
                    } else {
                        if (state.plots.isEmpty()) {
                            FarmEmptyState("Add a plot first",
                                "Choose a growing area before making a planting plan.")
                            OutlinedButton(onClick = onPlots,
                                modifier = Modifier.fillMaxWidth()) { Text("Open plots") }
                        } else {
                            Text("Growing area", style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { choosePlot = true },
                                enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                Text(state.plots.firstOrNull { it.id == selectedPlot }?.name ?: "Choose plot")
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
                            FarmEmptyState("Add a crop type first",
                                "Open Crop types to add a crop before planning.")
                            OutlinedButton(onClick = { selectedView = "types" },
                                modifier = Modifier.fillMaxWidth()) { Text("Open crop types") }
                        } else {
                            Text("Crop", style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { chooseCrop = true },
                                enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                Text(state.types.firstOrNull { it.id == selectedType }?.name ?: "Choose crop")
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
                            DateInput(plannedDate, "Planting date", { plannedDate = it },
                                enabled = !state.busy)
                            DateInput(harvestDate, "Expected harvest (optional)", { harvestDate = it },
                                enabled = !state.busy)
                            val valid = runCatching {
                                val planting = java.time.LocalDate.parse(plannedDate)
                                harvestDate.isBlank() ||
                                    !java.time.LocalDate.parse(harvestDate).isBefore(planting)
                            }.getOrDefault(false)
                            Button(onClick = { onPlan(selectedPlot, selectedType, plannedDate, harvestDate) },
                                enabled = !state.busy && selectedPlot.isNotBlank() &&
                                    selectedType.isNotBlank() && valid,
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text("Save planting plan")
                            }
                        }
                    }
                }
            }
        }

        if (selectedView == "types") {
            item(key = "types-heading") {
                FarmSectionHeader("Crop types",
                    "Available crops and their default harvest units.")
            }
            if (state.types.isEmpty() && state.error == null) {
                item(key = "types-empty") {
                    FarmEmptyState("No crop types yet",
                        "Add a crop type before you plan the first planting.")
                }
            }
            items(state.types, key = { "type-${it.id}" }) { type ->
                FarmSectionCard(type.name, subtitle = "Harvest unit: ${type.defaultHarvestUnit}") {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CropArtworkImage(type.name, Modifier.size(64.dp))
                        type.defaultGrowthDays?.let { Text("Typical growth: $it days") }
                    }
                }
            }
            if (canManage && !state.offline) item(key = "new-type") {
                FarmSectionCard("Add a crop type") {
                    TextButton(onClick = { addingType = !addingType }) {
                        Text(if (addingType) "Close form" else "Add another crop")
                    }
                    if (addingType) {
                        OutlinedTextField(typeName, { typeName = it },
                            label = { Text("Crop name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(unit, { unit = it },
                            label = { Text("Harvest unit (e.g., kg)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(growth, { growth = it },
                            label = { Text("Growth days (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(watering, { watering = it },
                            label = { Text("Watering interval (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        val growthValid = growth.isBlank() || growth.toIntOrNull()?.let { it > 0 } == true
                        val wateringValid = watering.isBlank() ||
                            watering.toIntOrNull()?.let { it > 0 } == true
                        Button(onClick = { onCreateType(typeName, unit, growth, watering) },
                            enabled = !state.busy && typeName.isNotBlank() &&
                                unit.isNotBlank() && growthValid && wateringValid,
                            modifier = Modifier.fillMaxWidth()) { Text("Add crop type") }
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
    var advanced by rememberSaveable(cycle.id) { mutableStateOf(false) }

    FarmSectionCard("${cycle.cropTypeName} · ${cycle.plotName}",
        subtitle = "Planned planting: ${cycle.plannedPlantingDate}") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CropArtworkImage(cycle.cropTypeName, Modifier.size(92.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            FarmStatusChip(cycle.statusName, when (cycle.status) {
                0 -> FarmStatusTone.Planned
                1 -> FarmStatusTone.Active
                3 -> FarmStatusTone.Failed
                else -> FarmStatusTone.Neutral
            })
            Text("Plot: " + cycle.plotName, style = MaterialTheme.typography.bodyMedium)
            Text("Expected harvest: " + (cycle.expectedHarvestDate ?: "Not set"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (cycle.status == 1) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onQuickMonitoring(cycle, "watering") },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                Text("Log watering")
            }
            OutlinedButton(onClick = { onQuickMonitoring(cycle, "inspection") },
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)) {
                Text("Inspect crop")
            }
        }
        Button(onClick = { onHarvests(cycle) },
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text("Record harvest")
        }
        OutlinedButton(onClick = { onMonitoring(cycle) }, modifier = Modifier.fillMaxWidth()) {
            Text("Activities, inspections & photos")
        }
    } else {
        OutlinedButton(onClick = { onMonitoring(cycle) }, modifier = Modifier.fillMaxWidth()) {
            Text("View activities & photos")
        }
        OutlinedButton(onClick = { onHarvests(cycle) }, modifier = Modifier.fillMaxWidth()) {
            Text("View harvest records")
        }
    }
    cycle.plantedDate?.let { Text("Planted: $it") }
    cycle.terminationReason?.let { Text("Reason: $it") }

    if (canManage && cycle.status == 0) {
        FarmSectionHeader("Start growing", "Enter the actual planting date.")
        DateInput(plantedDate, "Date planted", { plantedDate = it }, enabled = !busy)
        Button(onClick = { confirmation = 1 }, enabled = !busy && plantedDate.isNotBlank(),
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text("Mark as planted")
        }
    }
    if (canManage && cycle.status in 0..1) {
        TextButton(onClick = { advanced = !advanced }) {
            Text(if (advanced) "Close crop controls" else "More crop controls")
        }
        if (advanced) {
            if (cycle.status == 0) {
                DateInput(editDate, "Planned date", { editDate = it }, enabled = !busy)
                DateInput(editHarvest, "Expected harvest (optional)",
                    { editHarvest = it }, enabled = !busy)
                val planValid = runCatching {
                    val planting = java.time.LocalDate.parse(editDate)
                    editHarvest.isBlank() ||
                        !java.time.LocalDate.parse(editHarvest).isBefore(planting)
                }.getOrDefault(false)
                OutlinedButton(onClick = { onUpdatePlan(cycle, editDate, editHarvest) },
                    enabled = !busy && planValid, modifier = Modifier.fillMaxWidth()) {
                    Text("Update planting plan")
                }
            }
            if (cycle.status == 1) {
                OutlinedButton(onClick = { confirmation = 2 }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth()) { Text("Complete crop cycle") }
            }
            OutlinedTextField(reason, { reason = it },
                label = { Text("Reason (required for cancellation or failure)") },
                modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { confirmation = 4 },
                enabled = !busy && reason.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Cancel crop cycle")
            }
            if (cycle.status == 1) {
                OutlinedButton(onClick = { confirmation = 3 },
                    enabled = !busy && reason.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Mark crop as failed")
                }
            }
        }
    }
    confirmation?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Confirm status change") },
            text = { Text("Change ${cycle.cropTypeName} from ${cycle.statusName} to " +
                when (target) { 1 -> "Active"; 2 -> "Completed"; 3 -> "Failed"; else -> "Cancelled" } + "?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    onTransition(cycle, target, plantedDate, reason)
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Back") } }
        )
    }
    }
}
