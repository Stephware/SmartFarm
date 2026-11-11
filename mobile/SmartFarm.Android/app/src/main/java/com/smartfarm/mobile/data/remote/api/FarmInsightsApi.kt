package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.FarmHarvestSummaryDto
import com.smartfarm.mobile.data.remote.dto.FarmFinanceSummaryDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface FarmInsightsApi {
    @GET("api/farms/{farmId}/reports/harvests")
    suspend fun harvestReport(
        @Header("Authorization") bearer: String, @Path("farmId") farmId: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null
    ): FarmHarvestSummaryDto

    @GET("api/farms/{farmId}/finance/insights")
    suspend fun financeInsights(
        @Header("Authorization") bearer: String, @Path("farmId") farmId: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null
    ): FarmFinanceSummaryDto
}
