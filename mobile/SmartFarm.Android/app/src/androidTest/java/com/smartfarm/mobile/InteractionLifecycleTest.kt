package com.smartfarm.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.dto.HarvestEntryDto
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.features.harvests.HarvestScreen
import com.smartfarm.mobile.features.harvests.HarvestUiState
import com.smartfarm.mobile.ui.components.ConfirmActionButton
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InteractionLifecycleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmationStaysOpenUntilResultAndCannotSubmitTwice() {
        var calls = 0
        compose.setContent { SmartFarmTheme {
            ConfirmActionButton("Remove member", "Remove Alex?") { calls++ }
        } }
        compose.onNodeWithText("Remove member").performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Remove member?").assertIsDisplayed()
        compose.onNodeWithText("Confirm").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun voidFailureKeepsDialogAndReasonForRecovery() {
        val entry = HarvestEntryDto("entry", "cycle", BigDecimal.ONE, "kg",
            "2026-11-20T00:00:00Z", "2026-11-20T00:00:00Z", "worker", null,
            false, null, null, null)
        val overview = HarvestOverviewDto("farm", "cycle", "Tomato", "North plot", "kg",
            1, true, true, BigDecimal.ONE, 1, 0, listOf(entry))
        val state = mutableStateOf(HarvestUiState(farmId = "farm", cycleId = "cycle", overview = overview))
        var calls = 0
        compose.setContent { SmartFarmTheme {
            HarvestScreen(state.value, onBack = {}, onRecord = { _, _ -> }, onVoid = { _, reason ->
                assertEquals("Duplicate entry", reason)
                calls++
                state.value = state.value.copy(busy = true)
            })
        } }
        compose.onNodeWithText("History & totals").performScrollTo().performClick()
        compose.onNodeWithText("Review voiding this harvest").performScrollTo().performClick()
        compose.onNodeWithText("Reason (required)").performTextInput("Duplicate entry")
        compose.onNodeWithText("Void harvest").performClick()
        compose.onNodeWithText("Void harvest record?").assertIsDisplayed()
        compose.onNodeWithText("Void harvest").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(busy = false, error = "Could not confirm the save. Refresh harvest history before trying again.") }
        compose.onNodeWithText("Void harvest record?").assertIsDisplayed()
        compose.onNodeWithText("Reason (required)").assertTextContains("Duplicate entry")
        compose.onNodeWithText("Could not confirm the save. Refresh harvest history before trying again.").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
