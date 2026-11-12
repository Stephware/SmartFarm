package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.data.remote.dto.PlotDto
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.features.farms.FarmDetailScreen
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FarmOnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun farmPickerKeepsOnlyTwoPrimaryActionsAndHidesOptionalLocation() {
        compose.setContent { SmartFarmTheme {
            FarmListScreen(FarmUiState(), onOpen = {}, onCreate = { _, _, _, _, _ -> },
                onRequest = {}, onBack = {})
        } }
        compose.onNodeWithText("Add a farm").assertExists()
        compose.onNodeWithText("Join a farm").assertExists()
        compose.onNodeWithText("Farm name").assertDoesNotExist()
        compose.onNodeWithText("Add a farm").performClick()
        compose.onNodeWithText("Farm name").assertExists()
        compose.onNodeWithText("Location pin & time zone (optional)").assertExists()
        compose.onNodeWithText("Choose time zone").assertDoesNotExist()
        compose.onNodeWithText("Location pin & time zone (optional)").performClick()
        compose.onNodeWithText("Choose time zone").assertExists()
    }

    @Test fun newOwnerGetsDirectAddPlotShortcut() {
        var plotOpens = 0
        compose.setContent { SmartFarmTheme {
            ownerScreen(FarmUiState(selected = farm()), onPlots = { plotOpens++ })
        } }
        compose.onNodeWithText("Get your farm ready").performScrollTo().assertExists()
        compose.onNodeWithText("Add first plot").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, plotOpens) }
    }

    @Test fun existingPlotLeadsToCropPlanningWithoutInventingCropState() {
        var cropsOpened = 0
        val selected = farm()
        compose.setContent { SmartFarmTheme {
            ownerScreen(FarmUiState(selected = selected,
                plots = listOf(PlotDto("plot", "farm", "North", BigDecimal("100"), "version"))),
                crops = CropUiState(farmId = "farm"), onCrops = { cropsOpened++ })
        } }
        compose.onNodeWithText("Plan a crop").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, cropsOpened) }
    }

    @androidx.compose.runtime.Composable
    private fun ownerScreen(state: FarmUiState, crops: CropUiState? = null,
        onCrops: () -> Unit = {}, onPlots: () -> Unit = {}) {
        FarmDetailScreen(state = state, onBack = {},
            onSaveFarm = { _, _, _, _, _ -> }, onArchiveFarm = {},
            onAddPlot = { _, _ -> }, onSavePlot = { _, _, _ -> }, onArchivePlot = {},
            onCrops = onCrops, onInventory = {}, onDecision = { _, _ -> },
            onChangeRole = { _, _ -> }, onRemoveMember = {},
            homeCrops = crops, onPlots = onPlots)
    }

    private fun farm() = FarmDto("farm", "North Farm", "Angeles", "Asia/Manila", role = "Owner")
}
