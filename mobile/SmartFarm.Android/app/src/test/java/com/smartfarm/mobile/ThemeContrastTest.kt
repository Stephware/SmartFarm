package com.smartfarm.mobile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.smartfarm.mobile.ui.theme.LightColors
import com.smartfarm.mobile.ui.theme.DarkColors
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeContrastTest {
    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance(); val b = second.luminance()
        return (max(a, b) + .05f) / (min(a, b) + .05f)
    }

    @Test fun textAndControlsRemainReadableInBothThemes() {
        for (colors in listOf(LightColors, DarkColors)) {
            for ((text, background) in listOf(colors.onPrimary to colors.primary,
                colors.onSecondary to colors.secondary, colors.onTertiary to colors.tertiary,
                colors.onSurface to colors.surface, colors.onSurfaceVariant to colors.surfaceVariant,
                colors.onPrimaryContainer to colors.primaryContainer,
                colors.onSecondaryContainer to colors.secondaryContainer,
                colors.onTertiaryContainer to colors.tertiaryContainer,
                colors.onError to colors.error, colors.onErrorContainer to colors.errorContainer)) {
                assertTrue("Text contrast ${contrast(text, background)}", contrast(text, background) >= 4.5f)
            }
            assertTrue("Control outline contrast", contrast(colors.outline, colors.surface) >= 3f)
        }
    }
}
