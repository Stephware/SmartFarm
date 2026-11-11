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

// Harvest Gold + Pine visual system. Keep gold on large surfaces or with a dark-ink label.
internal object AniGoPalette {
    val pine = Color(0xFF153D2D)
    val pineMid = Color(0xFF28503C)
    val harvestGold = Color(0xFFB58A32)
    val goldInk = Color(0xFF80601D)
    val goldSoft = Color(0xFFFFF2D8)
    val cream = Color(0xFFF7F8F4)
}

internal val LightColors = lightColorScheme(
    primary = AniGoPalette.pine, onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1E8), onPrimaryContainer = Color(0xFF153D2D),
    secondary = AniGoPalette.goldInk, onSecondary = Color.White,
    secondaryContainer = AniGoPalette.goldSoft, onSecondaryContainer = Color(0xFF574016),
    tertiary = Color(0xFF80601D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8BC), onTertiaryContainer = Color(0xFF574016),
    background = AniGoPalette.cream, onBackground = Color(0xFF1B3428),
    surface = Color.White, onSurface = Color(0xFF1B3428),
    surfaceVariant = Color(0xFFEEF0EA), onSurfaceVariant = Color(0xFF506156),
    surfaceTint = AniGoPalette.pine, surfaceDim = Color(0xFFE1E7DF),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFBF8),
    surfaceContainer = Color(0xFFF3F6F0), surfaceContainerHigh = Color(0xFFEBF0E9),
    surfaceContainerHighest = Color(0xFFE5ECE3),
    outline = Color(0xFF728076), outlineVariant = Color(0xFFC5D3C8),
    error = Color(0xFFA10B22), onError = Color.White,
    errorContainer = Color(0xFFFFF0F1), onErrorContainer = Color(0xFF730F1E)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFC6E6CF), onPrimary = AniGoPalette.pine,
    primaryContainer = Color(0xFF244C35), onPrimaryContainer = Color(0xFFDDF4E2),
    secondary = Color(0xFFF4D28B), onSecondary = Color(0xFF3F2D0B),
    secondaryContainer = Color(0xFF493B1C), onSecondaryContainer = Color(0xFFFCE4AC),
    tertiary = Color(0xFFF1BE6B), onTertiary = Color(0xFF34280B),
    tertiaryContainer = Color(0xFF51421F), onTertiaryContainer = Color(0xFFFFF0C9),
    background = Color(0xFF111D16), onBackground = Color(0xFFF2F5ED),
    surface = Color(0xFF1D2C22), onSurface = Color(0xFFF2F5ED),
    surfaceVariant = Color(0xFF34463A), onSurfaceVariant = Color(0xFFC1D0C2),
    surfaceTint = Color(0xFFC6E6CF), surfaceDim = Color(0xFF111D16),
    surfaceBright = Color(0xFF34483A),
    surfaceContainerLowest = Color(0xFF111913), surfaceContainerLow = Color(0xFF19271D),
    surfaceContainer = Color(0xFF26352C), surfaceContainerHigh = Color(0xFF2D4032),
    surfaceContainerHighest = Color(0xFF364A3B),
    outline = Color(0xFF94A79B), outlineVariant = Color(0xFF526457),
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
