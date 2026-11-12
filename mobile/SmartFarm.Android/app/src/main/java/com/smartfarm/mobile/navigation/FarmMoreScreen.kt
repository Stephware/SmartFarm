package com.smartfarm.mobile.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage

/**
 * Secondary functions are grouped by farmers' goals, not by technical modules.
 * Authorization remains enforced by the existing screens and API.
 */
@Composable
internal fun FarmMoreScreen(farmName: String, role: String?, onOpen: (String) -> Unit) {
    val canManage = role == "Owner" || role == "Manager"
    RecordPage {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FarmHero("More", "Find farm tools, reports, and account settings.")
                Text(farmName, style = MaterialTheme.typography.titleMedium)
                if (role != null) FarmStatusChip(role, FarmStatusTone.Planned)
            }
        }
        item(key = "operations") {
            FarmSectionCard("Farm operations", subtitle = "Supplies, forecasts and growing areas") {
                MoreDestination("Supplies and stock", "Check balances, record supply use, review movements",
                    onClick = { onOpen("inventory") })
                MoreDestination("Weather and advisories", "See the saved forecast for this farm",
                    onClick = { onOpen("weather") })
                MoreDestination("Farm plots", "View your growing areas", onClick = { onOpen("plots") })
            }
        }
        item(key = "records") {
            FarmSectionCard("Reports", subtitle = "Harvest and cash summaries from confirmed records") {
                MoreDestination("Harvest reports and finances", "Read trends, totals and financial insights",
                    onClick = { onOpen("insights") })
            }
        }
        item(key = "management") {
            FarmSectionCard("Farm and team", subtitle = "Farm access and settings") {
                MoreDestination("Team and access", "Members and owner-managed join requests",
                    onClick = { onOpen("members") })
                if (canManage) {
                    MoreDestination("Farm settings", "Edit farm details, coordinates and time zone",
                        onClick = { onOpen("settings") })
                }
                MoreDestination("Switch farm", "Choose another farm or manage farm access",
                    onClick = { onOpen("switch") })
            }
        }
        item(key = "account") {
            FarmSectionCard("Account and connection", subtitle = "Personal settings and saved field records") {
                MoreDestination("Notifications", "Farm tasks, reminders and messages",
                    onClick = { onOpen("notifications") })
                MoreDestination("Offline sync", "Review unsynced work and any errors",
                    onClick = { onOpen("sync") })
                MoreDestination("My account", "Profile, appearance and log out",
                    onClick = { onOpen("account") })
            }
        }
    }
}

@Composable
private fun MoreDestination(title: String, description: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
