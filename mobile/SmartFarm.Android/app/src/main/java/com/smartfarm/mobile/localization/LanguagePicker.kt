package com.smartfarm.mobile.localization
import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.R
@Composable fun LanguagePicker() {
    val context = LocalContext.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge)
        TextButton(onClick = { AppLanguage.select(context, "en") }, enabled = AppLanguage.current(context) != "en") { Text("English") }
        TextButton(onClick = { AppLanguage.select(context, "fil") }, enabled = AppLanguage.current(context) != "fil") { Text("Filipino") }
    }
}
