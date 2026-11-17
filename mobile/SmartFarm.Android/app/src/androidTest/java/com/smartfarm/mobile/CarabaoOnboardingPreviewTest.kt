package com.smartfarm.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CarabaoOnboardingPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun previewIsOptInAndDoesNotChangeExistingAuthenticationChoices() {
        var previews = 0
        var logins = 0
        var registrations = 0
        compose.setContent {
            SmartFarmTheme {
                WelcomeScreen(
                    onLogin = { logins++ },
                    onRegister = { registrations++ },
                    onPreviewGuide = { previews++ }
                )
            }
        }
        compose.onNodeWithText("Explore AniGo with our farm guide").performScrollTo().performClick()
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, previews)
            assertEquals(1, logins)
            assertEquals(1, registrations)
        }
    }

    @Test
    fun fourSlidesCanBeViewedAndBackNavigatedWithoutCreatingFarmData() {
        var exits = 0
        compose.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(onExit = { exits++ }) }
        }
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.onNodeWithText("Riverside Farm").assertExists()
        compose.onNodeWithText("Back").assertIsNotEnabled()

        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()

        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Record work as you go").assertIsDisplayed()
        compose.onNodeWithText("Local save is not server confirmation", substring = true)
            .assertExists()

        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("From field to harvest").assertIsDisplayed()
        compose.onNodeWithText("Continue to AniGo").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun skipWorksOnSmallScreensWithLargeText() {
        var exits = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme { AniGoOnboardingScreen(onExit = { exits++ }) }
                }
            }
        }
        compose.onNodeWithText("Skip").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }
}
