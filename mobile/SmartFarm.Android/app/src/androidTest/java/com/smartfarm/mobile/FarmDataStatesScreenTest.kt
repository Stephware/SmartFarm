package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import com.smartfarm.mobile.features.inventory.*
import com.smartfarm.mobile.features.crops.*
import com.smartfarm.mobile.features.tasks.*
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.dto.*
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmErrorState
import com.smartfarm.mobile.ui.components.FarmLoadingState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Phase 4: loading, failed, and first-use views must have distinct semantics. */
class FarmDataStatesScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingDoesNotShowAnEmptyFarm() {
        compose.setContent { SmartFarmTheme {
            FarmLoadingState("Loading crop records…")
        } }
        compose.onNodeWithText("Loading crop records…").assertExists()
        compose.onNodeWithText("Nothing growing yet").assertDoesNotExist()
    }

    @Test fun errorProvidesAWorkingRetryInsteadOfAFalseZero() {
        var retries = 0
        compose.setContent { SmartFarmTheme {
            FarmErrorState("Couldn't load supplies", onRetry = { retries++ })
        } }
        compose.onNodeWithText("Couldn't load supplies").assertExists()
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun aRealNewFarmHasCompactGuidanceWithoutInventedTotals() {
        compose.setContent { SmartFarmTheme {
            FarmEmptyState("No inventory items yet.",
                "Add your first supply to track farm-wide stock.")
        } }
        compose.onNodeWithText("No inventory items yet.").assertExists()
        compose.onNodeWithText("Add your first supply to track farm-wide stock.").assertExists()
        compose.onNodeWithText("0 kg").assertDoesNotExist()
    }
    @Test fun cachedEmptySuppliesRemainKnownSavedData() {
        compose.setContent { SmartFarmTheme {
            InventoryListScreen(InventoryUiState(farmId = "farm", offline = true, cachedAtUtcMillis = 1L),
                false, {}, {}, { _, _, _, _, _ -> })
        } }
        compose.onNodeWithText("Viewing saved offline data").assertExists()
        compose.onNodeWithText("No supplies in saved records").assertExists()
        compose.onNodeWithText("Supply records unavailable offline").assertDoesNotExist()
        compose.onNodeWithText("Add supply").assertDoesNotExist()
    }

    @Test fun cachedEmptyCropsAreNotReportedAsMissingCache() {
        compose.setContent { SmartFarmTheme {
            CropScreen(CropUiState(farmId = "farm", offline = true, cachedAtUtcMillis = 1L), false,
                {}, { _, _, _, _ -> }, { _, _, _, _ -> }, { _, _, _ -> }, {}, {}, { _, _, _, _ -> })
        } }
        compose.onNodeWithText("No growing crops in saved records").assertExists()
        compose.onNodeWithText("Crop records unavailable offline").assertDoesNotExist()
    }

    @Test fun failedTaskRefreshNeverClaimsTheFarmerIsCaughtUp() {
        val yesterday = LocalDate.now(ZoneId.of("Asia/Manila")).minusDays(1).toString()
        compose.setContent { SmartFarmTheme {
            TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila", error = "Connection unavailable",
                tasks = listOf(TaskDto("done", "farm", null, "farmer", 2, yesterday, 1, "Earlier watering", "v"))),
                false, "farmer", {}, {}, {}, {})
        } }
        compose.onNodeWithText("Connection unavailable").assertExists()
        compose.onNodeWithText("Nothing due today").assertDoesNotExist()
    }

    @Test fun notificationLoadingIsNotCalledTaskLoading() {
        compose.setContent { SmartFarmTheme {
            TaskScreen(TaskUiState(busy = true), false, "farmer", {}, {}, {}, {})
        } }
        compose.onNodeWithText("Loading notifications…").assertExists()
        compose.onNodeWithText("No notifications yet.").assertDoesNotExist()
    }

    @Test fun loadingLabelAndProgressAreOneAccessibleAnnouncement() {
        compose.setContent { SmartFarmTheme { FarmLoadingState("Loading supplies…") } }
        compose.onNodeWithText("Loading supplies…").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate))
    }

    @Test fun suppliesMoveFromLoadingToErrorEmptyAndPopulatedWithoutFalseCounts() {
        val state = mutableStateOf(InventoryUiState(farmId = "farm", busy = true))
        var retries = 0
        compose.setContent { SmartFarmTheme {
            InventoryListScreen(state.value, false, {}, {}, { _, _, _, _, _ -> }, onRetry = { retries++ })
        } }
        compose.onNodeWithText("Stock overview").assertDoesNotExist()
        compose.onNodeWithText("No inventory items yet.").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(busy = false, error = "Could not load supplies") }
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(1, retries); state.value = state.value.copy(error = null) }
        compose.onNodeWithText("Your owner or manager can add supplies for this farm.").assertExists()
        compose.runOnIdle { state.value = state.value.copy(workspace = InventoryWorkspaceDto(listOf(
            InventoryItemDto("seed", "farm", "Rice seed", 1, "kg", null, java.math.BigDecimal.TEN,
                null, false, null, "v")), 0)) }
        compose.onNodeWithText("Stock overview").assertExists()
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Rice seed"))
        compose.onNodeWithText("Rice seed").assertIsDisplayed()
        compose.onNodeWithText("No inventory items yet.").assertDoesNotExist()
        compose.onNodeWithText("Add supply").assertDoesNotExist()
    }

    @Test fun longSupplyNameAndFullBalanceRemainReadableAtLargeText() {
        val name = "Rice seed for the northern community growing areas"
        val amount = "999999999999999.999"
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(
                androidx.compose.ui.unit.Dp(320f), androidx.compose.ui.unit.Dp(720f)))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme { InventoryListScreen(InventoryUiState(farmId = "farm",
                        workspace = InventoryWorkspaceDto(listOf(InventoryItemDto("supply", "farm", name, 1,
                            "sacks (50 kg)", null, java.math.BigDecimal(amount), null, false, null, "v")), 0)),
                        false, {}, {}, { _, _, _, _, _ -> }) }
                }
            }
        }
        compose.onNodeWithTag("record-list").performScrollToIndex(1)
        val titleLayouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(name, useUnmergedTree = true).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
        org.junit.Assert.assertTrue("Supply name should have usable width", titleLayouts.single().lineCount <= 4)
        val balanceLayouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(amount, useUnmergedTree = true).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(balanceLayouts) }
        org.junit.Assert.assertEquals("Keep every balance digit on one readable line", 1, balanceLayouts.single().lineCount)
        val balance = balanceLayouts.single()
        org.junit.Assert.assertFalse("Balance overflow: size=${balance.size}, constraints=${balance.layoutInput.constraints}, " +
            "width=${balance.didOverflowWidth}, height=${balance.didOverflowHeight}", balance.hasVisualOverflow)
    }

}
