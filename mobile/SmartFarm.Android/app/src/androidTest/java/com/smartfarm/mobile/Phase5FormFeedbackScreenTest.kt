package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Rule
import org.junit.Test

/** Phase 5: long form values should explain why Save cannot proceed. */
class Phase5FormFeedbackScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun newFarmExplainsOverlongNameWithoutSendingIt() {
        var creations = 0
        compose.setContent { SmartFarmTheme {
            FarmListScreen(FarmUiState(), onOpen = {}, onCreate = { _, _, _, _, _ -> creations++ },
                onRequest = {}, onBack = {})
        } }
        compose.onNodeWithText("Add a farm").performClick()
        compose.onNodeWithText("Farm name").performScrollTo()
            .performTextInput("Farm".repeat(31))
        compose.onNodeWithText("Farm name must be 120 characters or fewer.")
            .performScrollTo().assertExists()
        compose.onNodeWithText("Create farm").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { org.junit.Assert.assertEquals(0, creations) }
    }
}
