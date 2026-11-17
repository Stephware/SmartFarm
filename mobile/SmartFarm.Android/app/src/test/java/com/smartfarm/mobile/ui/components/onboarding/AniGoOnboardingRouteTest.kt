package com.smartfarm.mobile.ui.components.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class AniGoOnboardingRouteTest {
    @Test
    fun firstLaunchStartsInGuideAndCompletedInstallStartsAtWelcome() {
        assertEquals("onboarding", anigoInitialRoute(completed = false))
        assertEquals("welcome", anigoInitialRoute(completed = true))
    }
}
