package com.smartfarm.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun welcomeScreenIsDisplayed() {
        compose.onNodeWithText("Welcome to SmartFarm").assertIsDisplayed()
    }
}
