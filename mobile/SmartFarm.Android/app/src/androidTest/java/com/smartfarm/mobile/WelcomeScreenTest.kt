package com.smartfarm.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun welcomeScreenIsDisplayed() {
        // A new installation intentionally opens the AniGo tour first. Reach the
        // welcome page the same way a farmer would, rather than bypassing the
        // first-run logic or assuming onboarding was already completed.
        compose.waitForIdle()
        if (compose.onAllNodesWithText("Skip intro").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Skip intro").performClick()
            compose.waitForIdle()
        }
        if (compose.onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Skip").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Welcome to AniGo").assertIsDisplayed()
        compose.onNodeWithText("Developer diagnostics").performScrollTo().performClick()
        compose.onNodeWithText("Check backend").performScrollTo().assertIsDisplayed()
    }
}

