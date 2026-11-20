package com.smartfarm.mobile.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path

data class TaskDto(val id: String, val farmId: String, val cropCycleId: String?, val assignedToUserId: String?,
    val taskType: Int, val dueDate: String, val status: Int, val description: String?, val rowVersion: String)
data class TaskCloseInput(val status: Int, val rowVersion: String)

// Existing server contracts. Task creation/editing remains online-only.
data class TaskCreateInput(
    val taskType: Int, val dueDate: String, val description: String?,
    val cropCycleId: String?, val assignedToUserId: String?
)
data class TaskUpdateInput(
    val dueDate: String, val description: String?,
    val assignedToUserId: String?, val rowVersion: String
)
data class NotificationDto(val id: String, val title: String, val message: String,
    val createdAtUtc: String, val readAtUtc: String?)

data class AniAlertDto(val id: String, val farmId: String?, val title: String, val message: String,
    val localizedMessage: String, val source: String, val severity: Int?, val shouldInterrupt: Boolean,
    val isStale: Boolean, val readAtUtc: String?, val acknowledgedAtUtc: String?)
data class AniAlertPage(val items: List<AniAlertDto>, val hasMore: Boolean = false)

interface TaskApi {
    @GET("api/notifications/alerts")
    suspend fun alertPage(@Header("Authorization") bearer: String,
        @retrofit2.http.Query("page") page: Int, @retrofit2.http.Query("pageSize") pageSize: Int = 100): AniAlertPage

    @GET("api/notifications/alerts?unread=true&pageSize=100")
    suspend fun alerts(@Header("Authorization") bearer: String): AniAlertPage
    @PUT("api/notifications/{id}/acknowledge")
    suspend fun acknowledge(@Header("Authorization") bearer: String, @Path("id") id: String)

    @GET("api/farms/{farmId}/tasks")
    suspend fun tasks(@Header("Authorization") bearer: String, @Path("farmId") farmId: String): List<TaskDto>
    @POST("api/farms/{farmId}/tasks")
    suspend fun create(@Header("Authorization") bearer: String, @Path("farmId") farmId: String,
        @Body input: TaskCreateInput): TaskDto
    @PUT("api/farms/{farmId}/tasks/{taskId}")
    suspend fun update(@Header("Authorization") bearer: String, @Path("farmId") farmId: String,
        @Path("taskId") taskId: String, @Body input: TaskUpdateInput): TaskDto
    @PUT("api/farms/{farmId}/tasks/{taskId}/close")
    suspend fun close(@Header("Authorization") bearer: String, @Path("farmId") farmId: String,
        @Path("taskId") taskId: String, @Body input: TaskCloseInput): TaskDto
    @GET("api/notifications")
    suspend fun notifications(@Header("Authorization") bearer: String): List<NotificationDto>
    @PUT("api/notifications/{notificationId}/read")
    suspend fun read(@Header("Authorization") bearer: String, @Path("notificationId") notificationId: String)
}
