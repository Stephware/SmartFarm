package com.smartfarm.mobile.features.crops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.smartfarm.mobile.ui.components.DateInput
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    onRetry: () -> Unit = {}
) {
    var typeName by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("kg") }
    var growth by rememberSaveable { mutableStateOf("") }
    var watering by rememberSaveable { mutableStateOf("") }
    var selectedPlot by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("") }
    var plannedDate by rememberSaveable { mutableStateOf("") }
    var harvestDate by rememberSaveable { mutableStateOf("") }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Crop management", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back to farm") }
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
            }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)

            Text("Available crop types", style = MaterialTheme.typography.titleLarge)
            if (state.types.isEmpty()) Text("No crop types yet.")
            state.types.forEach { type -> Text("${type.name} · ${type.defaultHarvestUnit}") }

            if (canManage && !state.offline) {
                HorizontalDivider()
                Text("Add crop type", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(typeName, { typeName = it }, label = { Text("Crop name") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(unit, { unit = it }, label = { Text("Harvest unit (e.g., kg)") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(growth, { growth = it }, label = { Text("Growth days (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(watering, { watering = it }, label = { Text("Watering interval (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Button(onClick = { onCreateType(typeName, unit, growth, watering) },
                    enabled = !state.busy && typeName.isNotBlank() && unit.isNotBlank()) {
                    Text("Add crop type")
                }

                HorizontalDivider()
                Text("Plan planting", style = MaterialTheme.typography.titleLarge)
                if (state.plots.isEmpty()) Text("Create a plot first.")
                if (state.types.isEmpty()) Text("Create a crop type first.")
                if (state.plots.isNotEmpty()) {
                    Text("Choose plot", style = MaterialTheme.typography.titleMedium)
                    state.plots.forEach { plot ->
                        FilterChip(selected = selectedPlot == plot.id, onClick = { selectedPlot = plot.id },
                            enabled = !state.busy, label = { Text(plot.name) })
                    }
                }
                if (state.types.isNotEmpty()) {
                    Text("Choose crop", style = MaterialTheme.typography.titleMedium)
                    state.types.forEach { type ->
                        FilterChip(selected = selectedType == type.id, onClick = { selectedType = type.id },
                            enabled = !state.busy, label = { Text(type.name) })
                    }
                }
                DateInput(plannedDate, "Planting date", { plannedDate = it }, enabled = !state.busy)
                DateInput(harvestDate, "Expected harvest (optional)", { harvestDate = it }, enabled = !state.busy)
                Button(onClick = { onPlan(selectedPlot, selectedType, plannedDate, harvestDate) },
                    enabled = !state.busy && selectedPlot.isNotBlank() && selectedType.isNotBlank()
                        && plannedDate.isNotBlank()) { Text("Create planting plan") }
            }
            HorizontalDivider()
            Text("Crop cycles and history", style = MaterialTheme.typography.titleLarge)
            if (state.cycles.isEmpty()) Text("No crop cycles yet.")
            state.cycles.forEach { cycle ->
                CycleCard(cycle, canManage && !state.offline, state.busy, onUpdatePlan, onTransition, onMonitoring, onHarvests)
                HorizontalDivider()
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
    onHarvests: (CropCycleDto) -> Unit
) {
    var editDate by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf(cycle.plannedPlantingDate) }
    var editHarvest by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf(cycle.expectedHarvestDate.orEmpty()) }
    var plantedDate by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf("") }
    var reason by rememberSaveable(cycle.id, cycle.rowVersion) { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf<Int?>(null) }

    Text("${cycle.cropTypeName} · ${cycle.plotName}", style = MaterialTheme.typography.titleMedium)
    Text("${cycle.statusName} · Planned: ${cycle.plannedPlantingDate}")
    OutlinedButton(onClick = { onMonitoring(cycle) }) { Text("Activities, inspections & photos") }
    OutlinedButton(onClick = { onHarvests(cycle) }) { Text("Harvest records") }
    cycle.plantedDate?.let { Text("Planted: $it") }
    cycle.expectedHarvestDate?.let { Text("Expected harvest: $it") }
    cycle.terminationReason?.let { Text("Reason: $it") }

    if (canManage && cycle.status == 0) {
        DateInput(editDate, "Planned date", { editDate = it }, enabled = !busy)
        DateInput(editHarvest, "Expected harvest (optional)", { editHarvest = it }, enabled = !busy)
        OutlinedButton(onClick = { onUpdatePlan(cycle, editDate, editHarvest) },
            enabled = !busy && editDate.isNotBlank()) { Text("Update plan") }
        DateInput(plantedDate, "Actual planting date", { plantedDate = it }, enabled = !busy)
        Button(onClick = { confirmation = 1 }, enabled = !busy && plantedDate.isNotBlank()) {
            Text("Mark as planted")
        }
    }
    if (canManage && cycle.status == 1) {
        Button(onClick = { confirmation = 2 }, enabled = !busy) { Text("Complete cycle") }
    }
    if (canManage && cycle.status in listOf(0, 1)) {
        OutlinedTextField(reason, { reason = it },
            label = { Text("Reason to cancel or mark failed") },
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { confirmation = 4 },
            enabled = !busy && reason.isNotBlank()) { Text("Cancel cycle") }
        if (cycle.status == 1) {
            OutlinedButton(onClick = { confirmation = 3 },
                enabled = !busy && reason.isNotBlank()) { Text("Mark failed") }
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


