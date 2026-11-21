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
        // Language selection now lives in Account. Exercise the same platform
        // locale change here while signed out; AccountPreferencesTest covers its UI.
        compose.activityRule.scenario.onActivity { AppLanguage.select(it, "fil") }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Dito nagsisimula ang iyong bukid.").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Mag-log in").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Email address").performTextInput("Today")
        compose.onNodeWithText("Maglagay ng wastong email address.").assertExists()
        compose.activityRule.scenario.onActivity { AppLanguage.select(it, "en") }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Enter a valid email address.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Today").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Enter a valid email address.").assertExists()
    }
}
