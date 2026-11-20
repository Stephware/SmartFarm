package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

data class WeatherDayDto(
    val date: String,
    val minTemperatureC: BigDecimal?,
    val maxTemperatureC: BigDecimal?,
    val rainfallMm: BigDecimal?,
    val retrievedAtUtc: String,
    val provider: String,
    val maxPrecipitationProbability: BigDecimal? = null,
    val maxWindKph: BigDecimal? = null
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

/** Validated read-only advisory guidance; never a command to mutate farm records. */
data class GuidanceMessageDto(val key: String, val text: String)
data class WeatherGuidanceDto(
    val advisoryId: String, val cropCycleId: String?, val language: String,
    val severity: Int, val forecastDate: String, val weatherProvider: String?,
    val retrievedAtUtc: String?, val isStale: Boolean, val isSynthetic: Boolean,
    val generationSource: String, val model: String?,
    val explanations: List<GuidanceMessageDto>, val recommendations: List<GuidanceMessageDto>,
    val limitations: List<String>
)
