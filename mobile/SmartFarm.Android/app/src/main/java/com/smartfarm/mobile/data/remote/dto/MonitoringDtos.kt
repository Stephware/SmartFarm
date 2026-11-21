package com.smartfarm.mobile.data.remote.dto

data class ActivityInputDto(
    val activityType: Int, val notes: String? = null, val farmTaskId: String? = null)
data class ObservationInputDto(
    val healthStatus: Int, val growthStage: String? = null, val notes: String? = null,
    val issueCategory: Int = 0, val issueSeverity: Int = 0)
data class ObservationFollowUpInputDto(
    val followUpStatus: Int, val followUpTaskId: String?, val rowVersion: String)

data class ActivityDto(
    val id: String, val activityType: Int, val occurredAtUtc: String,
    val recordedAtUtc: String, val recordedByUserId: String, val notes: String?,
    val farmTaskId: String? = null)

data class ObservationPhotoDto(
    val id: String, val observationId: String,
    val contentType: String, val fileSizeBytes: Long, val uploadedAtUtc: String)

data class ObservationDto(
    val id: String, val healthStatus: Int, val growthStage: String?,
    val observedAtUtc: String, val recordedAtUtc: String,
    val recordedByUserId: String, val notes: String?,
    val photos: List<ObservationPhotoDto>,
    val issueCategory: Int = 0, val issueSeverity: Int = 0,
    val followUpStatus: Int = 0, val followUpTaskId: String? = null,
    val rowVersion: String = "")

data class MonitoringDto(
    val farmId: String, val cycleId: String, val canRecord: Boolean,
    val activities: List<ActivityDto>, val observations: List<ObservationDto>)

