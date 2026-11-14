package com.smartfarm.mobile.features.insights

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
    RecordPage {
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                FarmHero("Harvests", "Saved harvests, with each unit counted separately.")
                Text("Server-confirmed data only. Unsynced mobile harvests are not included.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "All", "30" to "30 days",
                        "year" to "This year").forEach { (key, label) ->
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
                if (today == null) Text("Farm time zone unavailable. Only unfiltered reports can be shown.",
                    color = MaterialTheme.colorScheme.error)
                Text("Period: ${state.from ?: "All recorded dates"}" +
                    (state.to?.let { " to $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.loading) {
                    FarmStatusChip("Loading confirmed farm records", FarmStatusTone.Planned)
                } else {
                    FarmStatusChip("Read-only report", FarmStatusTone.Neutral)
                }
                OutlinedButton(onClick = { onReload(state.from, state.to) },
                    enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh reports")
                }
            }
        }
        item(key = "harvest") {
            val report = state.harvest
            FarmSectionCard("Harvest summary", subtitle = "Voided records are excluded. Units remain separate.") {
                when {
                    report == null && state.loading -> Text("Loading harvest report…")
                    report == null -> {
                        FarmEmptyState("Harvest report unavailable",
                            state.harvestError ?: "Reconnect to view confirmed harvest data.")
                    }
                    else -> {
                        Text("Valid harvests: ${report.validHarvestCount}")
                        Text("Voided harvests: ${report.voidedHarvestCount}")
                        if (report.unitTotals.isEmpty()) {
                            FarmEmptyState("No recorded harvests", "No valid entries match these dates.")
                        } else report.unitTotals.forEach { total ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${total.quantity.toPlainString()} ${total.unit}",
                                    style = MaterialTheme.typography.headlineSmall)
                                Text("${total.harvestCount} valid harvests",
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
