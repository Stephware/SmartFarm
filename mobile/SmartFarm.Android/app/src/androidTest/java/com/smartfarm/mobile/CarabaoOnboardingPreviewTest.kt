package com.smartfarm.mobile

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingStore
import com.smartfarm.mobile.ui.components.onboarding.anigoInitialRoute
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingScreen
import com.smartfarm.mobile.ui.components.onboarding.AniGoFarmIntro
import com.smartfarm.mobile.ui.components.onboarding.AniGoWelcomePage
import com.smartfarm.mobile.ui.components.onboarding.anigoMotionAllowed
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CarabaoOnboardingPreviewTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun realJourneyContainsWelcomePlusAllFourFarmLessonsBeforeLogin() {
        var exits = 0
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(onExit = { exits++ }, motionAllowed = false)
            }
        }
        compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
        compose.onNodeWithText("Hi! I'm Ani, your farm buddy.").assertExists()
        compose.onNodeWithText("Back").assertIsNotEnabled()
        val lessons = listOf(
            "Your farm, organized",
            "Grow with a plan",
            "Record work as you go",
            "From field to harvest"
        )
        lessons.forEach { heading ->
            compose.onNodeWithText("Next →").performClick()
            compose.waitForIdle()
            compose.onNodeWithText(heading).assertIsDisplayed()
        }
        compose.onNodeWithText("Continue to AniGo").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun welcomeSlideRendersRasterMascotAndRespondsToBackNavigation() {
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(onExit = {}, motionAllowed = false)
            }
        }
        compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
        compose.onNodeWithContentDescription("AniGo carabao waving").assertExists()
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
    }

    @Test
    fun captureApprovedWelcomeLightForVisualReview() {
        withAppearance(dark = false) {
            compose.setContent {
                SmartFarmTheme { AniGoOnboardingScreen(onExit = {}, motionAllowed = false) }
            }
            compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
            saveScreenshot("00-welcome-light")
        }
    }

    @Test
    fun captureApprovedWelcomeDarkForVisualReview() {
        withAppearance(dark = true) {
            compose.setContent {
                SmartFarmTheme { AniGoOnboardingScreen(onExit = {}, motionAllowed = false) }
            }
            compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
            saveScreenshot("00-welcome-dark")
        }
    }

    @Test
    fun introductionUsesWalkingSpriteUntilArrival() {
        compose.setContent {
            SmartFarmTheme {
                AniGoFarmIntro(onFinished = {}, motionAllowed = true,
                    autoPlay = false, previewProgress = .25f)
            }
        }
        compose.onNodeWithContentDescription("AniGo carabao walking").assertExists()
        compose.onNodeWithContentDescription("AniGo carabao waving").assertDoesNotExist()
    }

    @Test
    fun introductionSettlesOnWavingSpriteAfterWalking() {
        compose.setContent {
            SmartFarmTheme {
                AniGoFarmIntro(onFinished = {}, motionAllowed = true,
                    autoPlay = false, previewProgress = .91f)
            }
        }
        compose.onNodeWithContentDescription("AniGo carabao waving").assertExists()
        compose.onNodeWithContentDescription("AniGo carabao walking").assertDoesNotExist()
    }

    @Test
    fun compactLargeTextIntroductionAlsoSettlesOnWavingSprite() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(720.dp, 400.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme {
                        AniGoFarmIntro(onFinished = {}, motionAllowed = true,
                            autoPlay = false, previewProgress = .91f)
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("AniGo carabao waving").assertExists()
        compose.onNodeWithContentDescription("AniGo carabao walking").assertDoesNotExist()
    }

    @Test
    fun talkingWelcomeStopsAndReturnsToWavingPose() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SmartFarmTheme {
                AniGoWelcomePage(active = true, activityResumed = true, motionAllowed = true)
            }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithContentDescription("AniGo carabao speaking").assertExists()
        compose.mainClock.advanceTimeBy(2_500)
        compose.onNodeWithContentDescription("AniGo carabao waving").assertExists()
        compose.onNodeWithContentDescription("AniGo carabao speaking").assertDoesNotExist()
        // The greeting plays once, not continuously while this page remains selected.
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithContentDescription("AniGo carabao waving").assertExists()
    }

    @Test
    fun phoneLessonsShowReadableAniGuidanceAndRetainAllExamples() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 780.dp))) {
                SmartFarmTheme {
                    AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false)
                }
            }
        }
        val tips = listOf(
            "Start your own farm, or ask to join someone's team.",
            "Choose what to grow, then follow each stage of your plan.",
            "Some records stay on your phone until you can sync them.",
            "Harvest a little today? Record each batch as you go."
        )
        tips.forEachIndexed { page, tip ->
            compose.onNode(hasTestTag("anigo-lesson-stage-$page")).assertExists()
            compose.onNodeWithText("ANI'S FARM TOUR  ·  ${page + 1} / 4").assertExists()
            compose.onNodeWithText(tip).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Skip").assertIsDisplayed()
            if (page < 3) {
                compose.onNodeWithText("Next →").performClick()
                compose.waitForIdle()
            } else {
                compose.onNodeWithText("Continue to AniGo").assertIsDisplayed()
            }
        }
    }

    @Test
    fun capturePhoneWelcomeAndFirstGuideLessonForVisualReview() {
        withAppearance(dark = false) {
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 780.dp))) {
                    SmartFarmTheme {
                        AniGoOnboardingScreen(onExit = {}, motionAllowed = false)
                    }
                }
            }
            compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
            compose.onNodeWithText("Hi! I'm Ani, your farm buddy.").assertExists()
            saveScreenshot("00-welcome-phone-light")
            compose.onNodeWithText("Next →").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
            saveScreenshot("01-farm-phone-light")
        }
    }

    @Test
    fun capturePhoneHarvestAndTipForVisualReview() {
        withAppearance(dark = true) {
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(390.dp, 840.dp))) {
                    SmartFarmTheme {
                        AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false)
                    }
                }
            }
            repeat(3) {
                compose.onNodeWithText("Next →").performClick()
                compose.waitForIdle()
            }
            compose.onNodeWithText("From field to harvest").assertIsDisplayed()
            compose.onNodeWithText("Harvest a little today? Record each batch as you go.")
                .performScrollTo().assertIsDisplayed()
            saveScreenshot("04-harvest-phone-dark")
        }
    }

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
            SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = { exits++ }, motionAllowed = false) }
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
                    SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = { exits++ }, motionAllowed = false) }
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
            SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, 
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
            SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = { exits++ }, motionAllowed = false) }
        }
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.onNodeWithText("Skip").performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test
    fun reducedMotionNavigationDoesNotStartPagerAnimations() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false) }
        }
        compose.onNodeWithText("Next →").performClick()
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
    }

    @Test
    fun returningFromSystemSettingsRechecksReducedMotion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val setting = Settings.Global.ANIMATOR_DURATION_SCALE
        val original = Settings.Global.getString(resolver, setting)
        val owner = GuideLifecycleOwner()
        instrumentation.uiAutomation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
        try {
            check(Settings.Global.putFloat(resolver, setting, 1f))
            compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
            compose.setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    Text(if (anigoMotionAllowed()) "Motion enabled" else "Motion disabled")
                }
            }
            compose.onNodeWithText("Motion enabled").assertExists()
            compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
            compose.runOnIdle {
                check(Settings.Global.putFloat(resolver, setting, 0f))
                owner.lifecycle.currentState = Lifecycle.State.RESUMED
            }
            compose.onNodeWithText("Motion disabled").assertExists()
        } finally {
            Settings.Global.putString(resolver, setting, original)
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }

    @Test
    fun swipeAndRecreationKeepTheSelectedLesson() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false) }
        }
        compose.onRoot().performTouchInput { swipeLeft() }
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
    }

    @Test
    fun introPausesInBackgroundAndFinishesAfterResume() {
        val owner = GuideLifecycleOwner()
        var finishes = 0
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                SmartFarmTheme { AniGoFarmIntro(onFinished = { finishes++ }, motionAllowed = true) }
            }
        }
        compose.mainClock.advanceTimeBy(512)
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        compose.mainClock.advanceTimeBy(4_000)
        compose.runOnIdle { assertEquals(0, finishes) }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertEquals(1, finishes) }
    }

    @Test
    fun lessonPausesInBackgroundAndResumesToTheCompleteExample() {
        val owner = GuideLifecycleOwner()
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                SmartFarmTheme {
                    AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = true, introAutoPlay = false)
                }
            }
        }
        compose.onNodeWithText("Skip intro").performClick()
        compose.mainClock.advanceTimeBy(512)
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.STARTED }
        compose.mainClock.advanceTimeBy(4_000)
        compose.onNodeWithContentDescription("Sample farm setup, step 1 of 4; demonstration only")
            .assertExists()
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.advanceTimeBy(2_500)
        compose.onNodeWithText("Owner & team members").assertExists()
    }

    @Test
    fun farmAndCropDemonstrationsShowCompleteExamplesWithReducedMotion() {
        compose.setContent {
            SmartFarmTheme {
                AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false)
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
                AniGoOnboardingScreen(includeWelcome = false, 
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
            SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false) }
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
                AniGoOnboardingScreen(includeWelcome = false, 
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
                AniGoOnboardingScreen(includeWelcome = false, onExit = { exits++ }, motionAllowed = false)
            }
        }
        compose.onNodeWithText("Next →").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Grow with a plan").assertIsDisplayed()

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitForIdle()
        compose.onNodeWithText("Your farm, organized").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, exits) }

        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
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
                        AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false)
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
        withAppearance(dark = false) {
            compose.setContent {
                SmartFarmTheme {
                    AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = true, introAutoPlay = false)
                }
            }
            compose.onNodeWithText("Skip intro").performClick()
            captureAllGuidePages("light")
        }
    }

    @Test
    fun captureDarkThemeOnboardingPagesForVisualReview() {
        withAppearance(dark = true) {
            compose.setContent {
                SmartFarmTheme {
                    AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = true, introAutoPlay = false)
                }
            }
            compose.onNodeWithText("Skip intro").performClick()
            captureAllGuidePages("dark")
        }
    }

    @Test
    fun captureFinishedIntroWithActualComposeArtForVisualReview() {
        withAppearance(dark = false) {
            compose.setContent {
                SmartFarmTheme {
                    AniGoFarmIntro(
                        onFinished = {},
                        motionAllowed = true,
                        autoPlay = false,
                        previewProgress = .91f
                    )
                }
            }
            compose.waitForIdle()
            compose.onNodeWithText("A better way to care for your farm.").assertIsDisplayed()
            saveScreenshot("intro-light")
        }
    }

    @Test
    fun captureCompactLargeTextGuideForVisualReview() {
        withAppearance(dark = false) {
            compose.setContent {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))
                ) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                        SmartFarmTheme {
                            AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false)
                        }
                    }
                }
            }
            compose.onNodeWithText("Riverside Farm").performScrollTo()
            saveScreenshot("farm-320dp-large-text")
        }
    }

    @Test
    fun compactDarkLargeTextKeepsAllLessonsAndControlsReachable() {
        withAppearance(dark = true) {
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                        SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false) }
                    }
                }
            }
            listOf("Riverside Farm", "Growing", "Pending sync", "Batch 2 · 35 kg")
                .forEachIndexed { page, sample ->
                    compose.onNodeWithText(sample).performScrollTo().assertIsDisplayed()
                    compose.onNode(hasText("Illustrative example · no farm records are created") and
                        hasAnyAncestor(hasTestTag("onboarding-page-$page")))
                        .performScrollTo().assertIsDisplayed()
                    saveScreenshot("large-text-dark-${page + 1}")
                    compose.onNodeWithText("Skip").assertIsDisplayed()
                    compose.onNodeWithText(if (page == 3) "Continue to AniGo" else "Next →")
                        .assertIsDisplayed()
                    if (page < 3) compose.onNodeWithText("Next →").performClick()
                }
        }
    }

    @Test
    fun captureTabletLessonsWithoutStretchingThePhoneComposition() {
        withAppearance(dark = false) {
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 1280.dp))) {
                    SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false) }
                }
            }
            captureAllGuidePages("tablet-light")
        }
    }

    @Test
    fun captureLandscapeLessonsWithReachableNavigation() {
        withAppearance(dark = true) {
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(720.dp, 400.dp))) {
                    SmartFarmTheme { AniGoOnboardingScreen(includeWelcome = false, onExit = {}, motionAllowed = false) }
                }
            }
            captureAllGuidePages("landscape-dark")
            compose.onNodeWithText("85 kg").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText("Continue to AniGo").assertIsDisplayed()
        }
    }

    @Test
    fun compactDarkIntroScrollsWithoutCoveringItsSkipAction() {
        withAppearance(dark = true) {
            var finishes = 0
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(720.dp, 400.dp))) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                        SmartFarmTheme {
                            AniGoFarmIntro(onFinished = { finishes++ }, motionAllowed = true,
                                autoPlay = false, previewProgress = .91f)
                        }
                    }
                }
            }
            compose.onNodeWithText("A better way to care for your farm.").assertIsDisplayed()
            compose.onNodeWithText("Your farm, your crops, your progress").assertIsDisplayed()
            compose.onNodeWithText("Skip intro").assertIsDisplayed()
            assertLandscapeIntroMascotFullyVisible()
            saveScreenshot("intro-landscape-dark-large-text")
            compose.onNodeWithText("Skip intro").performClick()
            compose.runOnIdle { assertEquals(1, finishes) }
        }
    }

    @Test
    fun compactLightLandscapeIntroShowsFullMascotAndReadableCopy() {
        withAppearance(dark = false) {
            compose.setContent {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(720.dp, 400.dp))) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                        SmartFarmTheme {
                            AniGoFarmIntro(onFinished = {}, motionAllowed = true,
                                autoPlay = false, previewProgress = .91f)
                        }
                    }
                }
            }
            compose.onNodeWithText("A better way to care for your farm.").assertIsDisplayed()
            compose.onNodeWithText("Your farm, your crops, your progress").assertIsDisplayed()
            compose.onNodeWithText("Skip intro").assertIsDisplayed()
            assertLandscapeIntroMascotFullyVisible()
            saveScreenshot("intro-landscape-light-large-text")
        }
    }

    private fun assertLandscapeIntroMascotFullyVisible() {
        val mascot = compose.onNodeWithTag("anigo-intro-landscape-mascot")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(
            "AniGo mascot must fit entirely within short-landscape viewport",
            mascot.left >= screen.left && mascot.right <= screen.right &&
                mascot.top >= screen.top && mascot.bottom <= screen.bottom
        )
    }

    @Test
    fun captureFinishedDarkIntroForVisualReview() {
        withAppearance(dark = true) {
            compose.setContent {
                SmartFarmTheme {
                    AniGoFarmIntro(onFinished = {}, motionAllowed = true,
                        autoPlay = false, previewProgress = .91f)
                }
            }
            compose.onNodeWithText("Skip intro").assertIsDisplayed()
            saveScreenshot("intro-dark")
        }
    }

    private fun captureAllGuidePages(theme: String) {
        compose.waitForIdle()
        if (theme == "landscape-dark") assertLandscapeSampleVisible(0)
        saveScreenshot("01-farm-$theme")
        repeat(3) { index ->
            // Capture the settled screen without a native touch-ripple frame.
            // Physical button/swipe navigation is exercised by the functional tests.
            compose.onNodeWithText("Next →").performSemanticsAction(SemanticsActions.OnClick) { it() }
            compose.waitForIdle()
            if (theme == "landscape-dark") assertLandscapeSampleVisible(index + 1)
            val label = when (index) {
                0 -> "02-crop"
                1 -> "03-work"
                else -> "04-harvest"
            }
            saveScreenshot("$label-$theme")
        }
    }

    private fun assertLandscapeSampleVisible(page: Int) {
        val first = listOf("Riverside Farm", "Rice planting", "Today's fieldwork", "Harvest records")[page]
        val last = listOf("Owner & team members", "Upcoming: crop activities", "Pending sync",
            "View farm supplies separately")[page]
        compose.onNodeWithText(last).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(first).assertIsDisplayed()
    }

    private fun withAppearance(dark: Boolean, action: () -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences(
            "appearance", android.content.Context.MODE_PRIVATE
        )
        val old = preferences.getBoolean("dark", false)
        preferences.edit().putBoolean("dark", dark).commit()
        try {
            action()
        } finally {
            preferences.edit().putBoolean("dark", old).commit()
        }
    }

    private fun saveScreenshot(filename: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Environment.DIRECTORY_PICTURES + "/AniGoOnboardingAudit/"
        // Replace only this reserved review image, so export cannot pick up an older run.
        context.contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf("$filename.png", directory))
        // MediaStore entries survive the test runner's app-uninstall cleanup.
        // App-private storage does not, and cannot be inspected after Gradle exits.
        val uri = requireNotNull(context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename + ".png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    directory
                )
            }
        )) { "Failed to create review screenshot: $filename" }
        context.contentResolver.openOutputStream(uri)?.use { output ->
            check(
                compose.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, output)
            ) { "Failed to render review screenshot: $filename" }
        } ?: error("Cannot write review screenshot: $filename")
    }

    private class GuideLifecycleOwner : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
    }
    @Test fun introSystemBackExitsToCallerWithoutEnteringALesson() {
        var exits = 0
        compose.setContent { SmartFarmTheme {
            AniGoOnboardingScreen(onExit = { exits++ }, motionAllowed = true, introAutoPlay = false)
        } }
        compose.onNodeWithText("Skip intro").assertExists()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.runOnIdle { assertEquals(1, exits) }
        compose.onNodeWithTag("onboarding-page-0").assertDoesNotExist()
    }

}
