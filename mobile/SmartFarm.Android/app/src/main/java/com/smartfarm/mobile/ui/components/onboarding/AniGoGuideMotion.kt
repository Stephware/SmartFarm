package com.smartfarm.mobile.ui.components.onboarding

import kotlin.math.abs
import kotlin.math.max

/**
 * Shared choreography for AniGo's four small, one-shot teaching demos.
 *
 * Each page has three/four meaningful on-screen events. The carabao reacts
 * briefly *at those events* rather than constantly bobbing or waving.
 * The final frame is always neutral, so static screenshots and replay end cleanly.
 */
internal object AniGoGuideMotion {
    const val GuideEnterMillis = 240
    const val DemoMillis = 2100
    const val IntroMillis = 2550

    private val beats = arrayOf(
        floatArrayOf(.18f, .46f, .73f),         // farm plots, team
        floatArrayOf(.23f, .39f, .73f),         // seed, planting, growth
        floatArrayOf(.10f, .36f, .59f, .79f),   // tasks, photo, pending sync
        floatArrayOf(.28f, .58f, .80f)          // partial harvests, supplies
    )

    fun gesture(page: Int, progress: Float): MascotGesture {
        if (page !in beats.indices) return MascotGesture()
        val p = progress.coerceIn(0f, 1f)
        if (p < .04f || p >= .98f) return MascotGesture()
        var intensity = 0f
        for (beat in beats[page]) {
            intensity = max(intensity, (1f - abs(p - beat) / .095f).coerceIn(0f, 1f))
        }
        // Two short peaks per action (raising and lowering the hoof/clipboard).
        val frame = when {
            intensity < .12f -> 0
            intensity < .42f -> 1
            intensity < .75f -> 2
            else -> 3
        }
        val tilt = if (page % 2 == 0) -1f else 1f
        return MascotGesture(frame, -4f * intensity, tilt * 1.6f * intensity)
    }

    data class MascotGesture(
        val frame: Int = 0,
        val liftDp: Float = 0f,
        val tiltDegrees: Float = 0f
    )
}
