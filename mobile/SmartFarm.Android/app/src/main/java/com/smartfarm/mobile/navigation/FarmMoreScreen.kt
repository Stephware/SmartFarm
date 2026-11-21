package com.smartfarm.mobile.navigation

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
        MoreEntry("crops", uiText(R.string.ui_crops_history_fa46462, "Crops & history"), uiText(R.string.ui_current_plantings_plans_and_saved_crop_records_8b5fa24, "Current plantings, plans and saved crop records"), FarmSymbol.Crop),
        MoreEntry("inventory", uiText(R.string.ui_supplies_6be7560, "Supplies"), uiText(R.string.ui_available_stock_and_its_history_43b8194, "Available stock and its history"), FarmSymbol.Stock),
        MoreEntry("weather", uiText(R.string.ui_weather_a0bba63, "Weather"), uiText(R.string.ui_forecast_and_saved_advisories_54bfca5, "Forecast and saved advisories"), FarmSymbol.Weather),
        MoreEntry("plots", uiText(R.string.ui_growing_plots_f39fd7d, "Growing plots"), uiText(R.string.ui_view_and_manage_growing_areas_b58ea2f, "View and manage growing areas"), FarmSymbol.Plots),
        MoreEntry("insights", uiText(R.string.ui_harvest_summary_2a05eb0, "Harvest summary"), uiText(R.string.ui_confirmed_harvests_separated_by_unit_f1db34b, "Confirmed harvests, separated by unit"), FarmSymbol.Reports)
    )

internal fun moreManagement(role: String?): List<MoreEntry> =
    if (role == null) emptyList() else listOf(
        MoreEntry("members", uiText(R.string.ui_team_access_c3b54bb, "Team & access"), uiText(R.string.ui_members_and_joining_requests_cbcaa2d, "Members and joining requests"), FarmSymbol.Tasks)
    ) + if (role == "Owner" || role == "Manager")
        listOf(MoreEntry("settings", uiText(R.string.ui_farm_settings_df0ed3a, "Farm settings"), uiText(R.string.ui_location_details_and_time_zone_447bb27, "Location, details and time zone"), FarmSymbol.Plots))
    else emptyList()

internal val morePersonal = listOf(
    MoreEntry("notifications", uiText(R.string.ui_notifications_7880118, "Notifications"), uiText(R.string.ui_reminders_and_farm_messages_24f2deb, "Reminders and farm messages"), FarmSymbol.Bell),
    MoreEntry("sync", uiText(R.string.ui_offline_sync_843fdb9, "Offline sync"), uiText(R.string.ui_work_saved_on_this_phone_af769c5, "Work saved on this phone"), FarmSymbol.Offline),
    MoreEntry("account", uiText(R.string.ui_my_account_b53181a, "My account"), uiText(R.string.ui_profile_appearance_and_sign_out_d8c7ca3, "Profile, appearance and sign out"), FarmSymbol.Crop)
)

/** Keep frequently-used workflows in their tabs; secondary tools are short, grouped rows. */
@Composable
internal fun FarmMoreScreen(farmName: String, role: String?, onOpen: (String) -> Unit,
    pendingSync: Int = 0, reviewSync: Int = 0) {
    RecordPage {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FarmHero(uiText(R.string.ui_my_farm_b2e9f88, "My Farm"), uiText(R.string.ui_find_farm_records_and_account_tools_3b25329, "Find farm records and account tools."))
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
                FarmSectionHeader(uiText(R.string.ui_farm_records_2028410, "Farm records"))
            }
            item(key = "farm-tools") { MoreGroup(moreFarmTools(role), onOpen) }
            item(key = "team-header") {
                FarmSectionHeader(uiText(R.string.ui_people_farm_060f125, "People & farm"), uiText(R.string.ui_team_access_and_farm_management_5d4fd7a, "Team access and farm management."))
            }
            item(key = "farm-management") {
                MoreGroup(moreManagement(role) + MoreEntry("switch", uiText(R.string.ui_switch_farm_3edb967, "Switch farm"),
                    uiText(R.string.ui_choose_another_farm_to_work_in_e50f5af, "Choose another farm to work in"), FarmSymbol.Plots), onOpen)
            }
        } else item(key = "loading-farm") {
            Text(uiText(R.string.ui_loading_farm_tools_e2c4f0d, "Loading farm tools…"), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item(key = "personal-header") {
            FarmSectionHeader(uiText(R.string.ui_account_saved_work_f6e4c59, "Account & saved work"))
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
                        if (reviewSync > 0) uiText(R.string.ui_1_s_need_review_bed55e1, "%1\$s need review", (reviewSync)) else uiText(R.string.ui_1_s_waiting_7777b48, "%1\$s waiting", (pendingSync)) else null,
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
