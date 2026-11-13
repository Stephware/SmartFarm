package com.smartfarm.mobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmLineIcon
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmSymbol
import com.smartfarm.mobile.ui.components.RecordPage

internal data class MoreEntry(val route: String, val label: String,
    val hint: String, val symbol: FarmSymbol)

internal fun moreFarmTools(role: String?): List<MoreEntry> =
    if (role == null) emptyList() else listOf(
        MoreEntry("crops", "Crops & history", "Current plantings, plans and saved crop records", FarmSymbol.Crop),
        MoreEntry("inventory", "Supplies", "Available stock and its history", FarmSymbol.Stock),
        MoreEntry("weather", "Weather", "Forecast and saved advisories", FarmSymbol.Weather),
        MoreEntry("plots", "Growing plots", "View and manage growing areas", FarmSymbol.Plots),
        MoreEntry("insights", "Harvest summary", "Confirmed harvests, separated by unit", FarmSymbol.Reports)
    )

internal fun moreManagement(role: String?): List<MoreEntry> =
    if (role == null) emptyList() else listOf(
        MoreEntry("members", "Team & access", "Members and joining requests", FarmSymbol.Tasks)
    ) + if (role == "Owner" || role == "Manager")
        listOf(MoreEntry("settings", "Farm settings", "Location, details and time zone", FarmSymbol.Plots))
    else emptyList()

internal val morePersonal = listOf(
    MoreEntry("notifications", "Notifications", "Reminders and farm messages", FarmSymbol.Bell),
    MoreEntry("sync", "Offline sync", "Work saved on this phone", FarmSymbol.Offline),
    MoreEntry("account", "My account", "Profile, appearance and sign out", FarmSymbol.Crop)
)

/** Keep frequently-used workflows in their tabs; secondary tools are short, grouped rows. */
@Composable
internal fun FarmMoreScreen(farmName: String, role: String?, onOpen: (String) -> Unit,
    pendingSync: Int = 0, reviewSync: Int = 0) {
    RecordPage {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmHero("My Farm", "Crops, growing areas, supplies and harvest history.")
                Text(farmName, style = MaterialTheme.typography.titleMedium)
                role?.let { FarmStatusChip(it, FarmStatusTone.Planned) }
            }
        }
        if (role != null) {
            item(key = "farm-tools-header") {
                FarmSectionHeader("Farm records")
            }
            items(moreFarmTools(role).size, key = { "farm-tool-$it" }) { index ->
                val entry = moreFarmTools(role)[index]
                MoreDestination(entry, onOpen)
            }
            item(key = "team-header") {
                FarmSectionHeader("People & farm", "Team access and farm management.")
            }
            items(moreManagement(role).size, key = { "farm-manage-$it" }) { index ->
                val entry = moreManagement(role)[index]
                MoreDestination(entry, onOpen)
            }
            item(key = "switch-farm") {
                MoreDestination(MoreEntry("switch", "Switch farm",
                    "Choose another farm to work in", FarmSymbol.Plots), onOpen)
            }
        } else item(key = "loading-farm") {
            Text("Loading farm tools…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key = "personal-header") {
            FarmSectionHeader("Account & saved work")
        }
        items(morePersonal.size, key = { "personal-$it" }) { index ->
            val entry = morePersonal[index]
            MoreDestination(entry, onOpen,
                trailing = if (entry.route == "sync" && pendingSync > 0)
                    if (reviewSync > 0) "$reviewSync need review"
                    else "$pendingSync waiting" else null)
        }
    }
}

@Composable
private fun MoreDestination(entry: MoreEntry, onOpen: (String) -> Unit,
    trailing: String? = null) {
    OutlinedButton(onClick = { onOpen(entry.route) },
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 14.dp, vertical = 9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FarmLineIcon(entry.symbol, Modifier.size(21.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.label, style = MaterialTheme.typography.titleSmall)
                Text(entry.hint, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
