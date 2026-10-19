package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.HarvestEntryDto
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.remote.dto.VoidHarvestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface HarvestApi {
    @GET("api/farms/{farmId}/crop-cycles/{cycleId}/harvests")
    suspend fun list(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String,
        @Path("cycleId") cycleId: String): HarvestOverviewDto

    @POST("api/farms/{farmId}/crop-cycles/{cycleId}/harvests")
    suspend fun record(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String,
        @Path("cycleId") cycleId: String,
        @Body input: RecordHarvestDto): HarvestEntryDto

    @POST("api/farms/{farmId}/crop-cycles/{cycleId}/harvests/{harvestId}/void")
    suspend fun void(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String,
        @Path("cycleId") cycleId: String,
        @Path("harvestId") harvestId: String,
        @Body input: VoidHarvestDto): HarvestEntryDto
}

