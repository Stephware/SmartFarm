package com.smartfarm.mobile.ui.components.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniGoGuideMotionTest {
    @Test
    fun eachTeachingActionTriggersAVisibleButBoundedMascotReaction() {
        val examples = listOf(
            0 to .18f,
            1 to .39f,
            2 to .59f,
            3 to .58f
        )
        examples.forEach { (page, event) ->
            val gesture = AniGoGuideMotion.gesture(page, event)
            assertTrue("Page $page should respond at $event", gesture.frame > 0)
            assertTrue("Gesture must be subtle", gesture.liftDp in -4f..0f)
            assertTrue("No excessive rotation", gesture.tiltDegrees in -1.6f..1.6f)
        }
    }

    @Test
    fun firstAndFinalFramesAlwaysReturnToNeutralPose() {
        repeat(4) { page ->
            assertEquals(AniGoGuideMotion.MascotGesture(), AniGoGuideMotion.gesture(page, 0f))
            assertEquals(AniGoGuideMotion.MascotGesture(), AniGoGuideMotion.gesture(page, 1f))
        }
    }

    @Test
    fun outOfRangePagesOrProgressAreSafe() {
        assertEquals(AniGoGuideMotion.MascotGesture(), AniGoGuideMotion.gesture(-1, .5f))
        assertEquals(AniGoGuideMotion.MascotGesture(), AniGoGuideMotion.gesture(10, .5f))
        assertEquals(AniGoGuideMotion.MascotGesture(), AniGoGuideMotion.gesture(0, -2f))
        assertEquals(AniGoGuideMotion.MascotGesture(), AniGoGuideMotion.gesture(0, 2f))
    }
}
