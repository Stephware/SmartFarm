package com.smartfarm.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val LightColors = lightColorScheme(
    primary = Color(0xFF1D6B3D), onPrimary = Color.White,
    primaryContainer = Color(0xFFAFF2BD), onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF466952), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E8D8), onSecondaryContainer = Color(0xFF0E2A17),
    tertiary = Color(0xFF735C00), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE18A), onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFF3F8F3), onBackground = Color(0xFF163224),
    surface = Color(0xFFFBFDF8), onSurface = Color(0xFF163224),
    surfaceVariant = Color(0xFFE0E9DF), onSurfaceVariant = Color(0xFF405244),
    surfaceTint = Color(0xFF1D6B3D), surfaceDim = Color(0xFFD2DDD3), surfaceBright = Color(0xFFFBFDF8),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF4F8F2),
    surfaceContainer = Color(0xFFEDF3EC), surfaceContainerHigh = Color(0xFFE7EEE5), surfaceContainerHighest = Color(0xFFE1E9DF),
    outline = Color(0xFF6B7D6E), outlineVariant = Color(0xFFBECABE),
    error = Color(0xFFA10B22), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)
internal val DarkColors = darkColorScheme(
    primary = Color(0xFF91D5A3), onPrimary = Color(0xFF00391B),
    primaryContainer = Color(0xFF00522A), onPrimaryContainer = Color(0xFFAFF2BD),
    secondary = Color(0xFFB5CCB9), onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF374C3D), onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFE5C349), onTertiary = Color(0xFF3B2F00),
    tertiaryContainer = Color(0xFF544600), onTertiaryContainer = Color(0xFFFFE18A),
    background = Color(0xFF101D15), onBackground = Color(0xFFE0EDE0),
    surface = Color(0xFF101D15), onSurface = Color(0xFFE0EDE0),
    surfaceVariant = Color(0xFF3E4B41), onSurfaceVariant = Color(0xFFBECEC0),
    surfaceTint = Color(0xFF91D5A3), surfaceDim = Color(0xFF101D15), surfaceBright = Color(0xFF34463A),
    surfaceContainerLowest = Color(0xFF0C1810), surfaceContainerLow = Color(0xFF142219),
    surfaceContainer = Color(0xFF18281E), surfaceContainerHigh = Color(0xFF233329), surfaceContainerHighest = Color(0xFF2D3D32),
    outline = Color(0xFF8A9B8D), outlineVariant = Color(0xFF3E4B41),
    error = Color(0xFFFFB3B5), onError = Color(0xFF680016),
    errorContainer = Color(0xFF8F0025), onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun SmartFarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
