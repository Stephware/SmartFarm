package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.WeatherOverviewDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface WeatherApi {
    @GET("api/farms/{farmId}/weather")
    suspend fun overview(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String): WeatherOverviewDto
}
