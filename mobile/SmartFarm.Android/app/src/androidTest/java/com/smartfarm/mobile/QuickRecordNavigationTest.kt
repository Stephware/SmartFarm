package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performScrollTo
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.navigation.FarmQuickRecordSheet
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickRecordNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val active = CropCycleDto("cycle", "farm", "plot", "North plot",
        "type", "Tomato", 1, "2026-11-01", "2026-11-01", "2027-01-01",
        "kg", 2, null, null, "version")

    @Test fun inspectionChoosesAnActiveCropAndReusesMonitoring() {
        var selected: Pair<String,String>? = null
        compose.setContent { SmartFarmTheme {
            FarmQuickRecordSheet("farm", "North farm",
                CropUiState(farmId = "farm", cycles = listOf(active)),
                onDismiss = {}, onSelect = { action, cycle -> selected = action to cycle },
                onBrowseCrops = {}, onSupplies = {})
        } }
        compose.onNodeWithText("Inspect").performClick()
        compose.onNodeWithText("Tomato").performClick()
        compose.runOnIdle { assertEquals("inspection" to "cycle", selected) }
    }

    @Test fun plannedCropsCannotBeRecordedAsHarvested() {
        var browsed = false
        compose.setContent { SmartFarmTheme {
            FarmQuickRecordSheet("farm", "North farm",
                CropUiState(farmId = "farm", cycles = listOf(active.copy(status = 0))),
                onDismiss = {}, onSelect = { _, _ -> },
                onBrowseCrops = { browsed = true }, onSupplies = {})
        } }
        compose.onNodeWithText("Harvest").performClick()
        compose.onNodeWithText("No active crops").assertExists()
        compose.onNodeWithText("Tomato").assertDoesNotExist()
        compose.onNodeWithText("Open crops").performClick()
        compose.runOnIdle { assertEquals(true, browsed) }
    }
    @Test fun otherShortcutNeverDefaultsToWatering() {
        compose.setContent { SmartFarmTheme {
            com.smartfarm.mobile.features.monitoring.MonitoringScreen(
                com.smartfarm.mobile.features.monitoring.MonitoringUiState(
                    farmId = "farm", cycleId = "cycle", canRecord = true),
                onBack = {}, onActivity = { _, _ -> }, onObservation = { _, _, _, _ -> },
                onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {}, quick = "other")
        } }
        compose.onNodeWithText("Other").performScrollTo().assertIsSelected()
    }

    @Test fun waterShortcutKeepsTheLinkedActivityTypeFixed() {
        var kind: Int? = null
        compose.setContent { SmartFarmTheme {
            com.smartfarm.mobile.features.monitoring.MonitoringScreen(
                com.smartfarm.mobile.features.monitoring.MonitoringUiState(
                    farmId = "farm", cycleId = "cycle", canRecord = true),
                onBack = {}, onActivity = { value, _ -> kind = value }, onObservation = { _, _, _, _ -> },
                onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {}, quick = "watering")
        } }
        compose.onNodeWithText("Fertilizing").assertDoesNotExist()
        compose.onNodeWithText("Other activity types").assertDoesNotExist()
        compose.onNodeWithText("Save activity").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, kind) }
    }

    @Test fun inspectionCanSaveNotSureWithoutInventingAnIssueOrPhoto() {
        var status: Int? = null
        compose.setContent { SmartFarmTheme {
            com.smartfarm.mobile.features.monitoring.MonitoringScreen(
                com.smartfarm.mobile.features.monitoring.MonitoringUiState(
                    farmId = "farm", cycleId = "cycle", canRecord = true),
                onBack = {}, onActivity = { _, _ -> }, onObservation = { health, _, _, photo ->
                    status = health; assertEquals(null, photo)
                }, onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {}, quick = "inspection")
        } }
        compose.onNodeWithText("Not sure").performScrollTo().assertIsSelected()
        compose.onNodeWithText("How serious is it?").assertDoesNotExist()
        compose.onNodeWithText("Save inspection").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, status) }
    }

}
