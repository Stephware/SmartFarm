package com.smartfarm.mobile.features.insights

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun FarmInsightsScreen(
    state: FarmInsightsUiState, farmTimeZone: String?,
    onBack: () -> Unit, onReload: (String?, String?) -> Unit
) {
    var range by rememberSaveable(state.farmId) { mutableStateOf("all") }
    val today = runCatching { LocalDate.now(ZoneId.of(requireNotNull(farmTimeZone))) }.getOrNull()
    RecordPage(title = uiText(R.string.ui_harvest_summary_2a05eb0, "Harvest summary"), onBack = onBack) {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FarmHero(uiText(R.string.ui_harvests_a2b3922, "Harvests"), uiText(R.string.ui_saved_harvests_with_each_unit_counted_separately_5250a53, "Saved harvests, with each unit counted separately."))
                Text(uiText(R.string.ui_server_confirmed_data_only_unsynced_mobile_harvests__7db9344, "Server-confirmed data only. Unsynced mobile harvests are not included."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to uiText(R.string.ui_all_a52ace4, "All"), "30" to uiText(R.string.ui_30_days_ffd7280, "30 days"),
                        "year" to uiText(R.string.ui_this_year_9859718, "This year")).forEach { (key, label) ->
                        FilterChip(selected = range == key,
                            enabled = !state.loading && (key == "all" || today != null),
                            onClick = {
                                range = key
                                val from = when (key) {
                                    "30" -> today?.minusDays(29)?.toString()
                                    "year" -> today?.withDayOfYear(1)?.toString()
                                    else -> null
                                }
                                onReload(from, if (key == "all") null else today?.toString())
                            }, label = { Text(label) })
                    }
                }
                if (today == null) Text(uiText(R.string.ui_farm_time_zone_unavailable_only_unfiltered_reports_c_fe79ab0, "Farm time zone unavailable. Only unfiltered reports can be shown."),
                    color = MaterialTheme.colorScheme.error)
                Text(uiText(R.string.ui_period_1_s_06bbcf0, "Period: %1\$s", (state.from ?: "All recorded dates")) +
                    (state.to?.let { uiText(R.string.ui_to_1_s_816c6ea, " to %1\$s", (it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.loading) {
                    FarmStatusChip(uiText(R.string.ui_loading_confirmed_farm_records_e9461a5, "Loading confirmed farm records"), FarmStatusTone.Planned)
                } else {
                    FarmStatusChip(uiText(R.string.ui_read_only_report_d939820, "Read-only report"), FarmStatusTone.Neutral)
                }
                OutlinedButton(onClick = { onReload(state.from, state.to) },
                    enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_refresh_reports_2cd6312, "Refresh reports"))
                }
            }
        }
        item(key = "harvest") {
            val report = state.harvest
            FarmSectionCard(uiText(R.string.ui_harvest_totals_741db9d, "Harvest totals"), subtitle = uiText(R.string.ui_voided_records_are_excluded_units_remain_separate_046178b, "Voided records are excluded. Units remain separate.")) {
                when {
                    report == null && state.loading -> Text(uiText(R.string.ui_loading_harvest_report_18f3d23, "Loading harvest report…"))
                    report == null -> {
                        FarmEmptyState(uiText(R.string.ui_harvest_report_unavailable_4af20de, "Harvest report unavailable"),
                            state.harvestError ?: uiText(R.string.ui_reconnect_to_view_confirmed_harvest_data_9f9e4ec, "Reconnect to view confirmed harvest data."))
                    }
                    else -> {
                        Text(uiText(R.string.ui_valid_harvests_1_s_3aa788d, "Valid harvests: %1\$s", (report.validHarvestCount)))
                        Text(uiText(R.string.ui_voided_harvests_1_s_38e8d81, "Voided harvests: %1\$s", (report.voidedHarvestCount)))
                        if (report.unitTotals.isEmpty()) {
                            FarmEmptyState(uiText(R.string.ui_no_recorded_harvests_f91dc50, "No recorded harvests"), uiText(R.string.ui_no_valid_entries_match_these_dates_ec09818, "No valid entries match these dates."))
                        } else report.unitTotals.forEach { total ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${total.quantity.toPlainString()} ${total.unit}",
                                    style = MaterialTheme.typography.headlineSmall)
                                Text(uiText(R.string.ui_1_s_valid_harvests_cfa7b47, "%1\$s valid harvests", (total.harvestCount)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
