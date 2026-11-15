package com.smartfarm.mobile

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
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
}
