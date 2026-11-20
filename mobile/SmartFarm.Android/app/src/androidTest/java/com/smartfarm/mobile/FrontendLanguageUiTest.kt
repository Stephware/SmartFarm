package com.smartfarm.mobile

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.localization.AppLanguage
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingStore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class FrontendLanguageUiTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val setup = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            AniGoOnboardingStore.create(context).markCompleted()
            InstrumentationRegistry.getInstrumentation().runOnMainSync { AppLanguage.select(context, "en") }
        }
        override fun after() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            InstrumentationRegistry.getInstrumentation().runOnMainSync { AppLanguage.select(context, "en") }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(setup).around(compose)
    @Test fun languageSurvivesRecreationAndNavigationWhileEmailDraftStaysData() {
        // The welcome/login forms no longer expose language controls.
        // Locale changes occur from My Account's preference selector (covered
        // by AccountPreferencesTest); exercise the same AppLanguage persistence
        // while a guest login form contains unfinished input.
        compose.onNodeWithText("Filipino").assertDoesNotExist()
        compose.onAllNodesWithText("Log in").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput("Today")
        compose.onNodeWithText("Enter a valid email address.").assertExists()
        compose.activityRule.scenario.onActivity { AppLanguage.select(it, "fil") }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Maglagay ng wastong email address.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Today").assertExists()
        compose.activityRule.scenario.onActivity { AppLanguage.select(it, "en") }
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Enter a valid email address.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Today").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Enter a valid email address.").assertExists()
    }
}
