package com.smartfarm.mobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.data.remote.dto.PlotDto
import com.smartfarm.mobile.features.farms.FarmDetailScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.navigation.FarmWorkspaceBar
import com.smartfarm.mobile.ui.components.FarmAppBar
import com.smartfarm.mobile.ui.components.FarmHeaderNavigation
import com.smartfarm.mobile.ui.components.LocalFarmHeaderNavigation
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real Compose interaction tests on the Android emulator, without a live farm server. */
class FinalFarmerJourneysTest {
    @get:Rule val compose = createComposeRule()

    private val farm = FarmDto("farm", "Green Valley", "Angeles",
        "Asia/Manila", role = "Owner")
    private val plots = listOf(
        PlotDto("plot1", "farm", "North plot", BigDecimal("100"), "version"),
        PlotDto("plot2", "farm", "South plot", BigDecimal("200"), "version")
    )

    @Test fun smallPhoneWithLargeTextRetainsAllMainNavigationActions() {
        var route: String? = null
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme {
                        FarmWorkspaceBar("farm/{farmId}") { route = it }
                    }
                }
            }
        }
        listOf("Today", "Record", "My Farm").forEach {
            compose.onNodeWithText(it).assertExists()
        }
        compose.onNodeWithText("Record").performClick()
        compose.runOnIdle { assertEquals("record", route) }
        compose.onNodeWithText("My Farm").performClick()
        compose.runOnIdle { assertEquals("myfarm/{farmId}", route) }
    }

    @Test fun compactHeaderKeepsFarmSwitchAndOfflineWorkReachable() {
        var switches = 0
        var syncOpens = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme {
                        CompositionLocalProvider(LocalFarmHeaderNavigation provides
                            FarmHeaderNavigation("Green Valley Farm", onFarms = { switches++ },
                                onNotifications = {},
                                queuedCount = 4, reviewCount = 1, onSync = { syncOpens++ })) {
                            FarmAppBar()
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Switch farm: Green Valley Farm").performClick()
        compose.onNodeWithContentDescription(
            "1 offline records need review; 4 waiting in total").performClick()
        compose.runOnIdle {
            assertEquals(1, switches)
            assertEquals(1, syncOpens)
        }
    }

    @Test fun plotsRevealOnlyOneEditFormAndKeepCropAccess() {
        var openedCrops = 0
        compose.setContent { SmartFarmTheme {
            FarmDetailScreen(
                state = FarmUiState(selected = farm, plots = plots),
                onBack = {},
                onSaveFarm = { _, _, _, _, _ -> },
                onArchiveFarm = {},
                onAddPlot = { _, _ -> },
                onSavePlot = { _, _, _ -> },
                onArchivePlot = {},
                onCrops = { openedCrops++ },
                onInventory = {},
                onDecision = { _, _ -> },
                onChangeRole = { _, _ -> },
                onRemoveMember = {},
                section = "Plots")
        } }
        compose.onNodeWithText("Save plot changes").assertDoesNotExist()
        compose.onNodeWithTag("plot-card-plot1").performScrollTo().performClick()
        compose.onNodeWithText("Edit North plot").performScrollTo().assertExists()
        compose.onNodeWithText("View crops").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, openedCrops) }
        compose.onNodeWithTag("plot-card-plot2").performScrollTo().performClick()
        compose.onNodeWithText("Edit South plot").performScrollTo().assertExists()
        compose.onNodeWithText("Edit North plot").assertDoesNotExist()
    }

    @Test fun onlyOwnersAndManagersSeePlotEditControls() {
        compose.setContent { SmartFarmTheme {
            FarmDetailScreen(
                state = FarmUiState(
                    selected = farm.copy(role = "FieldWorker"), plots = plots),
                onBack = {},
                onSaveFarm = { _, _, _, _, _ -> },
                onArchiveFarm = {},
                onAddPlot = { _, _ -> },
                onSavePlot = { _, _, _ -> },
                onArchivePlot = {},
                onCrops = {}, onInventory = {},
                onDecision = { _, _ -> },
                onChangeRole = { _, _ -> },
                onRemoveMember = {},
                section = "Plots")
        } }
        compose.onNodeWithText("Add growing plot").assertDoesNotExist()
        compose.onNodeWithTag("plot-card-plot1").performScrollTo().performClick()
        compose.onNodeWithText("View crops").performScrollTo().assertExists()
        compose.onNodeWithText("Save plot changes").assertDoesNotExist()
    }
}
