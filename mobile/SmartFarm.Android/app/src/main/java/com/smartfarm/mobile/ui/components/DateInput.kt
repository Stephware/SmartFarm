package com.smartfarm.mobile.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

/** The entire 48dp row opens the picker, including on small screens or with large text. */
@Composable
fun DateInput(value: String, label: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(
            onClick = {
                val initial = date ?: LocalDate.now()
                DatePickerDialog(context, { _, year, month, day ->
                    onValueChange(LocalDate.of(year, month + 1, day).toString())
                }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
            },
            enabled = enabled, modifier = Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .semantics {
                    contentDescription = "${label}: " +
                        (date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                            ?: "No date selected") + ". Choose date"
                }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    ?: "No date selected",
                    style = MaterialTheme.typography.bodyLarge)
                Text("Choose date", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (value.isNotBlank()) {
            TextButton(onClick = { onValueChange("") }, enabled = enabled,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("Clear $label") }
        }
    }
}
