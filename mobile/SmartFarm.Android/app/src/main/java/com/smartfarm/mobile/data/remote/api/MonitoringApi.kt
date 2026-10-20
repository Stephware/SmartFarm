package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.ActivityDto
import com.smartfarm.mobile.data.remote.dto.ActivityInputDto
import com.smartfarm.mobile.data.remote.dto.MonitoringDto
import com.smartfarm.mobile.data.remote.dto.ObservationDto
import com.smartfarm.mobile.data.remote.dto.ObservationInputDto
import com.smartfarm.mobile.data.remote.dto.ObservationPhotoDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface MonitoringApi {
    @GET("api/farms/{farmId}/crop-cycles/{cycleId}/monitoring")
    suspend fun list(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String): MonitoringDto

    @POST("api/farms/{farmId}/crop-cycles/{cycleId}/monitoring/activities")
    suspend fun activity(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Body input: ActivityInputDto): ActivityDto

    @POST("api/farms/{farmId}/crop-cycles/{cycleId}/monitoring/observations")
    suspend fun observation(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Body input: ObservationInputDto): ObservationDto

    @Multipart
    @POST("api/farms/{farmId}/crop-cycles/{cycleId}/monitoring/observations/{observationId}/photos")
    suspend fun upload(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Path("observationId") observationId: String,
        @Part photo: MultipartBody.Part): ObservationPhotoDto

    // Stable ID prevents duplicates after the upload succeeds but the response is lost.
    @Multipart
    @PUT("api/farms/{farmId}/crop-cycles/{cycleId}/monitoring/observations/{observationId}/photos/{photoId}")
    suspend fun uploadWithId(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Path("observationId") observationId: String, @Path("photoId") photoId: String,
        @Part photo: MultipartBody.Part): ObservationPhotoDto

    @Streaming
    @GET("api/farms/{farmId}/crop-cycles/{cycleId}/monitoring/photos/{photoId}")
    suspend fun photo(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Path("photoId") photoId: String): ResponseBody
}

