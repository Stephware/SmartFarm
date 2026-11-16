package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.smartfarm.mobile.data.remote.dto.HarvestEntryDto
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.features.harvests.HarvestScreen
import com.smartfarm.mobile.features.harvests.HarvestUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HarvestUsabilityScreenTest {
    @get:Rule val compose = createComposeRule()
    private val valid = HarvestEntryDto("entry", "cycle", BigDecimal("2"), "kg",
        "2026-11-12T00:30:00Z", "2026-11-12T00:35:00Z", "worker", "First picking",
        false, null, null, null)
    private val voided = valid.copy(id = "voided", quantity = BigDecimal("1"),
        isVoided = true, voidReason = "Duplicate entry")
    private fun overview(active: Boolean = true) = HarvestOverviewDto(
        "farm", "cycle", "Tomato", "North plot", "kg",
        if (active) 1 else 2, active, false, BigDecimal("2"), 1, 1,
        listOf(valid, voided)
    )

    @Test fun quickHarvestStartsWithEntryAndPreservesConfirmedTotals() {
        var hideKeyboard: () -> Unit = {}
        var testView: android.view.View? = null
        var saved: Pair<String, String>? = null
        compose.setContent {
            val keyboard = LocalSoftwareKeyboardController.current
            val localView = LocalView.current
            SideEffect {
                hideKeyboard = { keyboard?.hide() }
                testView = localView
            }
            SmartFarmTheme {
            HarvestScreen(HarvestUiState(farmId = "farm", cycleId = "cycle",
                overview = overview()), onBack = {},
                onRecord = { qty, note -> saved = qty to note }, onVoid = { _, _ -> },
                farmTimeZone = "Asia/Manila")
        } }
        compose.onNodeWithText("Save harvested amount").assertExists()
        compose.onNodeWithText("Harvested quantity (kg)").performScrollTo().performTextInput("1.5")
        // Hiding the IME is asynchronous. A visible button can receive a tap
        // that dismisses the keyboard instead of submitting the harvest.
        // Wait for the actual window inset to close before physical scrolling.
        compose.runOnIdle { hideKeyboard() }
        compose.waitUntil(5_000) {
            testView?.let {
                ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime())
            } == false
        }
        val saveMatcher = hasText("Save harvested amount") and hasClickAction()
        compose.onNodeWithTag("record-list").performScrollToNode(saveMatcher)
        val saveHarvest = compose.onNode(saveMatcher).performScrollTo()
        // The card itself is a tall LazyColumn item; ensure its child is
        // physically visible on smaller displays instead of clicking offscreen.
        for (attempt in 0 until 6) {
            if (saveHarvest.isDisplayed()) break
            compose.onNodeWithTag("record-list").performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        saveHarvest.assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("The harvest Save action must be delivered once", "1.5" to "", saved)
        }
        compose.onNodeWithText("2 kg").performScrollTo().assertExists()
        compose.onNodeWithText("History & totals").performScrollTo().performClick()
        compose.onAllNodesWithText("Nov 12, 2026", substring = true)[0].performScrollTo().assertExists()
        compose.onAllNodesWithText("Voided")[0].performScrollTo().performClick()
        // Harvest rows live below the large form and totals header in a LazyColumn.
        compose.onNodeWithTag("record-list").performScrollToIndex(1)
        compose.onNodeWithText("Duplicate entry", substring = true).assertExists()
    }

    @Test fun finishedCropOnlyShowsHistoryNotNewHarvestForm() {
        compose.setContent { SmartFarmTheme {
            HarvestScreen(HarvestUiState(farmId = "farm", cycleId = "cycle",
                overview = overview(false)), onBack = {}, onRecord = { _, _ -> },
                onVoid = { _, _ -> })
        } }
        compose.onNodeWithText("Save harvested amount").assertDoesNotExist()
        compose.onNodeWithText("Harvest history").assertExists()
    }
}
