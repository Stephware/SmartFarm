package com.smartfarm.mobile.ui.components.onboarding

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
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
private val Pine: Color @Composable get() = MaterialTheme.colorScheme.primary
private val Gold: Color @Composable get() = MaterialTheme.colorScheme.secondary
private val Ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
private val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val Leaf = Color(0xFF4B9C54)
private val Border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

internal fun reveal(progress: Float, start: Float): Float =
    ((progress - start) / 0.13f).coerceIn(0f, 1f)

@Composable
internal fun ExampleCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
    opacity: Float = 1f,
    compact: Boolean = false
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
            Canvas(Modifier.size(14.dp)) {
                drawLine(color, Offset(size.width * .15f, size.height * .5f),
                    Offset(size.width * .4f, size.height * .75f), 2.dp.toPx(), StrokeCap.Round)
                drawLine(color, Offset(size.width * .4f, size.height * .75f),
                    Offset(size.width * .85f, size.height * .2f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold)
            if (!compact) Text(subtext, color = Muted, style = MaterialTheme.typography.labelSmall)
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
        contentDescription = uiText(R.string.ui_sample_farm_setup_step_1_s_of_4_demonstration_only_b075867, "Sample farm setup, step %1\$s of 4; demonstration only", (phase))
    }) {
        ExampleCard(uiText(R.string.ui_riverside_farm_3379a8e, "Riverside Farm"), uiText(R.string.ui_sample_farm_workspace_6dd9a14, "Sample farm workspace")) {
            if (progress >= .18f) {
                IntroRow(uiText(R.string.ui_north_plot_196c923, "North Plot"), uiText(R.string.ui_crop_area_6ce5c1c, "Crop area"),
                    opacity = reveal(progress, .18f))
            }
            if (progress >= .46f) {
                IntroRow(uiText(R.string.ui_south_plot_0d12521, "South Plot"), uiText(R.string.ui_crop_area_6ce5c1c, "Crop area"),
                    opacity = reveal(progress, .46f))
            }
            if (progress >= .73f) {
                Spacer(Modifier.height(2.dp))
                Text(uiText(R.string.ui_owner_team_members_ea9cf2b, "Owner & team members"), color = Pine,
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
        contentDescription = uiText(R.string.ui_sample_crop_plan_step_1_s_of_4_demonstration_only_dbe7713, "Sample crop plan, step %1\$s of 4; demonstration only", (phase))
    }) {
        ExampleCard(uiText(R.string.ui_rice_planting_29a39f4, "Rice planting"), uiText(R.string.ui_sample_crop_calendar_40575e6, "Sample crop calendar")) {
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
                        if (progress >= .74f) uiText(R.string.ui_growing_52c8c11, "Growing") else if (progress >= .39f) uiText(R.string.ui_planted_92b5c62, "Planted")
                            else uiText(R.string.ui_planned_fb6f7c9, "Planned"),
                        color = Pine,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(uiText(R.string.ui_rice_1b9bc7b, "Rice"), color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(uiText(R.string.ui_plan_fa8ed0b, "Plan"), uiText(R.string.ui_plant_0bd287f, "Plant"), uiText(R.string.ui_grow_b97dc52, "Grow")).forEachIndexed { index, title ->
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
                Text(uiText(R.string.ui_upcoming_crop_activities_8b6e583, "Upcoming: crop activities"), color = Pine,
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
