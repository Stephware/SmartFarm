package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.data.remote.dto.CropCyclePlanDto
import com.smartfarm.mobile.data.remote.dto.CropTransitionDto
import com.smartfarm.mobile.data.remote.dto.CropTypeDto
import com.smartfarm.mobile.data.remote.dto.CropTypeInputDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CropApi {
    @GET("api/farms/{farmId}/crop-types")
    suspend fun cropTypes(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String): List<CropTypeDto>

    @POST("api/farms/{farmId}/crop-types")
    suspend fun createCropType(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Body input: CropTypeInputDto): CropTypeDto

    @GET("api/farms/{farmId}/crop-cycles")
    suspend fun cropCycles(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String): List<CropCycleDto>

    @POST("api/farms/{farmId}/plots/{plotId}/crop-cycles")
    suspend fun plan(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("plotId") plotId: String,
        @Body input: CropCyclePlanDto): CropCycleDto

    @PUT("api/farms/{farmId}/crop-cycles/{cycleId}")
    suspend fun updatePlan(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Body input: CropCyclePlanDto): CropCycleDto

    @POST("api/farms/{farmId}/crop-cycles/{cycleId}/transitions")
    suspend fun transition(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("cycleId") cycleId: String,
        @Body input: CropTransitionDto): CropCycleDto
}

