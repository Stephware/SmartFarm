package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.runtime.mutableStateOf
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.features.farms.FarmDetailScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import com.smartfarm.mobile.ui.components.ConfirmActionButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SafeActionsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun busyStateDisablesAnOpenConfirmation() {
        val enabled = mutableStateOf(false)
        var calls = 0
        compose.setContent {
            SmartFarmTheme { ConfirmActionButton("Remove member", "Remove Alex?", enabled.value) { calls++ } }
        }
        compose.onNodeWithText("Remove member").assertIsNotEnabled().performClick()
        compose.onNodeWithText("Confirm").assertDoesNotExist()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithText("Remove member").performClick()
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun archivingRequiresConfirmationAndCancelKeepsFarm() {
        var archives = 0
        compose.setContent {
            SmartFarmTheme {
                FarmDetailScreen(
                    state = FarmUiState(selected = FarmDto("farm", "North farm", "Location", "Asia/Manila", null, null, "Owner", "version")),
                    onBack = {}, onSaveFarm = { _, _, _, _, _ -> }, onArchiveFarm = { archives++ },
                    onAddPlot = { _, _ -> }, onSavePlot = { _, _, _ -> }, onArchivePlot = {},
                    onCrops = {}, onInventory = {}, onDecision = { _, _ -> },
                    onChangeRole = { _, _ -> }, onRemoveMember = {}
                )
            }
        }
        compose.onNodeWithText("Settings").performScrollTo().performClick()
        compose.onNodeWithText("Archive farm").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, archives) }
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, archives) }
        compose.onNodeWithText("Archive farm").performScrollTo().performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals(1, archives) }
    }
}
