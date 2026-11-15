package com.smartfarm.mobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmLineIcon
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmSymbol
import com.smartfarm.mobile.ui.components.farmRoleLabel
import com.smartfarm.mobile.ui.components.FarmSpacing
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
                FarmHero("My Farm", "Find farm records and account tools.")
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FarmSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(FarmSpacing.extraSmall)) {
                    Text(farmName, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f).widthIn(min = 180.dp))
                    role?.let { FarmStatusChip(farmRoleLabel(it), FarmStatusTone.Planned) }
                }
            }
        }
        if (role != null) {
            item(key = "farm-tools-header") {
                FarmSectionHeader("Farm records")
            }
            item(key = "farm-tools") { MoreGroup(moreFarmTools(role), onOpen) }
            item(key = "team-header") {
                FarmSectionHeader("People & farm", "Team access and farm management.")
            }
            item(key = "farm-management") {
                MoreGroup(moreManagement(role) + MoreEntry("switch", "Switch farm",
                    "Choose another farm to work in", FarmSymbol.Plots), onOpen)
            }
        } else item(key = "loading-farm") {
            Text("Loading farm tools…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key = "personal-header") {
            FarmSectionHeader("Account & saved work")
        }
        item(key = "personal") { MoreGroup(morePersonal, onOpen, pendingSync, reviewSync) }
    }
}

@Composable
private fun MoreGroup(entries: List<MoreEntry>, onOpen: (String) -> Unit,
    pendingSync: Int = 0, reviewSync: Int = 0) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
        Column {
            entries.forEachIndexed { index, entry ->
                MoreDestination(entry, onOpen,
                    trailing = if (entry.route == "sync" && pendingSync > 0)
                        if (reviewSync > 0) "$reviewSync need review" else "$pendingSync waiting" else null,
                    needsReview = reviewSync > 0)
                if (index < entries.lastIndex) HorizontalDivider(
                    Modifier.padding(start = 50.dp, end = FarmSpacing.medium),
                    color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun MoreDestination(entry: MoreEntry, onOpen: (String) -> Unit,
    trailing: String? = null, needsReview: Boolean = false) {
    Surface(onClick = { onOpen(entry.route) },
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp)
            .semantics { role = Role.Button },
        color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(horizontal = FarmSpacing.medium, vertical = FarmSpacing.compact),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(FarmSpacing.compact)) {
            FarmLineIcon(entry.symbol, Modifier.padding(top = 2.dp).size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FarmSpacing.extraSmall)) {
                Text(entry.label, style = MaterialTheme.typography.titleMedium)
                Text(entry.hint, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (trailing != null) {
                    FarmStatusChip(trailing, if (needsReview) FarmStatusTone.Warning else FarmStatusTone.Planned)
                }
            }
            FarmLineIcon(FarmSymbol.ChevronRight, Modifier.padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
