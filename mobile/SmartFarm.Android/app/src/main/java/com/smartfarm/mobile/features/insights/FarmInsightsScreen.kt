package com.smartfarm.mobile.features.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmMetricCard
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.data.remote.dto.MonthlyFinanceSummaryDto
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId

/** Avoid infinities, accidental mixed units or negative bar widths. */
internal fun chartFraction(value: BigDecimal, max: BigDecimal): Float {
    if (value <= BigDecimal.ZERO || max <= BigDecimal.ZERO) return 0f
    return value.divide(max, 6, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
}

@Composable
private fun InsightBar(label: String, amount: String, fraction: Float, note: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth().height(9.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(fraction).height(9.dp)
                .background(MaterialTheme.colorScheme.primary))
        }
        Text(amount, style = MaterialTheme.typography.bodyMedium)
        if (note != null) Text(note, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

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
                FarmHero("Farm reports", "Harvest yields and manual cash records in one place.")
                Text("Server-confirmed data only. Unsynced mobile harvests are not included.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All dates", "30" to "Last 30 days",
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FarmMetricCard("Valid entries", report.validHarvestCount.toString(),
                                modifier = Modifier.weight(1f))
                            FarmMetricCard("Voided entries", report.voidedHarvestCount.toString(),
                                modifier = Modifier.weight(1f))
                        }
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
        item(key = "harvest-chart") {
            state.harvest?.let { report ->
                FarmSectionCard("Yield by crop and plot",
                    subtitle = "Recorded quantities. Each unit is charted independently.") {
                    if (report.cropPlotTotals.isEmpty()) {
                        Text("No crop or plot totals for the selected dates.")
                    } else {
                        report.cropPlotTotals.groupBy { it.unit }.forEach { (unit, group) ->
                            Text("Unit: $unit", style = MaterialTheme.typography.titleMedium)
                            val max = group.maxOf { it.quantity }
                            group.sortedByDescending { it.quantity }.take(8).forEach { row ->
                                InsightBar("${row.cropTypeName} · ${row.plotName}",
                                    "${row.quantity.toPlainString()} $unit",
                                    chartFraction(row.quantity, max),
                                    "${row.harvestCount} partial harvests")
                            }
                            if (group.size > 8)
                                Text("Showing 8 of ${group.size} crop/plot totals in $unit.",
                                    style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item(key = "monthly") {
            state.harvest?.let { report ->
                FarmSectionCard("Monthly harvests", subtitle = "Recorded yield by month, separated by unit.") {
                    if (report.monthlyTotals.isEmpty()) {
                        Text("No monthly harvest totals match the period.")
                    } else {
                        report.monthlyTotals.groupBy { it.unit }.forEach { (unit, group) ->
                            Text("Unit: $unit", style = MaterialTheme.typography.titleMedium)
                            val recent = group.sortedBy { it.month }.takeLast(6)
                            val max = recent.maxOf { it.quantity }
                            recent.forEach { row ->
                                InsightBar(row.month, "${row.quantity.toPlainString()} $unit",
                                    chartFraction(row.quantity, max))
                            }
                        }
                    }
                }
            }
        }
        item(key = "finance") {
            val finance = state.finance
            FarmSectionCard("Manual cash records (PHP)",
                subtitle = "These are entered transactions, not profit or automatic harvest sales.") {
                when {
                    finance == null && state.loading -> Text("Loading farm finances…")
                    finance == null -> FarmEmptyState("Finance insights unavailable",
                        state.financeError ?: "Check connection and farm permissions.")
                    else -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FarmMetricCard("Money in", "₱" + finance.incomePhp.toPlainString(),
                                modifier = Modifier.weight(1f))
                            FarmMetricCard("Money out", "₱" + finance.expensesPhp.toPlainString(),
                                modifier = Modifier.weight(1f))
                        }
                        Text("Net recorded cash: ₱${finance.netPhp.toPlainString()}",
                            style = MaterialTheme.typography.titleLarge)
                        Text("${finance.entryCount} non-voided cash entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Harvests are not assumed sold. Inventory consumption is not automatically an expense.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item(key = "finance-chart") {
            state.finance?.let { finance ->
                FarmSectionCard("Cash trends", subtitle = "Income and expenses by month, in PHP.") {
                    val months = finance.months.sortedBy { it.month }.takeLast(6)
                    if (months.isEmpty()) {
                        Text("No cash entries in this period.")
                    } else {
                        val max = months.maxOf { maxOf(it.incomePhp, it.expensesPhp) }
                        months.forEach { month ->
                            Text(month.month, style = MaterialTheme.typography.titleMedium)
                            InsightBar("Money in", "₱${month.incomePhp.toPlainString()}",
                                chartFraction(month.incomePhp, max))
                            InsightBar("Money out", "₱${month.expensesPhp.toPlainString()}",
                                chartFraction(month.expensesPhp, max))
                        }
                    }
                    Text("For exports and detailed finance entries, use the Web farm workspace.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
