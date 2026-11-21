package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.WeatherOverviewDto
import com.smartfarm.mobile.data.remote.dto.WeatherGuidanceDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherApi {
    @GET("api/farms/{farmId}/weather/advisories/{advisoryId}/guidance")
    suspend fun guidance(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("advisoryId") advisoryId: String,
        @Query("cropCycleId") cropCycleId: String? = null): WeatherGuidanceDto

    @GET("api/farms/{farmId}/weather")
    suspend fun overview(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String): WeatherOverviewDto
}
