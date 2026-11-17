package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartfarm.mobile.R
import com.smartfarm.mobile.ui.theme.AniGoPalette
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import com.smartfarm.mobile.ui.theme.LocalPhotoAuthBars

/**
 * Shared Option 1 photo-led authentication surface. Uses AniGo's existing, licensed farm
 * photograph (also used by the web landing); no generated or remotely loaded artwork.
 * Scrollable with IME/safe insets so forms stay reachable on small devices and large text.
 */
@Composable
fun PhotoAuthScaffold(
    headline: String,
    description: String,
    formTitle: String,
    formSubtitle: String,
    modifier: Modifier = Modifier,
    compactHero: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val photoBars = LocalPhotoAuthBars.current
    // Full-bleed image is dark underneath the system bars, in both app themes.
    DisposableEffect(photoBars) {
        photoBars(true)
        onDispose { photoBars(false) }
    }

    val ink = MaterialTheme.colorScheme.onSurface
    val panel = MaterialTheme.colorScheme.surface
    Box(modifier.fillMaxSize().background(Color(0xFF122C20))) {
        Image(
            painter = painterResource(R.drawable.ani6_fields_hero),
            contentDescription = null, // Decorative brand photography, not an actual farm record.
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
            modifier = Modifier.matchParentSize()
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xC80C251C), Color(0x8D142E23), Color(0xD60A2119))
                )
            )
        )
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // The photo is already darkened: use the official cream/gold
                    // full wordmark, never a hand-drawn substitute or typed duplicate.
                    Image(
                        painter = painterResource(R.drawable.ani_go_logo_on_dark),
                        contentDescription = "AniGo",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.CenterStart,
                        modifier = Modifier.widthIn(max = 164.dp).height(48.dp)
                    )
                    Spacer(Modifier.height(if (compactHero) 32.dp else 82.dp))
                    Text(
                        "YOUR FARM, ORGANIZED",
                        color = Color(0xFFF3CD80),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        headline,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        description,
                        color = Color(0xFFF4F7F2),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(20.dp))
                }
                Surface(
                    color = panel,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 380.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            formTitle,
                            color = ink,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            formSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(2.dp))
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoAuthPrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val dark = LocalFarmAppearance.current.dark
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (dark) AniGoPalette.darkAction else AniGoPalette.pine,
            contentColor = Color.White
        )
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}
