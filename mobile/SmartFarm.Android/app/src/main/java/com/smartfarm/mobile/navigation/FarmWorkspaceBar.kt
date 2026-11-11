package com.smartfarm.mobile.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

// Main farm routes remain unchanged. Deep forms retain their existing Back navigation.
private enum class FarmNavSymbol { Farm, Tasks, Crops, Stock, Account }
private data class FarmNavTab(val route: String, val title: String, val symbol: FarmNavSymbol)

/** Vector line-art avoids emoji fonts and keeps icons crisp without a new dependency. */
@Composable
private fun FarmNavIcon(symbol: FarmNavSymbol, tint: Color) {
    Canvas(Modifier.size(23.dp).clearAndSetSemantics { }) {
        val unit = size.minDimension / 24f
        val width = 1.9f * unit
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(x1 * unit, y1 * unit), Offset(x2 * unit, y2 * unit),
                strokeWidth = width, cap = StrokeCap.Round)
        }
        fun frame(x: Float, y: Float, w: Float, h: Float) {
            drawRoundRect(
                color = tint, topLeft = Offset(x * unit, y * unit),
                size = Size(w * unit, h * unit),
                cornerRadius = CornerRadius(2f * unit, 2f * unit),
                style = Stroke(width = width)
            )
        }
        when (symbol) {
            FarmNavSymbol.Farm -> {
                line(3f, 11f, 12f, 3f); line(12f, 3f, 21f, 11f)
                line(5f, 10f, 5f, 21f); line(19f, 10f, 19f, 21f)
                line(5f, 21f, 19f, 21f)
                line(9f, 21f, 9f, 14f); line(9f, 14f, 15f, 14f); line(15f, 14f, 15f, 21f)
            }
            FarmNavSymbol.Tasks -> {
                frame(4f, 3f, 16f, 18f)
                line(8f, 12f, 11f, 15f); line(11f, 15f, 16f, 9f)
            }
            FarmNavSymbol.Crops -> {
                line(12f, 21f, 12f, 9f)
                val left = Path().apply {
                    moveTo(12f * unit, 15f * unit)
                    quadraticTo(4f * unit, 16f * unit, 4f * unit, 7f * unit)
                    quadraticTo(12f * unit, 7f * unit, 12f * unit, 15f * unit)
                }
                val right = Path().apply {
                    moveTo(12f * unit, 12f * unit)
                    quadraticTo(20f * unit, 12f * unit, 20f * unit, 4f * unit)
                    quadraticTo(13f * unit, 4f * unit, 12f * unit, 12f * unit)
                }
                drawPath(left, tint, style = Stroke(width = width))
                drawPath(right, tint, style = Stroke(width = width))
            }
            FarmNavSymbol.Stock -> {
                line(3f, 7f, 12f, 3f); line(12f, 3f, 21f, 7f)
                line(3f, 7f, 12f, 12f); line(21f, 7f, 12f, 12f)
                line(3f, 7f, 3f, 18f); line(21f, 7f, 21f, 18f)
                line(3f, 18f, 12f, 22f); line(21f, 18f, 12f, 22f)
                line(12f, 12f, 12f, 22f)
            }
            FarmNavSymbol.Account -> {
                drawCircle(color = tint, radius = 4f * unit, center = Offset(12f * unit, 8f * unit),
                    style = Stroke(width = width))
                drawArc(color = tint, startAngle = 194f, sweepAngle = 152f, useCenter = false,
                    topLeft = Offset(4f * unit, 13f * unit), size = Size(16f * unit, 15f * unit),
                    style = Stroke(width = width))
            }
        }
    }
}

@Composable
internal fun FarmWorkspaceBar(
    selectedRoute: String,
    expanded: Boolean = false,
    onNavigate: (String) -> Unit
) {
    val tabs = listOf(
        FarmNavTab("farm/{farmId}", "Farm", FarmNavSymbol.Farm),
        FarmNavTab("tasks/{farmId}", "Tasks", FarmNavSymbol.Tasks),
        FarmNavTab("crops/{farmId}", "Crops", FarmNavSymbol.Crops),
        FarmNavTab("inventory/{farmId}", "Stock", FarmNavSymbol.Stock),
        FarmNavTab("account", "Account", FarmNavSymbol.Account)
    )
    val colors = MaterialTheme.colorScheme
    if (expanded) {
        NavigationRail(containerColor = colors.surface,
            modifier = Modifier.testTag("farm-navigation-rail")) {
            tabs.forEach { tab ->
                val selected = selectedRoute == tab.route
                NavigationRailItem(selected = selected,
                    onClick = { if (!selected) onNavigate(tab.route) },
                    icon = { FarmNavIcon(tab.symbol, if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant) },
                    label = { Text(tab.title) },
                    colors = NavigationRailItemDefaults.colors(
                        indicatorColor = colors.primaryContainer,
                        selectedTextColor = colors.primary,
                        unselectedTextColor = colors.onSurfaceVariant))
            }
        }
        return
    }
    NavigationBar(
        modifier = Modifier.testTag("farm-bottom-navigation"),
        containerColor = colors.surface,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            val selected = selectedRoute == tab.route
            NavigationBarItem(
                selected = selected,
                // Re-tapping an active tab must not reset unsaved field forms.
                onClick = { if (!selected) onNavigate(tab.route) },
                icon = {
                    FarmNavIcon(tab.symbol,
                        if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant)
                },
                label = { Text(tab.title) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.onSecondaryContainer,
                    selectedTextColor = colors.onSecondaryContainer,
                    indicatorColor = colors.primaryContainer,
                    unselectedIconColor = colors.onSurfaceVariant,
                    unselectedTextColor = colors.onSurfaceVariant
                )
            )
        }
    }
}
