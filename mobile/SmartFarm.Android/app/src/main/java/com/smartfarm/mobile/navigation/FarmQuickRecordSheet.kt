package com.smartfarm.mobile.navigation

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
                Text(if (action == null) "Record" else "Choose a growing crop",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss,
                    modifier = Modifier.semantics { contentDescription = "Close record menu" }) {
                    Text("Close")
                }
            }
            Text(farmName, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action == null) {
                Text("What did you do on the farm?",
                    style = MaterialTheme.typography.titleMedium)
                RecordOption("Water", "Log watering for an active crop") { action = "watering" }
                RecordOption("Use Supplies", "Apply fertilizer with optional supply use") {
                    action = "fertilizing"
                }
                RecordOption("Inspect", "Note crop condition and add a photo") {
                    action = "inspection"
                }
                RecordOption("Harvest", "Save an actual harvest quantity") {
                    action = "harvest"
                }
                HorizontalDivider()
                TextButton(onClick = { otherOptions = !otherOptions }) {
                    Text(if (otherOptions) "Hide other activities" else "Other")
                }
                if (otherOptions) {
                    RecordOption("Other fieldwork", "Weeding, pesticide application or other work") { action = "other" }
                    RecordOption("General supply use", "Use a supply outside fertilizer application", onSupplies)
                }
            } else {
                TextButton(onClick = { action = null }) { Text("← Back to record options") }
                Text("Only active crop cycles can receive new records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    cropState.farmId != farmId || cropState.busy -> {
                        CircularProgressIndicator()
                        Text("Loading growing crops…")
                    }
                    cropState.error != null -> {
                        FarmEmptyState("Crops unavailable",
                            "Reconnect or open Crops to refresh the farm's current records.")
                        OutlinedButton(onClick = onBrowseCrops, modifier = Modifier.fillMaxWidth()) {
                            Text("Open crops")
                        }
                    }
                    growing.isEmpty() -> {
                        FarmEmptyState("No active crops",
                            "Start a planting cycle before recording activities, inspections or harvests.")
                        Button(onClick = onBrowseCrops, modifier = Modifier.fillMaxWidth()) {
                            Text("Open crops")
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
