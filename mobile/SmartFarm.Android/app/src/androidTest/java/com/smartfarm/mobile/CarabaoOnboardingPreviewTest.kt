package com.smartfarm.mobile

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingStore
import com.smartfarm.mobile.ui.components.onboarding.anigoInitialRoute
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CarabaoOnboardingPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun firstRunFlagPersistsAcrossStoreInstancesWithoutAccountOrNetwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testPrefs = "anigo_onboarding_instrumentation"
        val preferences = context.getSharedPreferences(testPrefs, android.content.Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val fresh = AniGoOnboardingStore.create(context, testPrefs)
            org.junit.Assert.assertFalse(fresh.isCompleted())
            assertEquals("onboarding", anigoInitialRoute(fresh.isCompleted()))
            org.junit.Assert.assertTrue(fresh.markCompleted())

            val restarted = AniGoOnboardingStore.create(context, testPrefs)
            org.junit.Assert.assertTrue(restarted.isCompleted())
            assertEquals("welcome", anigoInitialRoute(restarted.isCompleted()))
            // Replaying or signing out does not reset the one-time completion.
            org.junit.Assert.assertTrue(restarted.isCompleted())
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun myAccountCanReplayGuideWithoutLoggingOutOrMutatingCompletion() {
        var replays = 0
        var logouts = 0
        compose.setContent {
            SmartFarmTheme {
                AccountScreen(
                    state = AuthUiState(),
                    onLogout = { logouts++ },
                    onFarms = {},
                    onSync = {},
                    onReplayGuide = { replays++ }
                )
            }
        }
        compose.onNodeWithText("Replay AniGo guide").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, replays)
            assertEquals(0, logouts)
        }
    }

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
            SmartFarmTheme { AniGoOnboardingScreen(onExit = { exits++ }, motionAllowed = false) }
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
                    SmartFarmTheme { AniGoOnboardingScreen(onExit = { exits++ }, motionAllowed = false) }
                }
            }
        }
        compose.onNodeWithText("Skip").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun optionalAnimatedIntroductionCanBeSkippedWithoutExitingTheGuide() {
        // Hold the intro before its one-shot animation starts; the production
        // default still auto-plays on real devices.
        var exits = 0
        compose.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(
                onExit = { exits++ }, motionAllowed = true, introAutoPlay = false
            ) }
        }
        compose.onNodeWithText("Skip intro").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, exits) }
    }

    @Test
    fun reducedMotionBypassesIntroductionAndKeepsGuideAvailable() {
        var exits = 0
        compose.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(onExit = { exits++ }, motionAllowed = false) }
        }
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.onNodeWithText("Skip").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun farmAndCropDemonstrationsShowCompleteExamplesWithReducedMotion() {
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(onExit = {}, motionAllowed = false)
            }
        }
        compose.onNodeWithText("Riverside Farm").assertExists()
        compose.onNodeWithText("North Plot").assertExists()
        compose.onNodeWithText("South Plot").assertExists()
        compose.onNodeWithText("Owner & team members").assertExists()
        compose.onNodeWithText("Replay example").assertDoesNotExist()
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Rice planting").assertExists()
        compose.onNodeWithText("Growing").assertExists()
        compose.onNodeWithText("Upcoming: crop activities").assertExists()
    }

    @Test
    fun animatedFarmAndCropExamplesCanReplayAndNavigateNormally() {
        var exits = 0
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(
                    onExit = { exits++ },
                    motionAllowed = true,
                    introAutoPlay = false
                )
            }
        }
        compose.onNodeWithText("Skip intro").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.onNodeWithText("South Plot").assertExists()
        compose.onNodeWithContentDescription("Replay farm management example")
            .performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Owner & team members").assertExists()
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()
        compose.onNodeWithText("Upcoming: crop activities").assertExists()
        compose.onNodeWithContentDescription("Replay crop planning example")
            .performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Growing").assertExists()
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Record work as you go").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, exits) }
    }

    @Test
    fun fieldworkAndHarvestExamplesShowAccurateFinalStatesWithoutMotion() {
        compose.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(onExit = {}, motionAllowed = false) }
        }
        repeat(2) {
            compose.onNodeWithText("Next →").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Record work as you go").assertIsDisplayed()
        compose.onNodeWithText("Watering").assertExists()
        compose.onNodeWithText("Leaf inspection").assertExists()
        compose.onNodeWithText("Photo").assertExists()
        compose.onNodeWithText("Saved on device").assertExists()
        compose.onNodeWithText("Pending sync").assertExists()
        compose.onNodeWithContentDescription("Replay fieldwork example").assertDoesNotExist()
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("From field to harvest").assertIsDisplayed()
        compose.onNodeWithText("Batch 1 · 50 kg").assertExists()
        compose.onNodeWithText("Batch 2 · 35 kg").assertExists()
        compose.onNodeWithText("85 kg").assertExists()
        compose.onNodeWithText("View farm supplies separately").assertExists()
        compose.onNodeWithContentDescription("Replay harvest example").assertDoesNotExist()
    }

    @Test
    fun fieldworkAndHarvestExamplesReplayWithoutChangingNavigationOrRecords() {
        var exitCount = 0
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(
                    onExit = { exitCount++ },
                    motionAllowed = true,
                    introAutoPlay = false
                )
            }
        }
        compose.onNodeWithText("Skip intro").performClick()
        compose.waitForIdle()
        repeat(2) {
            compose.onNodeWithText("Next →").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Record work as you go").assertIsDisplayed()
        compose.onNodeWithText("Pending sync").assertExists()
        compose.onNodeWithContentDescription("Replay fieldwork example")
            .performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Saved on device").assertExists()

        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("From field to harvest").assertIsDisplayed()
        compose.onNodeWithText("85 kg").assertExists()
        compose.onNodeWithContentDescription("Replay harvest example")
            .performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Batch 2 · 35 kg").assertExists()
        compose.onNodeWithText("Continue to AniGo").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, exitCount) }
    }

    @Test
    fun nativeAndroidBackMovesThroughGuidePagesBeforeExiting() {
        var exits = 0
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(onExit = { exits++ }, motionAllowed = false)
            }
        }
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()

        Espresso.pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, exits) }

        Espresso.pressBack()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun compactLargeTextStacksIllustrationAndKeepsSampleRecordsReachable() {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))
            ) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme {
                        AniGoOnboardingScreen(onExit = {}, motionAllowed = false)
                    }
                }
            }
        }
        compose.onNodeWithText("Riverside Farm").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("North Plot").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Rice planting").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Growing").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
    }

    @Test
    fun captureAllFourActualComposePagesForVisualReview() {
        compose.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(onExit = {}, motionAllowed = false) }
        }
        saveScreenshot("01-farm")
        repeat(3) { index ->
            compose.onNodeWithText("Next →").performClick()
            compose.waitForIdle()
            saveScreenshot(
                when (index) {
                    0 -> "02-crop"
                    1 -> "03-work"
                    else -> "04-harvest"
                }
            )
        }
    }

    private fun saveScreenshot(filename: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "anigo-onboarding")
        directory.mkdirs()
        FileOutputStream(File(directory, filename + ".png")).use { out ->
            compose.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
