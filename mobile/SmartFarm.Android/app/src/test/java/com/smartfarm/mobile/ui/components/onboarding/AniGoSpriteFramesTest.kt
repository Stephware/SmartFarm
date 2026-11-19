package com.smartfarm.mobile.ui.components.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AniGoSpriteFramesTest {
    @Test fun walkingUsesFourDifferentRasterFramesAndLoops() {
        val frames = (0 until 4).map { aniGoSpriteIndex(AniGoCarabaoPose.WAVE, frame = it, walking = true) }
        assertEquals(listOf(0, 1, 2, 3), frames)
        assertEquals(0, aniGoSpriteIndex(AniGoCarabaoPose.WAVE, frame = 4, walking = true))
    }

    @Test fun speakingSwitchesBetweenTwoMouthImages() {
        assertEquals(4, aniGoSpriteIndex(AniGoCarabaoPose.WAVE, frame = 0, talking = true))
        assertEquals(5, aniGoSpriteIndex(AniGoCarabaoPose.WAVE, frame = 1, talking = true))
        assertEquals(4, aniGoSpriteIndex(AniGoCarabaoPose.WAVE, frame = 2, talking = true))
    }

    @Test fun everyLessonUsesItsApprovedPoseImage() {
        val expected = mapOf(
            AniGoCarabaoPose.WAVE to 6,
            AniGoCarabaoPose.GUIDE to 7,
            AniGoCarabaoPose.PLANT to 8,
            AniGoCarabaoPose.RECORD to 9,
            AniGoCarabaoPose.HARVEST to 10,
            AniGoCarabaoPose.CELEBRATE to 11
        )
        expected.forEach { (pose, index) ->
            assertEquals(index, aniGoSpriteIndex(pose))
        }
        assertTrue(expected.values.distinct().size == 6)
    }
}
