package com.smartfarm.mobile.features.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.ui.components.FarmSectionCard

/** Guidance only: all mutations still use existing Plots and Crop screens. */
internal enum class FarmSetupNext { AddPlot, PlanOrStartCrop, Complete, Loading }

internal fun nextFarmSetupStep(hasPlots: Boolean, cropsLoaded: Boolean, hasActiveCrop: Boolean): FarmSetupNext =
    when {
        !hasPlots -> FarmSetupNext.AddPlot
        !cropsLoaded -> FarmSetupNext.Loading
        !hasActiveCrop -> FarmSetupNext.PlanOrStartCrop
        else -> FarmSetupNext.Complete
    }

@Composable
internal fun FarmSetupGuide(
    farmId: String,
    hasPlots: Boolean,
    cropState: CropUiState?,
    onPlots: () -> Unit,
    onCrops: () -> Unit
) {
    var hidden by rememberSaveable(farmId) { mutableStateOf(false) }
    var showSteps by rememberSaveable(farmId) { mutableStateOf(false) }
    val loaded = cropState?.farmId == farmId &&
        !cropState.busy && cropState.error == null
    val hasActiveCrop = loaded && cropState?.cycles?.any { it.status == 1 } == true
    val hasPlannedCrop = loaded && cropState?.cycles?.any { it.status == 0 } == true
    val next = nextFarmSetupStep(hasPlots, loaded, hasActiveCrop)
    if (hidden || next == FarmSetupNext.Loading || next == FarmSetupNext.Complete) return

    FarmSectionCard("Get your farm ready",
        subtitle = if (next == FarmSetupNext.AddPlot) "Step 1 of 3 · Set up a growing area"
                   else "Step 2 of 3 · Start a crop") {
        Text(if (next == FarmSetupNext.AddPlot)
            "Add your first plot to begin planning crops."
        else if (hasPlannedCrop)
            "Your planting plan is ready. Start the growing cycle when you plant."
        else "Your plot is ready. Choose what to grow next.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = if (next == FarmSetupNext.AddPlot) onPlots else onCrops,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
            Text(if (next == FarmSetupNext.AddPlot) "Add first plot"
                else if (hasPlannedCrop) "Start planned crop" else "Plan a crop")
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showSteps = !showSteps }) {
                Text(if (showSteps) "Hide setup steps" else "See all setup steps")
            }
            TextButton(onClick = { hidden = true }) { Text("Not now") }
        }
        if (showSteps) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Add a growing plot ${if (hasPlots) "✓" else "· Next step"}",
                    style = MaterialTheme.typography.bodyMedium)
                Text("2. Plan and start your first crop ${if (hasActiveCrop) "✓" else "· Not started"}",
                    style = MaterialTheme.typography.bodyMedium)
                Text("3. Record fieldwork from the Record button",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
