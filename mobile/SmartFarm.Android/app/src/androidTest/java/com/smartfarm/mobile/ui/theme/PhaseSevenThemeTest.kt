package com.smartfarm.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exact Phase 7 color roles; values belong to the native Compose theme. */
class PhaseSevenThemeTest {
    @Test
    fun darkSchemeIsCharcoalWithPineAndGold() {
        assertEquals(Color(0xFF121917), DarkColors.background)
        assertEquals(Color(0xFF202B26), DarkColors.surface)
        assertEquals(Color(0xFF2A3832), DarkColors.surfaceVariant)
        assertEquals(Color(0xFF19382C), DarkColors.primaryContainer)
        assertEquals(Color(0xFFD4AD61), DarkColors.secondary)
        assertEquals(Color(0xFFF1F4F0), DarkColors.onBackground)
    }

    @Test
    fun filledActionAndDarkSurfaceTextAreReadable() {
        assertTrue(contrast(AniGoPalette.darkAction, AniGoPalette.onDarkAction) >= 4.5f)
        assertTrue(contrast(DarkColors.primary, DarkColors.surface) >= 4.5f)
        assertTrue(contrast(DarkColors.onSurfaceVariant, DarkColors.surface) >= 4.5f)
        assertTrue(contrast(DarkColors.secondary, DarkColors.background) >= 4.5f)
    }

    @Test
    fun existingLightModeRemainsUnchanged() {
        assertEquals(Color.White, LightColors.surface)
        assertEquals(AniGoPalette.cream, LightColors.background)
    }

    private fun contrast(a: Color, b: Color): Float {
        val bright = maxOf(a.luminance(), b.luminance())
        val dark = minOf(a.luminance(), b.luminance())
        return (bright + 0.05f) / (dark + 0.05f)
    }
}
