package com.smartfarm.mobile.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

data class TaskDto(val id: String, val farmId: String, val cropCycleId: String?, val assignedToUserId: String?,
    val taskType: Int, val dueDate: String, val status: Int, val description: String?, val rowVersion: String)
data class TaskCloseInput(val status: Int, val rowVersion: String)
data class NotificationDto(val id: String, val title: String, val message: String,
    val createdAtUtc: String, val readAtUtc: String?)

interface TaskApi {
    @GET("api/farms/{farmId}/tasks")
    suspend fun tasks(@Header("Authorization") bearer: String, @Path("farmId") farmId: String): List<TaskDto>
    @PUT("api/farms/{farmId}/tasks/{taskId}/close")
    suspend fun close(@Header("Authorization") bearer: String, @Path("farmId") farmId: String,
        @Path("taskId") taskId: String, @Body input: TaskCloseInput): TaskDto
    @GET("api/notifications")
    suspend fun notifications(@Header("Authorization") bearer: String): List<NotificationDto>
    @PUT("api/notifications/{notificationId}/read")
    suspend fun read(@Header("Authorization") bearer: String, @Path("notificationId") notificationId: String)
}
