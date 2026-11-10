package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.data.remote.dto.WeatherDayDto
import java.time.Duration
import java.time.Instant

/** Forecast updates are expected every 6 hours; two missed updates mark them stale. */
internal object WeatherFreshness {
    private val maxAge: Duration = Duration.ofHours(12)
    private val allowedClockSkew: Duration = Duration.ofMinutes(5)

    fun isStale(days: List<WeatherDayDto>, now: Instant = Instant.now()): Boolean {
        if (days.isEmpty()) return false
        return days.any { day ->
            val checked = runCatching { Instant.parse(day.retrievedAtUtc) }.getOrNull()
                ?: return@any true
            checked.isBefore(now.minus(maxAge)) || checked.isAfter(now.plus(allowedClockSkew))
        }
    }
}
