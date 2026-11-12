package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.features.crops.CropScreen
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Rule
import org.junit.Test

class CropWorkflowScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun cycle(id: String, status: Int) = CropCycleDto(
        id, "farm", "plot", "North plot", "crop-type", "Tomato",
        status, "2026-11-01",
        if (status == 0) null else "2026-11-02",
        "2027-01-01", "kg", 2, null, null, "version"
    )

    @Test fun growingPlannedAndHistoryAreSeparate() {
        compose.setContent { SmartFarmTheme {
            Crops(CropUiState(farmId = "farm", cycles = listOf(
                cycle("growing", 1), cycle("planned", 0), cycle("previous", 2))))
        } }
        compose.onNodeWithText("Growing now").assertExists()
        compose.onNodeWithTag("crop-summary-growing").assertExists()
        compose.onNodeWithTag("crop-summary-planned").assertDoesNotExist()
        compose.onNodeWithText("Planned").performClick()
        compose.onNodeWithText("Ready for planting").assertExists()
        compose.onNodeWithTag("crop-summary-planned").assertExists()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("Past crops").assertExists()
        compose.onNodeWithTag("crop-summary-previous").assertExists()
    }

    @Test fun cropControlsAreSecondaryButFieldworkIsDirect() {
        var shortcut: String? = null
        compose.setContent { SmartFarmTheme {
            Crops(CropUiState(farmId = "farm", cycles = listOf(cycle("growing", 1))),
                canManage = true, onShortcut = { shortcut = it })
        } }
        compose.onNodeWithTag("crop-summary-growing").performClick()
        compose.onNodeWithText("Log watering").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals("watering", shortcut) }
        compose.onNodeWithText("Complete crop cycle").assertDoesNotExist()
        compose.onNodeWithText("More crop controls").performScrollTo().performClick()
        compose.onNodeWithText("Complete crop cycle").performScrollTo().assertExists()
    }

    @Test fun newFarmOffersDirectPlotSetupBeforePlanting() {
        var openedPlots = false
        compose.setContent { SmartFarmTheme {
            Crops(CropUiState(farmId = "farm"), canManage = true,
                onPlots = { openedPlots = true })
        } }
        compose.onNodeWithText("Plan a crop").performClick()
        compose.onNodeWithText("Open plots").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(true, openedPlots) }
    }

    @androidx.compose.runtime.Composable
    private fun Crops(
        state: CropUiState, canManage: Boolean = false,
        onShortcut: (String) -> Unit = {}, onPlots: () -> Unit = {}
    ) {
        CropScreen(state, canManage, {},
            { _, _, _, _ -> }, { _, _, _, _ -> }, { _, _, _ -> },
            {}, {}, { _, _, _, _ -> },
            onQuickMonitoring = { _, mode -> onShortcut(mode) },
            onPlots = onPlots)
    }
}
