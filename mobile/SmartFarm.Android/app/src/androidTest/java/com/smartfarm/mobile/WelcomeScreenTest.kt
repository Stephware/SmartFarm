package com.smartfarm.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun welcomeScreenIsDisplayed() {
        compose.skipFirstRunGuideIfShown()
        compose.onNodeWithText("Your farm starts here.").assertIsDisplayed()
        compose.onNodeWithText("Developer diagnostics").performScrollTo().performClick()
        compose.onNodeWithText("Check backend").performScrollTo().assertIsDisplayed()
    }
}

