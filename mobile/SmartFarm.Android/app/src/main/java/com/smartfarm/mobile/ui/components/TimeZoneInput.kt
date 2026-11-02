package com.smartfarm.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.ZoneId

@Composable
fun TimeZoneInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val zones = remember { ZoneId.getAvailableZoneIds().sorted() }
    OutlinedTextField(value, onValueChange, label = { Text("Time zone") }, enabled = enabled,
        singleLine = true, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = { search = ""; choosing = true }, enabled = enabled) { Text("Choose time zone") }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Choose time zone") },
        text = {
            Column {
                OutlinedTextField(search, { search = it }, label = { Text("Search city or region") }, singleLine = true)
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(zones.filter { it.contains(search, ignoreCase = true) }, key = { it }) { zone ->
                        TextButton(onClick = { onValueChange(zone); choosing = false }, enabled = enabled) {
                            Text(zone.replace('_', ' '))
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { choosing = false }) { Text("Cancel") } })
}
