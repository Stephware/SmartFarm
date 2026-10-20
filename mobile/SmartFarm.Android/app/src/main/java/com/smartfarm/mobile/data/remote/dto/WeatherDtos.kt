package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

data class WeatherDayDto(
    val date: String,
    val minTemperatureC: BigDecimal?,
    val maxTemperatureC: BigDecimal?,
    val rainfallMm: BigDecimal?,
    val retrievedAtUtc: String,
    val provider: String
)

data class WeatherAdvisoryDto(
    val id: String, val date: String, val type: Int,
    val severity: Int, val status: Int, val message: String,
    val createdAtUtc: String
)

data class WeatherOverviewDto(
    val farmId: String, val hasCoordinates: Boolean,
    val timeZoneId: String, val days: List<WeatherDayDto>,
    val advisories: List<WeatherAdvisoryDto>
)
