package com.smartfarm.mobile.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DateInput(value: String, label: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    Column {
        OutlinedTextField(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty(), {},
            label = { Text(label) }, readOnly = true, enabled = enabled, modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(enabled = enabled, onClick = {
                    val initial = date ?: LocalDate.now()
                    DatePickerDialog(context, { _, year, month, day ->
                        onValueChange(LocalDate.of(year, month + 1, day).toString())
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }) { Text("Choose date") }
            })
        if (value.isNotBlank()) TextButton(onClick = { onValueChange("") }, enabled = enabled) { Text("Clear $label") }
    }
}
