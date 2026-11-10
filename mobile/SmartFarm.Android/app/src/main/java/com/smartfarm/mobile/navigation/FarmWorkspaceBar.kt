package com.smartfarm.mobile.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Main farm navigation only. Deep screens (monitoring, harvest, stock details, sync)
 * intentionally keep their existing Back behavior and do not display these tabs.
 * Actions that change farm data stay on their original screens.
 */
@Composable
internal fun FarmWorkspaceBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    val tabs = listOf(
        Triple("farm/{farmId}", "Farm", "⌂"),
        Triple("tasks/{farmId}", "Tasks", "☑"),
        Triple("crops/{farmId}", "Crops", "❧"),
        Triple("inventory/{farmId}", "Stock", "▤"),
        Triple("account", "Account", "◉")
    )
    NavigationBar(
        modifier = Modifier.testTag("farm-bottom-navigation"),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { (route, label, symbol) ->
            NavigationBarItem(
                selected = selectedRoute == route,
                // Re-tapping the active tab should not add another destination or reset forms.
                onClick = { if (selectedRoute != route) onNavigate(route) },
                // The visible label supplies the spoken name; don't read decorative glyphs.
                icon = {
                    Text(
                        symbol,
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                label = { Text(label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
