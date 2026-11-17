package com.smartfarm.mobile.ui.components.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.R
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import kotlinx.coroutines.launch

/**
 * Phase 2 static preview. Phase 3 adds intro/pose animation; Phase 7 handles
 * completion persistence. No network calls, account changes, or farm writes.
 */
private const val PageCount = 4
private val Pine = Color(0xFF145239)
private val Ink = Color(0xFF0B3928)
private val Gold = Color(0xFFCBA14E)
private val Cream = Color(0xFFFBF8F0)
private val Muted = Color(0xFF61756A)

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
fun AniGoOnboardingScreen(onExit: () -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { PageCount })
    val scope = rememberCoroutineScope()
    val dark = LocalFarmAppearance.current.dark
    val background = if (dark) Color(0xFF101F18) else Cream
    val foreground = if (dark) Color(0xFFECF4EB) else Ink
    val secondary = if (dark) Color(0xFFC7D4CA) else Muted

    Column(
        modifier.fillMaxSize().background(background).windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(
                    if (dark) R.drawable.ani_go_logo_on_dark else R.drawable.ani_go_logo
                ),
                contentDescription = "AniGo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(130.dp).height(38.dp)
            )
            TextButton(onClick = onExit, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Skip", color = secondary)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 7.dp)
                .semantics {
                    contentDescription = "Step " + (pager.currentPage + 1) + " of " + PageCount
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(PageCount) { index ->
                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(
                    if (index <= pager.currentPage) Pine
                    else if (dark) Color(0xFF3A5244) else Color(0xFFE4E5DA)
                ))
            }
        }
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { page -> GuidePageContent(page, dark, foreground, secondary) }

        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0,
                modifier = Modifier.width(92.dp).heightIn(min = 52.dp)
            ) { Text("Back", color = if (pager.currentPage == 0) secondary else foreground) }
            Button(
                onClick = {
                    if (pager.currentPage == PageCount - 1) onExit()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.weight(1f).heightIn(min = 54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pine, contentColor = Color.White)
            ) {
                Text(if (pager.currentPage == PageCount - 1) "Continue to AniGo" else "Next →",
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GuidePageContent(page: Int, dark: Boolean, ink: Color, secondary: Color) {
    val details = copy[page]
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(details.title, color = ink, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        Text(details.description, color = secondary, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(24.dp)).background(
                Brush.verticalGradient(if (dark)
                    listOf(Color(0xFF20392C), Color(0xFF375340))
                    else listOf(Color(0xFFEFF7EC), Color(0xFFFFF2D7)))
            )
        ) {
            FieldBackdrop(dark, Modifier.fillMaxSize())
            AniGoCarabao(details.pose, modifier = Modifier.align(Alignment.BottomStart)
                .padding(start = 1.dp, bottom = 4.dp).size(203.dp))
            Box(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, bottom = 10.dp)
                    .width(151.dp)
            ) {
                when (page) {
                    0 -> DemoCard("Riverside Farm", "Farm workspace",
                        listOf("✓ North Plot", "✓ South Plot", "Team & roles"))
                    1 -> DemoCard("Rice planting", "Sample crop plan",
                        listOf("✓ Plan • Plant", "○ Grow • Harvest", "Crop calendar"))
                    2 -> DemoCard("Today's work", "Sample fieldwork",
                        listOf("✓ Water crops", "✓ Inspect leaves", "○ Take a photo"))
                    else -> DemoCard("Harvest records", "Sample crop history",
                        listOf("85 kg harvested", "✓ Batch 1 · 50 kg", "✓ Batch 2 · 35 kg"))
                }
            }
        }
        Spacer(Modifier.height(17.dp))
        Surface(shape = RoundedCornerShape(16.dp),
            color = if (dark) Color(0xFF21372A) else Color.White,
            tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp)) {
                Text("HOW ANIGO HELPS", color = Gold,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text(details.detail, color = ink, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(9.dp))
                Text("Illustrative example · no farm records are created",
                    style = MaterialTheme.typography.labelSmall, color = secondary)
            }
        }
    }
}

@Composable
private fun DemoCard(title: String, subtitle: String, values: List<String>) {
    Surface(
        color = Color(0xFFFFFEFC), shadowElevation = 5.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFDFE4D9), RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("EXAMPLE", color = Color(0xFF8F6827),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(title, color = Ink, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.labelSmall)
            values.forEach { value ->
                Text(value, color = if (value.startsWith("✓")) Pine else Ink,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium)
            }
        }
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
