package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.dto.WeatherDayDto
import com.smartfarm.mobile.features.farms.WeatherFreshness
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherFreshnessTest {
    private val now = Instant.parse("2026-11-11T08:00:00Z")

    private fun day(retrievedAt: String) = WeatherDayDto(
        date = "2026-11-11", minTemperatureC = null, maxTemperatureC = null,
        rainfallMm = null, retrievedAtUtc = retrievedAt, provider = "Open-Meteo")

    @Test fun recentForecastDoesNotWarn() {
        assertFalse(WeatherFreshness.isStale(listOf(day("2026-11-11T02:00:00Z")), now))
        assertFalse(WeatherFreshness.isStale(listOf(day("2026-11-10T20:00:00Z")), now))
    }

    @Test fun moreThanTwelveHoursOldWarns() {
        assertTrue(WeatherFreshness.isStale(listOf(day("2026-11-10T19:59:59Z")), now))
        assertTrue(WeatherFreshness.isStale(listOf(
            day("2026-11-11T06:00:00Z"), day("2026-11-09T08:00:00Z")), now))
    }

    @Test fun invalidOrFutureTimestampsDoNotLookFresh() {
        assertTrue(WeatherFreshness.isStale(listOf(day("not-a-date")), now))
        assertTrue(WeatherFreshness.isStale(listOf(day("2026-11-11T08:06:00Z")), now))
    }

    @Test fun missingForecastShowsExistingNoDataMessageRatherThanStaleWarning() {
        assertFalse(WeatherFreshness.isStale(emptyList(), now))
    }
}
