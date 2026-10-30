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
                quick = "watering")
        } }
        compose.onNodeWithText("Quick log: Watering").assertExists()
        compose.onNodeWithText("Record inspection / observation").assertDoesNotExist()
        compose.onNodeWithText("Save activity").performClick()
        compose.runOnIdle { assertEquals(1 to "", saved) }
        compose.onNodeWithText("Show all logging options").performClick()
        compose.onNodeWithText("Record inspection / observation").assertExists()
    }

    @Test fun inspectionShortcutShowsOnlyInspectionFirst() {
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                onBack = {}, onActivity = { _, _ -> },
                onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {},
                quick = "inspection")
        } }
        compose.onNodeWithText("Quick log: Inspection").assertExists()
        compose.onNodeWithText("Record farm activity").assertDoesNotExist()
        compose.onNodeWithText("Save observation").assertExists()
    }
}
