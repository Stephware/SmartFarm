package com.smartfarm.mobile.ui.components.onboarding

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.R
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** One-shot intro shown only when the user explicitly opens the guide preview.
 *
 * In Phase 3 this is not shown on every app launch or saved to preferences.
 * Skip and the system Back key both reveal the existing four-page guide.
 * All scenery is local Canvas work; no video, network dependency or audio.
 */
private const val INTRO_DURATION_MS = 2550
private val Pine = Color(0xFF145239)
private val Gold = Color(0xFFCBA14E)
private val Cream = Color(0xFFFBF8F0)

@Composable
internal fun anigoMotionAllowed(): Boolean {
    val context = LocalContext.current
    // Honor the OS "remove animations" / animator-duration-scale setting.
    // A missing or unreadable setting defaults to enabled.
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
}

@Composable
fun AniGoFarmIntro(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    motionAllowed: Boolean = anigoMotionAllowed(),
    autoPlay: Boolean = true
) {
    val latestFinish by rememberUpdatedState(onFinished)
    val progress = remember { Animatable(0f) }
    val dark = LocalFarmAppearance.current.dark
    val ink = if (dark) Color(0xFFF2F4EC) else Color(0xFF103D2B)
    val bg = if (dark) Color(0xFF14251B) else Cream

    LaunchedEffect(motionAllowed, autoPlay) {
        if (!motionAllowed) {
            latestFinish()
            return@LaunchedEffect
        }
        if (!autoPlay) return@LaunchedEffect // Hold the first frame for deterministic UI review/tests.
        // Deliberately bounded: finishes once, never restarts or loops on this route.
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(INTRO_DURATION_MS, easing = LinearEasing)
        )
        latestFinish()
    }

    val value = progress.value
    val walkLinear = ((value - 0.04f) / 0.66f).coerceIn(0f, 1f)
    val arrival = FastOutSlowInEasing.transform(walkLinear)
    val walking = walkLinear < 1f
    val pose = if (value > 0.74f) AniGoCarabaoPose.WAVE else AniGoCarabaoPose.GUIDE
    val frame = ((value * INTRO_DURATION_MS / 145f).toInt()).coerceAtLeast(0) % 4
    val logoOpacity = ((value - 0.34f) / 0.24f).coerceIn(0f, 1f)
    val titleOpacity = ((value - 0.62f) / 0.22f).coerceIn(0f, 1f)
    val mascotBounce = if (walking) sin(value * 2f * PI.toFloat() * 12f) * 5f else 0f
    val density = LocalDensity.current

    BackHandler { latestFinish() }

    BoxWithConstraints(
        modifier.fillMaxSize()
            .background(bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val small = maxHeight < 620.dp
        val characterSize = if (small) 190.dp else 250.dp
        val stepDistance = with(density) { (maxWidth * 0.85f).toPx() }
        val yBouncePx = with(density) { mascotBounce.dp.toPx() }

        AniGoIntroLandscape(dark, Modifier.fillMaxSize())

        Column(
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = if (small) 64.dp else 94.dp, start = 22.dp, end = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.ani_go_logo_on_dark else R.drawable.ani_go_logo
                ),
                contentDescription = "AniGo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 210.dp, height = 64.dp)
                    .graphicsLayer { alpha = logoOpacity }
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "A better way to care for your farm.",
                color = ink,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.graphicsLayer { alpha = titleOpacity }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Come along. I'll show you around.",
                color = if (dark) Color(0xFFE4D7BC) else Pine,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.graphicsLayer { alpha = titleOpacity }
            )
        }

        AniGoCarabao(
            pose = pose,
            frame = frame,
            walking = walking,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = if (small) 64.dp else 91.dp)
                .size(characterSize)
                .graphicsLayer {
                    translationX = -stepDistance * (1f - arrival)
                    translationY = yBouncePx
                    rotationZ = if (walking) mascotBounce * 0.45f else 0f
                }
        )

        Text(
            "Your farm, your crops, your progress",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) Color(0xFFF3E9CE) else Pine,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                .graphicsLayer { alpha = titleOpacity }
        )

        TextButton(
            onClick = { latestFinish() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp)
        ) {
            Text("Skip intro", color = ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AniGoIntroLandscape(dark: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.verticalGradient(
                if (dark) listOf(Color(0xFF14251B), Color(0xFF244A31), Color(0xFF50603A))
                else listOf(Color(0xFFFAF9F2), Color(0xFFE9F5EC), Color(0xFFE8E6C6))
            )
        )

        val horizon = h * 0.64f
        drawCircle(
            if (dark) Color(0x3375A086) else Color(0x99FFF7D5),
            radius = w * 0.20f,
            center = Offset(w * .78f, h * .15f)
        )
        // Receding hills and two clearly visible layers of farm plots.
        drawPath(Path().apply {
            moveTo(0f, horizon)
            cubicTo(w * .26f, horizon - h * .15f, w * .47f, horizon + h * .03f,
                w * .75f, horizon - h * .10f)
            quadraticTo(w * .88f, horizon - h * .15f, w, horizon - h * .09f)
            lineTo(w, h); lineTo(0f, h); close()
        }, if (dark) Color(0xFF4A6B47) else Color(0xFFB4D49F))
        drawPath(Path().apply {
            moveTo(0f, horizon + h * .12f)
            cubicTo(w * .30f, horizon - h * .02f, w * .64f, horizon + h * .22f, w, horizon + h * .03f)
            lineTo(w, h); lineTo(0f, h); close()
        }, if (dark) Color(0xFF34603C) else Color(0xFF79B26C))

        drawPath(Path().apply {
            moveTo(w * .37f, h)
            cubicTo(w * .46f, h * .86f, w * .68f, h * .80f, w, h * .82f)
            lineTo(w, h); close()
        }, if (dark) Color(0xFFAE9351) else Color(0xFFE2BE67))
        val furrow = if (dark) Color(0xFF73925A) else Color(0xFF5F9F59)
        for (i in 0..3) {
            val start = w * (0.04f + i * 0.21f)
            val end = w * (0.59f + i * 0.13f)
            drawPath(Path().apply {
                moveTo(start, h)
                cubicTo(start + w * .02f, h * .90f, end - w * .05f, h * .86f, end, h * .84f)
            }, furrow, style = Stroke(width = 2.5.dp.toPx()))
        }
        // Small farmhouse in the distance; proportional geometry at any screen width.
        val houseX = w * .76f
        val houseY = horizon - h * .03f
        drawRect(if (dark) Color(0xFFE4D09A) else Color(0xFFF4E7BF),
            topLeft = Offset(houseX, houseY), size = androidx.compose.ui.geometry.Size(w * .14f, h * .07f))
        drawPath(Path().apply {
            moveTo(houseX - w * .018f, houseY)
            lineTo(houseX + w * .07f, houseY - h * .055f)
            lineTo(houseX + w * .158f, houseY)
            close()
        }, if (dark) Gold else Color(0xFFC48E4B))
        drawRect(if (dark) Pine else Color(0xFF517C50),
            topLeft = Offset(houseX + w * .056f, houseY + h * .026f),
            size = androidx.compose.ui.geometry.Size(w * .032f, h * .044f))
    }
}
