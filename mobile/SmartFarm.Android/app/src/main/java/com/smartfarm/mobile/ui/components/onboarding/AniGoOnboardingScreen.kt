package com.smartfarm.mobile.ui.components.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.R
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import com.smartfarm.mobile.ui.theme.AniGoPalette
import kotlinx.coroutines.launch

/**
 * Phase 3 adds a one-shot, skippable carabao walking introduction; Phase 7 handles
 * completion persistence. No network calls, account changes, or farm writes.
 */
private const val PageCount = 4
private val Pine = Color(0xFF145239)
private val Cream = Color(0xFFFBF8F0)

private data class PageCopy(
    val title: String, val description: String, val detail: String, val pose: AniGoCarabaoPose
)

private val copy = listOf(
    PageCopy("Your farm, organized",
        "Manage a farm or join a team. Keep plots and records in one workspace.",
        "Create your farm, or join another farm with its owner's approval.",
        AniGoCarabaoPose.GUIDE),
    PageCopy("Grow with a plan",
        "Plan crops, set planting dates, and see what comes next.",
        "Keep your crop calendar and growing activities organized.",
        AniGoCarabaoPose.PLANT),
    PageCopy("Record work as you go",
        "Log watering, inspections, photos, and supported field activities.",
        "Some Android records can be saved offline and synchronized later. Local save is not server confirmation.",
        AniGoCarabaoPose.RECORD),
    PageCopy("From field to harvest",
        "Record harvests, review supplies, and follow your farm's progress.",
        "Multiple partial harvests can be recorded for a crop, with units kept separate.",
        AniGoCarabaoPose.HARVEST)
)

