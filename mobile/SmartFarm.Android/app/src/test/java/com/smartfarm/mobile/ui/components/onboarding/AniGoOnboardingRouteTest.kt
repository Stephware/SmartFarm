package com.smartfarm.mobile.ui.components.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class AniGoOnboardingRouteTest {
    @Test
    fun authenticatedSessionsAlwaysBypassFirstRunOnboarding() {
        assertEquals("farms", anigoInitialRoute(completed = false, authenticated = true))
        assertEquals("farms", anigoInitialRoute(completed = true, authenticated = true))
        assertEquals("onboarding", anigoInitialRoute(completed = false, authenticated = false))
    }

    @Test
    fun replayDestinationLabelsNeverImplyFirstRunCompletion() {
        assertEquals("Back to My Account", anigoReplayExitLabel(authenticated = true))
        assertEquals("Back to Welcome", anigoReplayExitLabel(authenticated = false))
    }

    @Test
    fun firstLaunchStartsInGuideAndCompletedInstallStartsAtWelcome() {
        assertEquals("onboarding", anigoInitialRoute(completed = false))
        assertEquals("welcome", anigoInitialRoute(completed = true))
    }
}
