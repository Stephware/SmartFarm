package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.theme.AniGoPalette

// Spacing is shared by field forms, dashboards and future farm-management screens.
object FarmSpacing {
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
}

enum class FarmStatusTone { Active, Planned, Warning, Failed, Neutral }

@Composable
fun FarmSectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FarmSpacing.extraSmall)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FarmSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FarmSpacing.large),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.medium)
        ) {
            FarmSectionHeader(title, subtitle)
            content()
        }
    }
}

@Composable
fun FarmStatusChip(
    label: String,
    tone: FarmStatusTone = FarmStatusTone.Neutral,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val (background, foreground) = when (tone) {
        FarmStatusTone.Active -> colors.primaryContainer to colors.onPrimaryContainer
        FarmStatusTone.Planned -> colors.secondaryContainer to colors.onSecondaryContainer
        FarmStatusTone.Warning -> colors.tertiaryContainer to colors.onTertiaryContainer
        FarmStatusTone.Failed -> colors.errorContainer to colors.onErrorContainer
        FarmStatusTone.Neutral -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = background,
        contentColor = foreground
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun FarmEmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(FarmSpacing.large),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.small)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Consistent Pine hero with a restrained Harvest Gold accent. */
@Composable
fun FarmHero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(FarmSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.medium)
        ) {
            Box(
                modifier = Modifier.width(5.dp).height(72.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AniGoPalette.harvestGold)
            )
            Column(verticalArrangement = Arrangement.spacedBy(FarmSpacing.small)) {
                Text(
                    title, style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A compact metric container, ready for the Phase 2 dashboard. */
@Composable
fun FarmMetricCard(label: String, value: String, note: String? = null, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(FarmSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(FarmSpacing.extraSmall)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            if (!note.isNullOrBlank()) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Decorative crop rows are a schematic illustration only, not real plot geometry
 * or a signal of crop health.
 */
@Composable
fun FarmPlotPreview(modifier: Modifier = Modifier) {
    val furrowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)
    Canvas(
        modifier = modifier.fillMaxWidth().height(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        val inset = 18.dp.toPx()
        val stroke = 7.dp.toPx()
        for (row in 1..3) {
            val y = size.height * row / 4f
            drawLine(
                color = furrowColor,
                start = Offset(inset, y),
                end = Offset(size.width - inset, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
