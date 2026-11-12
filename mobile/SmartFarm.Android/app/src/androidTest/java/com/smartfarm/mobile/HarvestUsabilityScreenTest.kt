package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
        var saved: Pair<String, String>? = null
        compose.setContent { SmartFarmTheme {
            HarvestScreen(HarvestUiState(farmId = "farm", cycleId = "cycle",
                overview = overview()), onBack = {},
                onRecord = { qty, note -> saved = qty to note }, onVoid = { _, _ -> },
                farmTimeZone = "Asia/Manila")
        } }
        compose.onNodeWithText("Save harvested amount").assertExists()
        compose.onNodeWithText("Harvested quantity (kg)").performScrollTo().performTextInput("1.5")
        compose.onNodeWithText("Save harvested amount").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("1.5" to "", saved) }
        compose.onNodeWithText("2 kg").performScrollTo().assertExists()
        compose.onNodeWithText("History & totals").performScrollTo().performClick()
        compose.onNodeWithText("Nov 12, 2026", substring = true).performScrollTo().assertExists()
        compose.onNodeWithText("Voided").performScrollTo().performClick()
        compose.onNodeWithText("Duplicate entry").performScrollTo().assertExists()
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
