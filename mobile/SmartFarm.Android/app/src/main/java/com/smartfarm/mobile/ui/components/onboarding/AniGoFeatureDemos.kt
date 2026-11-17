package com.smartfarm.mobile.ui.components.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Phase 4–5 teaching cards. This is a standalone presentation layer: sample labels,
 * deterministic progress, no ViewModel/API/Room writes or any real farm state.
 * The caller advances [progress] once per active page and may reset it to replay.
 */
private val Pine = Color(0xFF145239)
private val Gold = Color(0xFFCBA14E)
private val Ink = Color(0xFF173D2B)
private val Muted = Color(0xFF66756A)
private val Leaf = Color(0xFF4B9C54)
private val Border = Color(0xFFE3E9DF)

internal fun reveal(progress: Float, start: Float): Float =
    ((progress - start) / 0.13f).coerceIn(0f, 1f)

@Composable
internal fun ExampleCard(
    title: String,
    subtitle: String,
    label: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = Color(0xFFFFFEFC),
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Border, RoundedCornerShape(16.dp))
    ) {
        Column(
            Modifier.padding(horizontal = 11.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(label.uppercase(), color = Color(0xFF946A27),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(title, color = Ink, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.labelSmall)
            content()
        }
    }
}

@Composable
internal fun IntroRow(
    title: String,
    subtext: String,
    color: Color = Pine,
    opacity: Float = 1f
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .graphicsLayer {
                alpha = opacity
                translationY = (1f - opacity) * 10f
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier.size(19.dp).clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = .16f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = color, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold)
            Text(subtext, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Farm created, then two plots appear, then the team relationship is explained. */
@Composable
fun AniGoFarmManagementDemo(progress: Float, modifier: Modifier = Modifier) {
    val phase = when {
        progress >= .84f -> 4
        progress >= .57f -> 3
        progress >= .28f -> 2
        else -> 1
    }
    Box(modifier.semantics {
        contentDescription = "Sample farm setup, step $phase of 4; demonstration only"
    }) {
        ExampleCard("Riverside Farm", "Sample farm workspace", "Farm added") {
            if (progress >= .18f) {
                IntroRow("North Plot", "Crop area",
                    opacity = reveal(progress, .18f))
            }
            if (progress >= .46f) {
                IntroRow("South Plot", "Crop area",
                    opacity = reveal(progress, .46f))
            }
            if (progress >= .73f) {
                Spacer(Modifier.height(2.dp))
                Text("Owner & team members", color = Pine,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer {
                        alpha = reveal(progress, .73f)
                    })
            }
        }
    }
}

/** Animated farm growth is drawn inside the instructional sample card, not
 * actual crop data. Only the illustration and calendar milestones change.
 */
@Composable
fun AniGoCropPlanningDemo(progress: Float, modifier: Modifier = Modifier) {
    val phase = when {
        progress >= .82f -> 4
        progress >= .53f -> 3
        progress >= .25f -> 2
        else -> 1
    }
    Box(modifier.semantics {
        contentDescription = "Sample crop plan, step $phase of 4; demonstration only"
    }) {
        ExampleCard("Rice planting", "Sample crop calendar", "Crop plan") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeedlingGrowth(
                    growth = ((progress - .23f) / .52f).coerceIn(0f, 1f),
                    modifier = Modifier.width(64.dp).height(51.dp)
                )
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (progress >= .74f) "Growing" else if (progress >= .39f) "Planted"
                            else "Planned",
                        color = Pine,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text("Rice", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf("Plan", "Plant", "Grow").forEachIndexed { index, title ->
                    val active = progress >= when (index) {
                        0 -> .02f
                        1 -> .39f
                        else -> .73f
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height(5.dp).clip(CircleShape)
                                .background(if (active) Gold else Border)
                        )
                        Text(title, color = if (active) Ink else Muted,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (progress >= .76f) {
                Text("Upcoming: crop activities", color = Pine,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.graphicsLayer {
                        alpha = reveal(progress, .76f)
                    })
            }
        }
    }
}

/** Tiny seedling growth on the instructional card, with a fixed view box and
 * no bitmap/GIF decoding. Pausing or disabling animation shows the mature frame.
 */
@Composable
private fun SeedlingGrowth(growth: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val earthY = h * .86f
        drawOval(Color(0xFFE3C89A), Offset(w * .05f, earthY),
            Size(w * .90f, h * .12f))
        drawOval(Color(0xFFB88E5E), Offset(w * .17f, earthY + h * .02f),
            Size(w * .68f, h * .07f))
        val stemTop = earthY - h * (.10f + .62f * growth)
        val center = w * .50f
        drawLine(Color(0xFF327E45),
            start = Offset(center, earthY),
            end = Offset(center, stemTop),
            strokeWidth = w * .052f
        )
        if (growth > .14f) {
            val leafScale = ((growth - .14f) / .50f).coerceIn(0f, 1f)
            drawPath(Path().apply {
                moveTo(center, earthY - h * .30f * growth)
                quadraticTo(center - w * .42f * leafScale, earthY - h * .36f * growth,
                    center - w * .43f * leafScale, earthY - h * .60f * growth)
                quadraticTo(center - w * .10f * leafScale, earthY - h * .61f * growth,
                    center, earthY - h * .30f * growth)
                close()
            }, Leaf)
            drawPath(Path().apply {
                moveTo(center, earthY - h * .43f * growth)
                quadraticTo(center + w * .36f * leafScale, earthY - h * .48f * growth,
                    center + w * .37f * leafScale, earthY - h * .70f * growth)
                quadraticTo(center + w * .09f * leafScale, earthY - h * .70f * growth,
                    center, earthY - h * .43f * growth)
                close()
            }, Color(0xFF78B96D))
        }
    }
}
