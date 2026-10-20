package com.smartfarm.mobile.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OfflineDataNotice(offline: Boolean, savedAtUtcMillis: Long?) {
    if (!offline) return
    val saved = savedAtUtcMillis?.let {
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
            .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
    } ?: "a previous visit"
    Text(
        "Offline copy from $saved. Details may have changed on the server. " +
            "Only queued field records work offline; edits to farm and crop settings need internet.",
        color = MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.bodyMedium
    )
}
