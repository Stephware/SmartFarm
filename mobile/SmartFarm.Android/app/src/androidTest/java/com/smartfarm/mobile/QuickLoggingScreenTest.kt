package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.monitoring.MonitoringUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuickLoggingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun wateringShortcutKeepsTheSameSaveHandler() {
        var saved: Pair<Int, String>? = null
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                onBack = {}, onActivity = { type, notes -> saved = type to notes },
                onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {},
                onThumbnail = { byteArrayOf() }, onDismissPreview = {},
                quick = "watering")
        } }
        compose.onNodeWithText("Activity").assertIsSelected()
        compose.onNodeWithText("Watering").assertIsDisplayed()
        compose.onNodeWithText("Record inspection / observation").assertDoesNotExist()
        compose.onNodeWithText("Save activity").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1 to "", saved) }
        compose.onNodeWithText("Inspection").performScrollTo().performClick()
        compose.onNodeWithText("Save inspection").assertExists()
    }

    @Test fun inspectionShortcutShowsOnlyInspectionFirst() {
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                onBack = {}, onActivity = { _, _ -> },
                onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {},
                onThumbnail = { byteArrayOf() }, onDismissPreview = {},
                quick = "inspection")
        } }
        compose.onNodeWithText("Inspection").assertIsSelected()
        compose.onNodeWithText("Record farm activity").assertDoesNotExist()
        compose.onNodeWithText("Save inspection").assertExists()
    }
}
