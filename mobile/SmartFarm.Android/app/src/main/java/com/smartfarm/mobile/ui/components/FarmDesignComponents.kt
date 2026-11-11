package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

/** Native page heading uses the reference's open ground and compact hierarchy. */
@Composable
fun FarmHero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() })
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

data class FarmHeaderNavigation(val farmName: String?, val onFarms: () -> Unit, val onNotifications: () -> Unit)
val LocalFarmHeaderNavigation = compositionLocalOf<FarmHeaderNavigation?> { null }

/** Brand and screen context remain outside the scrolling workspace. */
@Composable
fun FarmAppBar(modifier: Modifier = Modifier) {
    val navigation = LocalFarmHeaderNavigation.current
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp)
                .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer) {
                    FarmLineIcon(FarmSymbol.Crop, Modifier.padding(8.dp).size(23.dp))
                }
                Text("AniGo", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold)
                Box(Modifier.weight(1f))
                if (navigation != null) {
                    androidx.compose.material3.OutlinedButton(onClick = navigation.onFarms,
                        modifier = Modifier.widthIn(max = 160.dp), shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)) {
                        Text(navigation.farmName ?: "My farms", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium)
                    }
                    androidx.compose.material3.IconButton(onClick = navigation.onNotifications,
                        modifier = Modifier.semantics { contentDescription = "Notifications" }) {
                        FarmLineIcon(FarmSymbol.Bell)
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

enum class FarmSymbol { Crop, Plots, Tasks, Clock, Stock, Weather, Reports, Offline, Bell }

/** One stroke system for metrics, shortcuts and offline indicators. */
@Composable
fun FarmLineIcon(symbol: FarmSymbol, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier.size(22.dp)) {
        val u = size.minDimension / 24f
        val stroke = 1.7f * u
        fun line(a: Float, b: Float, c: Float, d: Float) = drawLine(tint,
            Offset(a*u,b*u), Offset(c*u,d*u), stroke, StrokeCap.Round)
        fun rect(x: Float, y: Float, w: Float, h: Float) = drawRoundRect(tint,
            Offset(x*u,y*u), androidx.compose.ui.geometry.Size(w*u,h*u),
            androidx.compose.ui.geometry.CornerRadius(2*u), style = Stroke(stroke))
        when (symbol) {
            FarmSymbol.Crop -> {
                line(12f,21f,12f,9f)
                val leaf = Path().apply {
                    moveTo(12*u,15*u); cubicTo(4*u,16*u,3*u,9*u,4*u,6*u)
                    cubicTo(10*u,6*u,13*u,10*u,12*u,15*u)
                    moveTo(12*u,11*u); cubicTo(12*u,4*u,17*u,3*u,21*u,3*u)
                    cubicTo(21*u,9*u,17*u,12*u,12*u,11*u)
                }
                drawPath(leaf,tint,style=Stroke(stroke))
            }
            FarmSymbol.Plots -> { rect(3f,3f,7f,7f);rect(14f,3f,7f,7f);rect(3f,14f,7f,7f);rect(14f,14f,7f,7f) }
            FarmSymbol.Tasks -> { rect(4f,3f,16f,18f);line(8f,12f,11f,15f);line(11f,15f,16f,9f) }
            FarmSymbol.Clock -> {
                drawCircle(tint,9*u,Offset(12*u,12*u),style=Stroke(stroke))
                line(12f,6f,12f,12f);line(12f,12f,16f,14f)
            }
            FarmSymbol.Stock -> { rect(3f,7f,18f,14f);line(3f,11f,21f,11f);line(9f,3f,15f,3f);line(9f,3f,9f,7f);line(15f,3f,15f,7f) }
            FarmSymbol.Weather -> {
                drawCircle(tint,5*u,Offset(12*u,12*u),style=Stroke(stroke))
                for (i in 0..7) {
                    val a = i * Math.PI / 4
                    line((12+8*kotlin.math.cos(a)).toFloat(),(12+8*kotlin.math.sin(a)).toFloat(),
                        (12+10*kotlin.math.cos(a)).toFloat(),(12+10*kotlin.math.sin(a)).toFloat())
                }
            }
            FarmSymbol.Reports -> { line(4f,20f,21f,20f);line(7f,16f,7f,10f);line(12f,16f,12f,4f);line(17f,16f,17f,7f) }
            FarmSymbol.Bell -> {
                val bell = Path().apply {
                    moveTo(5*u,17*u); lineTo(19*u,17*u); lineTo(17*u,13*u); lineTo(17*u,9*u)
                    cubicTo(17*u,2*u,7*u,2*u,7*u,9*u); lineTo(7*u,13*u); close()
                }
                drawPath(bell,tint,style=Stroke(stroke)); line(10f,21f,14f,21f)
            }
            FarmSymbol.Offline -> { line(3f,3f,21f,21f);line(6f,11f,10f,9f);line(14f,9f,18f,11f);line(9f,15f,12f,14f);drawCircle(tint,1.3f*u,Offset(12*u,19*u)) }
        }
    }
}

@Composable
fun FarmMetricCard(label: String, value: String, note: String? = null, modifier: Modifier = Modifier,
    symbol: FarmSymbol = FarmSymbol.Crop) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(10.dp),
                color = if (symbol == FarmSymbol.Clock || symbol == FarmSymbol.Tasks)
                    MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer) {
                FarmLineIcon(symbol, Modifier.padding(9.dp).size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!note.isNullOrBlank()) Text(note, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Schematic planting rows, never geographic geometry or crop health. */
@Composable
fun FarmPlotPreview(modifier: Modifier = Modifier, cropName: String? = null) {
    val rows = when (cropArtworkKind(cropName)) {
        CropArtworkKind.Rice -> listOf(Color(0xFFB4A65C),Color(0xFFD2BF6F),Color(0xFF9B934C))
        CropArtworkKind.Eggplant -> listOf(Color(0xFF8C9471),Color(0xFFA4A78B),Color(0xFF657E59))
        else -> listOf(Color(0xFF79995F),Color(0xFF91AF75),Color(0xFF5F8551))
    }
    Box(modifier.fillMaxWidth().height(112.dp).clip(MaterialTheme.shapes.medium)
        .background(rows[0]), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 18.dp.toPx()
            var x = -size.height
            var index = 0
            while (x < size.width + size.height) {
                drawLine(rows[index%3],Offset(x, size.height),Offset(x+size.height*1.5f,0f),
                    strokeWidth=step/3)
                x += step/3; index++
            }
            drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color(0x55153D2D))))
        }
        if (!cropName.isNullOrBlank()) CropArtworkImage(cropName, Modifier.size(76.dp))
    }
}

@Composable
fun FarmQuickAction(label: String, symbol: FarmSymbol, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedButton(onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp), shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FarmLineIcon(symbol, Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
