package com.smartfarm.mobile.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display UTC API timestamps in the farm's configured time zone, never the device's guessed zone. */
fun farmDateTime(utc: String, zoneId: String?): String {
    val zone = runCatching { ZoneId.of(zoneId?.takeIf { it.isNotBlank() } ?: "UTC") }
        .getOrDefault(ZoneId.of("UTC"))
    return runCatching {
        Instant.parse(utc).atZone(zone).format(
            DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.ENGLISH)
        ) + " (${zone.id})"
    }.getOrElse { utc }
}
