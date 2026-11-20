package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
        TextButton(onClick = { hidden = false }) { Text(uiText(R.string.ui_help_from_ani_0a96868, "Help from Ani")) }
        return
    }
    val title = when (next) {
        FarmSetupNext.AddPlot -> uiText(R.string.ui_add_your_first_plot_5e0c31f, "Add your first plot")
        FarmSetupNext.StartCrop -> uiText(R.string.ui_start_your_planned_crop_75ee7bb, "Start your planned crop")
        FarmSetupNext.Record -> uiText(R.string.ui_record_your_fieldwork_626646f, "Record your fieldwork")
        else -> uiText(R.string.ui_plan_a_crop_b56f05c, "Plan a crop")
    }
    val message = when (next) {
        FarmSetupNext.AddPlot -> uiText(R.string.ui_give_your_growing_area_a_name_and_size_before_planni_8c7495c, "Give your growing area a name and size before planning a crop.")
        FarmSetupNext.StartCrop -> uiText(R.string.ui_when_you_plant_open_the_plan_and_start_its_growing_c_5390541, "When you plant, open the plan and start its growing cycle.")
        FarmSetupNext.Record -> uiText(R.string.ui_record_watering_inspections_photos_and_partial_harve_945fa96, "Record watering, inspections, photos and partial harvests from Record.")
        else -> uiText(R.string.ui_choose_what_to_grow_and_when_to_plant_in_your_plot_3c06171, "Choose what to grow and when to plant in your plot.")
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
        TextButton(onClick = { showSteps = !showSteps }) { Text(if (showSteps) uiText(R.string.ui_hide_setup_steps_7d0378a, "Hide setup steps") else uiText(R.string.ui_see_all_setup_steps_274c620, "See all setup steps")) }
        if (showSteps) {
            Text(uiText(R.string.ui_1_add_a_growing_plot_b7f0dd1, "1. Add a growing plot"))
            Text(uiText(R.string.ui_2_plan_and_start_your_first_crop_eaddc78, "2. Plan and start your first crop"))
            Text(uiText(R.string.ui_3_record_fieldwork_from_the_record_button_88d5b2d, "3. Record fieldwork from the Record button"))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { dismiss() }) { Text(uiText(R.string.ui_not_now_a0e63d7, "Not now")) }
            TextButton(onClick = { dismiss() }) { Text(uiText(R.string.ui_dismiss_this_tip_63c99ea, "Dismiss this tip")) }
        }
    }
}
