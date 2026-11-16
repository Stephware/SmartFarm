package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
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
        compose.onNodeWithText("City, town or farm address").performScrollTo().performTextInput("Bulacan")
        compose.onNodeWithText("Farm name").performScrollTo()
            .performTextInput("Farm".repeat(31))
        compose.onNodeWithText("Farm name must be 120 characters or fewer.")
            .performScrollTo().assertExists()
        compose.onNode(hasText("Create farm") and hasClickAction()).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { org.junit.Assert.assertEquals(0, creations) }
    }

    @Test fun farmNameAndLocationFollowTheExistingTwoCharacterMinimum() {
        var creations = 0
        compose.setContent { SmartFarmTheme {
            FarmListScreen(FarmUiState(), {}, { _, _, _, _, _ -> creations++ }, {}, {})
        } }
        compose.onNodeWithText("Add a farm").performClick()
        compose.onNodeWithText("Farm name").performScrollTo().performTextInput("A")
        compose.onNodeWithText("City, town or farm address").performScrollTo().performTextInput("Bulacan")
        compose.onNode(hasText("Create farm") and hasClickAction()).assertIsNotEnabled()
        compose.onNode(hasText("Farm name") and hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Error, "Farm name must be at least 2 characters."))
        compose.onNodeWithText("Farm name must be at least 2 characters.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Farm name").performScrollTo().performTextReplacement("AniGo farm")
        compose.onNodeWithText("City, town or farm address").performScrollTo().performTextReplacement("B")
        compose.onNode(hasText("Create farm") and hasClickAction()).assertIsNotEnabled()
        compose.onNodeWithText("Location must be at least 2 characters.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("City, town or farm address").performScrollTo().performTextReplacement("Bulacan")
        compose.onNode(hasText("Create farm") and hasClickAction()).assertIsEnabled()
        compose.runOnIdle { org.junit.Assert.assertEquals(0, creations) }
    }
}
