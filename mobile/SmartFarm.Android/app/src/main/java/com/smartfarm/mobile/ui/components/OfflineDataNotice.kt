package com.smartfarm.mobile.ui.components

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OfflineDataNotice(offline: Boolean, savedAtUtcMillis: Long?) {
    if (!offline) return
    val saved = savedAtUtcMillis?.let {
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
    } ?: uiText(R.string.ui_a_previous_visit_74990fa, "a previous visit")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top) {
        FarmLineIcon(FarmSymbol.Offline, Modifier.size(24.dp), MaterialTheme.colorScheme.onTertiaryContainer)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(uiText(R.string.ui_viewing_saved_offline_data_ce4ed9e, "Viewing saved offline data"), style = MaterialTheme.typography.titleSmall)
            Text(
                uiText(R.string.ui_offline_copy_from_1_s_may_be_out_of_date_81dcbd6, "Offline copy from %1\$s. May be out of date. ", (saved)) +
                    uiText(R.string.ui_fieldwork_waits_to_send_settings_need_internet_99528b9, "Fieldwork waits to send; settings need internet."),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        }
    }
}
