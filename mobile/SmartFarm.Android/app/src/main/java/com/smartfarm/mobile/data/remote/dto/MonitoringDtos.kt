package com.smartfarm.mobile.data.remote.dto

data class ActivityInputDto(val activityType: Int, val notes: String? = null)
data class ObservationInputDto(
    val healthStatus: Int, val growthStage: String? = null, val notes: String? = null)

data class ActivityDto(
    val id: String, val activityType: Int, val occurredAtUtc: String,
    val recordedAtUtc: String, val recordedByUserId: String, val notes: String?)

data class ObservationPhotoDto(
    val id: String, val observationId: String,
    val contentType: String, val fileSizeBytes: Long, val uploadedAtUtc: String)

data class ObservationDto(
    val id: String, val healthStatus: Int, val growthStage: String?,
    val observedAtUtc: String, val recordedAtUtc: String,
    val recordedByUserId: String, val notes: String?,
    val photos: List<ObservationPhotoDto>)

data class MonitoringDto(
    val farmId: String, val cycleId: String, val canRecord: Boolean,
    val activities: List<ActivityDto>, val observations: List<ObservationDto>)

