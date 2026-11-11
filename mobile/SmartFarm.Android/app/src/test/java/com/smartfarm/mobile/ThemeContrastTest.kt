package com.smartfarm.mobile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.smartfarm.mobile.ui.theme.LightColors
import com.smartfarm.mobile.ui.theme.DarkColors
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeContrastTest {
    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance(); val b = second.luminance()
        return (max(a, b) + .05f) / (min(a, b) + .05f)
    }

    @Test fun lightWorkspaceUsesThePinnedReferencePalette() {
        assertEquals(Color(0xFF28503C), LightColors.primary)
        assertEquals(Color(0xFF153D2D), LightColors.onPrimaryContainer)
        assertEquals(Color(0xFFFFF2D8), LightColors.secondaryContainer)
        assertEquals(Color.White, LightColors.surface)
        assertEquals(Color(0xFFF7F8F4), LightColors.background)
    }

    @Test fun textAndControlsRemainReadableInBothThemes() {
        for (colors in listOf(LightColors, DarkColors)) {
            for ((text, background) in listOf(colors.onPrimary to colors.primary,
                colors.onSecondary to colors.secondary, colors.onTertiary to colors.tertiary,
                colors.onSurface to colors.surface, colors.onBackground to colors.background,
                colors.onSurface to colors.surfaceContainer, colors.onSurfaceVariant to colors.surfaceVariant,
                colors.onSurfaceVariant to colors.surfaceContainerHigh,
                colors.onPrimaryContainer to colors.primaryContainer,
                colors.onSecondaryContainer to colors.secondaryContainer,
                colors.onTertiaryContainer to colors.tertiaryContainer,
                colors.onError to colors.error, colors.onErrorContainer to colors.errorContainer)) {
                assertTrue("Text contrast ${contrast(text, background)}", contrast(text, background) >= 4.5f)
            }
            assertTrue("Control outline contrast", contrast(colors.outline, colors.surface) >= 3f)
            assertTrue("Primary focus contrast", contrast(colors.primary, colors.background) >= 3f)
        }
    }
}
