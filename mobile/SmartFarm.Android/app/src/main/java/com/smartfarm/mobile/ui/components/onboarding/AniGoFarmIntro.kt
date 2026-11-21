package com.smartfarm.mobile.ui.components.onboarding

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import kotlin.math.PI
import kotlin.math.sin

/** One-shot intro for first-run guidance and explicit replays.
 * Skip and the system Back key both reveal the four-page guide.
 * All scenery is local Canvas work; no video, network dependency or audio.
 */
private val Pine = Color(0xFF145239)
private val Gold = Color(0xFFCBA14E)
private val Cream = Color(0xFFFBF8F0)

@Composable
internal fun anigoMotionAllowed(): Boolean {
    val context = LocalContext.current
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // Honor the OS "remove animations" / animator-duration-scale setting.
    // A missing or unreadable setting defaults to enabled.
    return remember(context, lifecycleState) {
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
    autoPlay: Boolean = true,
    previewProgress: Float = 0f,
    onBack: () -> Unit = onFinished
) {
    val latestFinish by rememberUpdatedState(onFinished)
    val latestBack by rememberUpdatedState(onBack)
    // Static review frames use the exact runtime illustration and UI layers.
    // Real entry paths keep the 0f default and play the normal intro.
    val progress = remember { Animatable(previewProgress.coerceIn(0f, 1f)) }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val activityResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val dark = LocalFarmAppearance.current.dark
    val ink = MaterialTheme.colorScheme.onBackground
    val bg = if (dark) MaterialTheme.colorScheme.background else Cream

    LaunchedEffect(motionAllowed, autoPlay, activityResumed) {
        if (!motionAllowed) {
            latestFinish()
            return@LaunchedEffect
        }
        if (!autoPlay || !activityResumed) return@LaunchedEffect

        // Never advance a partially completed intro while the activity is
        // backgrounded. Return at the remaining duration, not from frame 1.
        if (progress.value < 1f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (AniGoGuideMotion.IntroMillis *
                        (1f - progress.value)).toInt().coerceAtLeast(1),
                    easing = LinearEasing
                )
            )
        }
        latestFinish()
    }

    val value = progress.value
    val walkLinear = ((value - 0.04f) / 0.66f).coerceIn(0f, 1f)
    val arrival = FastOutSlowInEasing.transform(walkLinear)
    val walking = walkLinear < 1f
    // Keep the FULL-BODY carabao throughout its entrance. Switching from full
    // walking art to a talking bust before the scene transition caused a jump.
    // The open/closed-mouth bust animation belongs on the following Welcome slide.
    val pose = AniGoCarabaoPose.WAVE
    val walkFrame = ((value * AniGoGuideMotion.IntroMillis / 145f).toInt()).coerceAtLeast(0) % 4
    val frame = if (walking) walkFrame else 3
    val logoOpacity = ((value - 0.34f) / 0.24f).coerceIn(0f, 1f)
    val titleOpacity = ((value - 0.62f) / 0.22f).coerceIn(0f, 1f)
    val mascotBounce = if (walking) sin(value * 2f * PI.toFloat() * 12f) * 5f else 0f
    val density = LocalDensity.current

    BackHandler { latestBack() }

    BoxWithConstraints(
        modifier.fillMaxSize()
            .background(bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val small = maxHeight < 620.dp || maxWidth < 340.dp
        val compactLandscape = maxWidth > maxHeight && maxHeight < 520.dp
        val stacked = !compactLandscape && (maxHeight < 520.dp || density.fontScale >= 1.45f)
        val characterSize = if (small) 190.dp else 250.dp
        val stepDistance = with(density) { (maxWidth * 0.85f).toPx() }
        val yBouncePx = with(density) { mascotBounce.dp.toPx() }

        AniGoIntroLandscape(dark, Modifier.fillMaxSize())

        if (compactLandscape) {
            // Short landscape screens need an explicit two-column composition.
            // The old vertical stack put the carabao below the visible viewport.
            val landscapeMascotSize = if (maxWidth < 550.dp) 124.dp else 168.dp
            Row(
                Modifier.align(Alignment.Center)
                    .widthIn(max = 680.dp).fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 38.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    Modifier.weight(1f)
                        .background(bg.copy(alpha = .96f), RoundedCornerShape(18.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (dark) R.drawable.ani_go_logo_on_dark else R.drawable.ani_go_logo
                        ),
                        contentDescription = uiText(R.string.ui_anigo_80e6f53, "AniGo"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 165.dp, height = 48.dp)
                            .graphicsLayer { alpha = logoOpacity }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        uiText(R.string.ui_a_better_way_to_care_for_your_farm_19a81cd, "A better way to care for your farm."),
                        color = ink,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.graphicsLayer { alpha = titleOpacity }
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        uiText(R.string.ui_come_along_i_ll_show_you_around_4826b18, "Come along. I'll show you around."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.graphicsLayer { alpha = titleOpacity }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        uiText(R.string.ui_your_farm_your_crops_your_progress_f110a64, "Your farm, your crops, your progress"),
                        color = ink,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.graphicsLayer { alpha = titleOpacity }
                    )
                }
                AniGoCarabao(
                    pose = pose,
                    frame = frame,
                    walking = walking,
                    modifier = Modifier.size(landscapeMascotSize)
                        .testTag("anigo-intro-landscape-mascot")
                        .graphicsLayer {
                            translationX = -stepDistance * (1f - arrival)
                            translationY = yBouncePx
                            rotationZ = if (walking) mascotBounce * 0.45f else 0f
                        }
                )
            }
        } else {
        Column(
            Modifier.align(Alignment.TopCenter)
                .widthIn(max = 560.dp).fillMaxWidth()
                .background(if (stacked) bg else Color.Transparent)
                .verticalScroll(rememberScrollState())
                .padding(top = if (small || stacked) 64.dp else 94.dp,
                    start = 22.dp, end = 22.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.ani_go_logo_on_dark else R.drawable.ani_go_logo
                ),
                contentDescription = uiText(R.string.ui_anigo_80e6f53, "AniGo"),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 210.dp, height = 64.dp)
                    .graphicsLayer { alpha = logoOpacity }
            )
            Spacer(Modifier.height(14.dp))
            Text(
                uiText(R.string.ui_a_better_way_to_care_for_your_farm_19a81cd, "A better way to care for your farm."),
                color = ink,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.graphicsLayer { alpha = titleOpacity }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                uiText(R.string.ui_come_along_i_ll_show_you_around_4826b18, "Come along. I'll show you around."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.graphicsLayer { alpha = titleOpacity }
            )
            if (stacked) {
                Spacer(Modifier.height(16.dp))
                AniGoCarabao(pose, Modifier.size(180.dp), frame, walking = walking)
                Text(uiText(R.string.ui_your_farm_your_crops_your_progress_f110a64, "Your farm, your crops, your progress"),
                    color = ink, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
            }
        }

        if (!stacked) {
        AniGoCarabao(
            pose = pose,
            frame = frame,
            walking = walking,
            modifier = Modifier.align(Alignment.Center)
                .offset(y = if (small) 48.dp else 72.dp)
                .size(characterSize)
                .graphicsLayer {
                    translationX = -stepDistance * (1f - arrival)
                    translationY = yBouncePx
                    rotationZ = if (walking) mascotBounce * 0.45f else 0f
                }
        )

        Text(
            uiText(R.string.ui_your_farm_your_crops_your_progress_f110a64, "Your farm, your crops, your progress"),
            style = MaterialTheme.typography.bodyMedium,
            color = ink,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                .graphicsLayer { alpha = titleOpacity }
                .background(bg.copy(alpha = .94f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        }

        }

        TextButton(
            onClick = { latestFinish() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 16.dp)
        ) {
            Text(uiText(R.string.ui_skip_intro_5ee6bb6, "Skip intro"), color = ink, fontWeight = FontWeight.SemiBold)
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
                if (dark) listOf(Color(0xFF121917), Color(0xFF202B26), Color(0xFF354334))
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
