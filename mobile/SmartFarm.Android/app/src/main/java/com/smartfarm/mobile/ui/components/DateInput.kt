package com.smartfarm.mobile.ui.components

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.smartfarm.mobile.ui.theme.LocalFarmAppearance

/** The entire 48dp row opens the picker, including on small screens or with large text. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DateInput(value: String, label: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val dark = LocalFarmAppearance.current.dark
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = {
                val initial = date ?: LocalDate.now()
                DatePickerDialog(context, if (dark) R.style.Theme_SmartFarm_DatePicker_Dark else 0, { _, year, month, day ->
                    onValueChange(LocalDate.of(year, month + 1, day).toString())
                }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
            },
            enabled = enabled, modifier = Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics {
                    contentDescription = "${label}: " +
                        (date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            ?: uiText(R.string.ui_no_date_selected_a775f9f, "No date selected")) + uiText(R.string.ui_choose_date_da04f9d, ". Choose date")
                }
        ) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    ?: uiText(R.string.ui_no_date_selected_a775f9f, "No date selected"),
                    style = MaterialTheme.typography.bodyLarge)
                Text(uiText(R.string.ui_choose_date_343b5a9, "Choose date"), style = MaterialTheme.typography.labelLarge)
            }
        }
        if (value.isNotBlank()) {
            TextButton(onClick = { onValueChange("") }, enabled = enabled,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text(uiText(R.string.ui_clear_1_s_40157cb, "Clear %1\$s", (label))) }
        }
    }
}