@Composable
fun AniGoOnboardingScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    motionAllowed: Boolean = anigoMotionAllowed(),
    introAutoPlay: Boolean = true
) {
    // The intro is a one-shot before the four learning pages, not a fifth page.
    // The OS reduced-motion setting can bypass it; completion survives rotation.
    var showIntro by rememberSaveable { mutableStateOf(motionAllowed) }
    if (showIntro) {
        AniGoFarmIntro(
            onFinished = { showIntro = false },
            modifier = modifier,
            motionAllowed = motionAllowed,
            autoPlay = introAutoPlay
        )
        return
    }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val activityResumed = lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    val pager = rememberPagerState(pageCount = { PageCount })
    val scope = rememberCoroutineScope()
    val selectedPage = pager.settledPage
    val changingPage = pager.isScrollInProgress
    val goToPage: (Int) -> Unit = { target ->
        scope.launch {
            if (motionAllowed) pager.animateScrollToPage(target)
            else pager.scrollToPage(target)
        }
    }

    // Fade in smoothly after the skippable introduction. Respect backgrounding.
    val guideEntry = remember { Animatable(if (motionAllowed) 0f else 1f) }
    LaunchedEffect(activityResumed, motionAllowed) {
        if (!motionAllowed) guideEntry.snapTo(1f)
        else if (activityResumed && guideEntry.value < 1f) {
            guideEntry.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = (AniGoGuideMotion.GuideEnterMillis *
                        (1f - guideEntry.value)).toInt().coerceAtLeast(1)
                )
            )
        }
    }

    var exiting by rememberSaveable { mutableStateOf(false) }
    val exitOnce = {
        if (!exiting) {
            exiting = true
            onExit()
        }
    }
    BackHandler(enabled = !exiting) {
        if (!changingPage) {
            if (selectedPage > 0) goToPage(selectedPage - 1) else exitOnce()
        }
    }
    val dark = LocalFarmAppearance.current.dark
    val colors = MaterialTheme.colorScheme
    val background = if (dark) colors.background else Cream
    val foreground = colors.onBackground
    val secondary = colors.onSurfaceVariant

    BoxWithConstraints(
        modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val shortWindow = maxHeight < 500.dp
        Column(
            Modifier.fillMaxSize().graphicsLayer { alpha = guideEntry.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = if (shortWindow) 0.dp else 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        if (dark) R.drawable.ani_go_logo_on_dark else R.drawable.ani_go_logo
                    ),
                    contentDescription = "AniGo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(130.dp).height(if (shortWindow) 28.dp else 38.dp)
                )
                TextButton(
                    onClick = { exitOnce() },
                    enabled = !exiting,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Skip", color = secondary)
                }
            }
            Row(
                Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = if (shortWindow) 2.dp else 7.dp)
                    .semantics {
                        contentDescription = "Step " + (selectedPage + 1) + " of " + PageCount
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(PageCount) { index ->
                    val marker by animateColorAsState(
                        targetValue = if (index <= selectedPage) colors.primary else colors.outlineVariant,
                        animationSpec = tween(
                            durationMillis = if (motionAllowed)
                                AniGoGuideMotion.GuideEnterMillis else 0
                        ),
                        label = "AniGo onboarding step"
                    )
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(marker))
                }
            }
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f)
            ) { page ->
                GuidePageContent(
                    page = page, dark = dark, ink = foreground, secondary = secondary,
                    pageActive = selectedPage == page && !changingPage,
                    activityResumed = activityResumed,
                    motionAllowed = motionAllowed
                )
            }

            Row(
                Modifier.widthIn(max = 560.dp).fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp,
                        top = if (shortWindow) 0.dp else 8.dp, bottom = if (shortWindow) 4.dp else 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (!changingPage && selectedPage > 0) goToPage(selectedPage - 1)
                    },
                    enabled = selectedPage > 0 && !changingPage && !exiting,
                    modifier = Modifier.width(92.dp).heightIn(min = if (shortWindow) 48.dp else 52.dp)
                ) { Text("Back", color = if (selectedPage == 0) secondary else foreground) }
                Button(
                    onClick = {
                        if (!changingPage) {
                            if (selectedPage == PageCount - 1) exitOnce()
                            else goToPage(selectedPage + 1)
                        }
                    },
                    enabled = !changingPage && !exiting,
                    modifier = Modifier.weight(1f).heightIn(min = if (shortWindow) 48.dp else 54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dark) AniGoPalette.darkAction else Pine,
                        contentColor = Color.White)
                ) {
                    Text(if (selectedPage == PageCount - 1) "Continue to AniGo" else "Next →",
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GuidePageContent(
    page: Int,
    dark: Boolean,
    ink: Color,
    secondary: Color,
    pageActive: Boolean,
    activityResumed: Boolean,
    motionAllowed: Boolean
) {
    val details = copy[page]
    var replayCount by remember(page) { mutableIntStateOf(0) }
    var lastHandledReplay by remember(page) { mutableIntStateOf(0) }
    val progress = remember(page) { Animatable(if (motionAllowed) 0f else 1f) }

    // Cancel when off-page, pause/resume with activity lifecycle, and run once
    // per visit. Explicit Replay resets only this page to its first frame.
    LaunchedEffect(pageActive, activityResumed, motionAllowed, replayCount) {
        when {
            !pageActive -> progress.snapTo(if (motionAllowed) 0f else 1f)
            !motionAllowed -> progress.snapTo(1f)
            !activityResumed -> Unit
            else -> {
                if (replayCount != lastHandledReplay) {
                    lastHandledReplay = replayCount
                    progress.snapTo(0f)
                }
                if (progress.value < 1f) {
                    progress.animateTo(
                        1f,
                        animationSpec = tween(
                            durationMillis = (AniGoGuideMotion.DemoMillis *
                                (1f - progress.value)).toInt().coerceAtLeast(1),
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
    }

    val exampleProgress = progress.value
    val gesture = if (pageActive && activityResumed && motionAllowed)
        AniGoGuideMotion.gesture(page, exampleProgress)
        else AniGoGuideMotion.MascotGesture()
    val density = LocalDensity.current
    val gestureLiftPx = with(density) { gesture.liftDp.dp.toPx() }
    val needsStack = density.fontScale >= 1.45f
    val pageScroll = rememberScrollState()
    // A returning user should see this slide's heading, not a scrolled-down card.
    LaunchedEffect(pageActive) {
        if (pageActive) pageScroll.scrollTo(0)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            Modifier.fillMaxWidth().testTag("onboarding-page-$page")
                .verticalScroll(pageScroll).heightIn(min = viewportHeight)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(details.title, color = ink, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(10.dp))
            Text(details.description, color = secondary, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            // Keep the guide and illustrative record together without hiding either.
            // At 320dp or large system text, stack them to avoid clipped cards.
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = needsStack || maxWidth < 295.dp
                val backdrop = Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF20392C), Color(0xFF375340))
                    else listOf(Color(0xFFEFF7EC), Color(0xFFFFF2D7))
                )
                if (compact) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(backdrop)
                    ) {
                        Box(Modifier.fillMaxWidth().height(180.dp)) {
                            FieldBackdrop(dark, Modifier.fillMaxSize())
                            AniGoCarabao(
                                pose = details.pose,
                                frame = gesture.frame,
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .size(180.dp)
                                    .graphicsLayer {
                                        translationY = gestureLiftPx
                                        rotationZ = gesture.tiltDegrees
                                    }
                            )
                        }
                        Box(Modifier.fillMaxWidth().padding(12.dp)) {
                            AniGoGuideExample(page, exampleProgress, viewportHeight < 300.dp)
                        }
                    }
                } else {
                    val shortPage = viewportHeight < 300.dp
                    val characterSize = (maxWidth * 0.52f)
                        .coerceAtMost(if (shortPage) 152.dp else 240.dp)
                    val cardWidth = (maxWidth * 0.48f).coerceIn(136.dp, 240.dp)
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = if (shortPage) 180.dp else 240.dp)
                            .clip(RoundedCornerShape(24.dp)).background(backdrop)
                    ) {
                        FieldBackdrop(dark, Modifier.matchParentSize())
                        AniGoCarabao(
                            pose = details.pose,
                            frame = gesture.frame,
                            modifier = Modifier.align(Alignment.CenterStart)
                                .padding(start = 1.dp, bottom = 4.dp)
                                .size(characterSize)
                                .graphicsLayer {
                                    translationY = gestureLiftPx
                                    rotationZ = gesture.tiltDegrees
                                }
                        )
                        Box(
                            Modifier.align(Alignment.CenterEnd)
                                .padding(end = 10.dp, top = 12.dp, bottom = 12.dp)
                                .width(cardWidth)
                        ) {
                            AniGoGuideExample(page, exampleProgress, shortPage)
                        }
                    }
                }
            }
            Spacer(Modifier.height(17.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(details.detail, color = ink, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(9.dp))
                Text("Illustrative example · no farm records are created",
                    style = MaterialTheme.typography.labelSmall, color = secondary)
                if (pageActive && motionAllowed) {
                    TextButton(
                        onClick = { replayCount++ },
                        modifier = Modifier.align(Alignment.End)
                            .heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = when (page) {
                                    0 -> "Replay farm management example"
                                    1 -> "Replay crop planning example"
                                    2 -> "Replay fieldwork example"
                                    else -> "Replay harvest example"
                                }
                            }
                    ) {
                        Text("Replay example", color = ink,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AniGoGuideExample(page: Int, progress: Float, compact: Boolean) {
    when (page) {
        0 -> AniGoFarmManagementDemo(progress)
        1 -> AniGoCropPlanningDemo(progress)
        2 -> AniGoFieldworkDemo(progress, compact = compact)
        else -> AniGoHarvestDemo(progress)
    }
}

@Composable
private fun FieldBackdrop(dark: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Color.White.copy(alpha = if (dark) .08f else .50f), h * .24f,
            Offset(w * .18f, h * .20f))
        drawPath(Path().apply {
            moveTo(0f, h * .78f)
            cubicTo(w * .25f, h * .48f, w * .62f, h * .78f, w, h * .60f)
            lineTo(w, h); lineTo(0f, h); close()
        }, if (dark) Color(0xFF547348) else Color(0xFFB3D493))
        drawPath(Path().apply {
            moveTo(0f, h * .86f)
            cubicTo(w * .35f, h * .70f, w * .70f, h * .87f, w, h * .70f)
            lineTo(w, h); lineTo(0f, h); close()
        }, if (dark) Color(0xFF3C643E) else Color(0xFF74A865))
        drawPath(Path().apply {
            moveTo(w * .25f, h)
            cubicTo(w * .50f, h * .85f, w * .70f, h * .85f, w, h * .79f)
            lineTo(w, h); close()
        }, if (dark) Color(0xFF9A8947) else Color(0xFFE5C06A))
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingPreview() {
    SmartFarmTheme { AniGoOnboardingScreen(onExit = {}) }
}
