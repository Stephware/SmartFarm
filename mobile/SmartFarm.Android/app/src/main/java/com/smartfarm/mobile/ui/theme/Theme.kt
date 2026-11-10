package com.smartfarm.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// AniGo design system: greenery is primary, soil brown is secondary.
// Color pairs keep >= 4.5:1 contrast for normal text in light and dark themes.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF2D6C45), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEDE0), onPrimaryContainer = Color(0xFF173B28),
    secondary = Color(0xFF8B5E3C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E3D5), onSecondaryContainer = Color(0xFF48311F),
    tertiary = Color(0xFF766018), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4E6B8), onTertiaryContainer = Color(0xFF342900),
    background = Color(0xFFF8F6F1), onBackground = Color(0xFF26352D),
    surface = Color.White, onSurface = Color(0xFF26352D),
    surfaceVariant = Color(0xFFEEE9DF), onSurfaceVariant = Color(0xFF48544B),
    surfaceTint = Color(0xFF2D6C45), surfaceDim = Color(0xFFE3E4DC),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBFAF6),
    surfaceContainer = Color(0xFFF5F2EC), surfaceContainerHigh = Color(0xFFEFECE5),
    surfaceContainerHighest = Color(0xFFE9E6DE),
    outline = Color(0xFF6B746B), outlineVariant = Color(0xFFC4CBC1),
    error = Color(0xFFA10B22), onError = Color.White,
    errorContainer = Color(0xFFFFF0F1), onErrorContainer = Color(0xFF730F1E)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA9DFB8), onPrimary = Color(0xFF113B24),
    primaryContainer = Color(0xFF234E35), onPrimaryContainer = Color(0xFFD9F2DE),
    secondary = Color(0xFFE0B996), onSecondary = Color(0xFF432B1D),
    secondaryContainer = Color(0xFF63452E), onSecondaryContainer = Color(0xFFF8E5D3),
    tertiary = Color(0xFFE4CE88), onTertiary = Color(0xFF3E3008),
    tertiaryContainer = Color(0xFF554717), onTertiaryContainer = Color(0xFFF5E7B7),
    background = Color(0xFF141E18), onBackground = Color(0xFFE6EFE8),
    surface = Color(0xFF1D2921), onSurface = Color(0xFFE6EFE8),
    surfaceVariant = Color(0xFF344137), onSurfaceVariant = Color(0xFFD8E2D6),
    surfaceTint = Color(0xFFA9DFB8), surfaceDim = Color(0xFF141E18),
    surfaceBright = Color(0xFF344137),
    surfaceContainerLowest = Color(0xFF111913), surfaceContainerLow = Color(0xFF18231B),
    surfaceContainer = Color(0xFF202F25), surfaceContainerHigh = Color(0xFF2A3A2F),
    surfaceContainerHighest = Color(0xFF36483A),
    outline = Color(0xFFA3B0A5), outlineVariant = Color(0xFF526256),
    error = Color(0xFFFFB3B5), onError = Color(0xFF680016),
    errorContainer = Color(0xFF8F0025), onErrorContainer = Color(0xFFFFDADE)
)

internal val FarmTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
)

internal val FarmShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SmartFarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = FarmTypography,
        shapes = FarmShapes,
        content = content
    )
}
