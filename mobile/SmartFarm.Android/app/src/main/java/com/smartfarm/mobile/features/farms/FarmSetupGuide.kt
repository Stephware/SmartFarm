package com.smartfarm.mobile.features.farms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.onboarding.AniGoCarabao
import com.smartfarm.mobile.ui.components.onboarding.AniGoCarabaoPose

internal enum class FarmSetupNext { AddPlot, PlanOrStartCrop, StartCrop, Record, Complete, Loading }
internal fun nextFarmSetupStep(hasPlots: Boolean, cropsLoaded: Boolean, hasActiveCrop: Boolean,
    hasPlannedCrop: Boolean = false): FarmSetupNext = when {
    !hasPlots -> FarmSetupNext.AddPlot
    !cropsLoaded -> FarmSetupNext.Loading
    hasActiveCrop -> FarmSetupNext.Record
    hasPlannedCrop -> FarmSetupNext.StartCrop
    else -> FarmSetupNext.PlanOrStartCrop
}

/** Presentation only; actions open existing authorized screens. No farm records are changed. */
@Composable
internal fun FarmSetupGuide(farmId: String, hasPlots: Boolean, cropState: CropUiState?,
    onPlots: () -> Unit, onCrops: () -> Unit, actorId: String?, onRecord: () -> Unit) {
    if (actorId == null) return
    val loaded = cropState?.farmId == farmId && !cropState.busy && !cropState.offline && cropState.error == null
    val next = nextFarmSetupStep(hasPlots, loaded,
        loaded && cropState?.cycles?.any { it.status == 1 } == true,
        loaded && cropState?.cycles?.any { it.status == 0 } == true)
    if (next == FarmSetupNext.Loading) return
    val prefs = LocalContext.current.getSharedPreferences("ani_guide_v1", android.content.Context.MODE_PRIVATE)
    val key = "$actorId:$farmId:$next"
    var showSteps by remember(key) { mutableStateOf(false) }
    var hidden by remember(key) { mutableStateOf(prefs.getBoolean(key, false)) }
    fun dismiss() { prefs.edit().putBoolean(key, true).apply(); hidden = true }
    if (hidden) {
        TextButton(onClick = { hidden = false }) { Text("Help from Ani") }
        return
    }
    val title = when (next) {
        FarmSetupNext.AddPlot -> "Add your first plot"
        FarmSetupNext.StartCrop -> "Start your planned crop"
        FarmSetupNext.Record -> "Record your fieldwork"
        else -> "Plan a crop"
    }
    val message = when (next) {
        FarmSetupNext.AddPlot -> "Give your growing area a name and size before planning a crop."
        FarmSetupNext.StartCrop -> "When you plant, open the plan and start its growing cycle."
        FarmSetupNext.Record -> "Record watering, inspections, photos and partial harvests from Record."
        else -> "Choose what to grow and when to plant in your plot."
    }
    FarmSectionCard(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AniGoCarabao(if (next == FarmSetupNext.Record) AniGoCarabaoPose.RECORD else AniGoCarabaoPose.GUIDE,
                Modifier.size(80.dp))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = {
            dismiss()
            when (next) { FarmSetupNext.AddPlot -> onPlots(); FarmSetupNext.Record -> onRecord(); else -> onCrops() }
        }, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text(title) }
        TextButton(onClick = { showSteps = !showSteps }) { Text(if (showSteps) "Hide setup steps" else "See all setup steps") }
        if (showSteps) {
            Text("1. Add a growing plot")
            Text("2. Plan and start your first crop")
            Text("3. Record fieldwork from the Record button")
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { dismiss() }) { Text("Not now") }
            TextButton(onClick = { dismiss() }) { Text("Dismiss this tip") }
        }
    }
}
