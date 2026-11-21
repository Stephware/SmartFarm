package com.smartfarm.mobile.ui.components

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** UTC timestamp fields only; date-only values stay unchanged. */
fun farmDateTime(utc: String, zoneId: String?): String {
    val zone = runCatching { ZoneId.of(zoneId?.takeIf { it.isNotBlank() } ?: "Asia/Manila") }
        .getOrDefault(ZoneId.of("Asia/Manila"))
    return runCatching {
        val instant = runCatching { Instant.parse(utc) }.getOrElse {
            runCatching { OffsetDateTime.parse(utc).toInstant() }.getOrElse {
                // Database DateTime fields with no offset represent UTC instants.
                LocalDateTime.parse(utc).toInstant(ZoneOffset.UTC)
            }
        }
        instant.atZone(zone).format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a", Locale.getDefault()))
    }.getOrElse { utc }
}
