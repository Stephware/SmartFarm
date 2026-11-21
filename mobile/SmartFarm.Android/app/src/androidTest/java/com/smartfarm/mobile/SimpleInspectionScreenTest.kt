package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.monitoring.MonitoringUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SimpleInspectionScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun issueOptionsAppearOnlyWhenCropNeedsAttention() {
        var category = -1
        var severity = -1
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(
                MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                onBack = {}, onActivity = { _, _ -> }, onObservation = { _, _, _, _ -> },
                onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {}, quick = "inspection",
                onIssueObservation = { _, _, _, issue, level, _ ->
                    category = issue; severity = level
                })
        } }
        compose.onNodeWithText("What did you notice? (optional)").assertDoesNotExist()
        compose.onNodeWithText("Growth stage").assertDoesNotExist()
        compose.onNodeWithText("Needs attention").performScrollTo().performClick()
        compose.onNodeWithText("What did you notice? (optional)").performScrollTo().assertExists()
        compose.onNodeWithText("Pest").performScrollTo().performClick()
        compose.onNodeWithText("How serious is it?").performScrollTo().assertExists()
        compose.onNodeWithText("Low").performScrollTo().performClick()
        compose.onNodeWithText("Save inspection").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, category)
            assertEquals(1, severity)
        }
    }

    @Test fun inspectionKeepsPhotoAndGrowthDetailsOptional() {
        var simpleSaved = false
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(
                MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                onBack = {}, onActivity = { _, _ -> },
                onObservation = { _, _, _, _ -> simpleSaved = true },
                onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {}, quick = "inspection")
        } }
        compose.onNodeWithText("Growth stage").assertDoesNotExist()
        compose.onNodeWithText("Add growth stage (optional)").performScrollTo().performClick()
        compose.onNodeWithText("Growth stage").performScrollTo().assertExists()
        compose.onNodeWithText("Save inspection").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, simpleSaved) }
    }
}
