package com.smartfarm.mobile.data.remote.dto

import java.math.BigDecimal

data class FarmDto(
    val id: String,
    val name: String,
    val locationText: String,
    val timeZoneId: String,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val role: String,
    val rowVersion: String? = null
)

data class FarmInputDto(
    val name: String,
    val locationText: String,
    val timeZoneId: String = "Asia/Manila",
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val rowVersion: String? = null
)

data class PlotDto(
    val id: String,
    val farmId: String,
    val name: String,
    val areaSqMeters: BigDecimal,
    val rowVersion: String
)

data class PlotInputDto(
    val name: String,
    val areaSqMeters: BigDecimal,
    val rowVersion: String? = null
)

data class FarmMemberDto(val id: String, val userId: String,
    val fullName: String, val email: String, val role: String)

data class UpdateMemberDto(val role: Int)

data class FarmJoinRequestDto(
    val id: String, val farmId: String, val farmName: String, val userId: String,
    val fullName: String, val email: String, val status: String,
    val requestedAtUtc: String, val reviewedAtUtc: String? = null
)
data class FarmEnrollmentOverviewDto(
    val joinCode: String = "", val requests: List<FarmJoinRequestDto> = emptyList(),
    val history: List<FarmJoinRequestDto> = emptyList()
)
data class RegenerateJoinCodeDto(val currentCode: String)
data class FarmJoinCodeDto(val joinCode: String)
data class FarmJoinInputDto(val farmCode: String)
data class FarmJoinDecisionDto(val approve: Boolean)

data class RowVersionDto(val rowVersion: String)
