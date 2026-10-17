package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface FarmApi {
    @GET("api/farms")
    suspend fun farms(@Header("Authorization") auth: String): List<FarmDto>

    @POST("api/farms")
    suspend fun createFarm(@Header("Authorization") auth: String,
        @Body input: FarmInputDto): FarmDto

    @GET("api/farms/{farmId}")
    suspend fun farm(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String): FarmDto

    @PUT("api/farms/{farmId}")
    suspend fun updateFarm(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Body input: FarmInputDto): FarmDto

    @HTTP(method = "DELETE", path = "api/farms/{farmId}", hasBody = true)
    suspend fun archiveFarm(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Body input: FarmInputDto)

    @GET("api/farms/{farmId}/plots")
    suspend fun plots(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String): List<PlotDto>

    @POST("api/farms/{farmId}/plots")
    suspend fun createPlot(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Body input: PlotInputDto): PlotDto

    @PUT("api/farms/{farmId}/plots/{plotId}")
    suspend fun updatePlot(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("plotId") plotId: String,
        @Body input: PlotInputDto): PlotDto

    @HTTP(method = "DELETE", path = "api/farms/{farmId}/plots/{plotId}", hasBody = true)
    suspend fun archivePlot(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("plotId") plotId: String,
        @Body input: PlotInputDto)

    @GET("api/farms/{farmId}/members")
    suspend fun members(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String): List<FarmMemberDto>

    @POST("api/farms/{farmId}/members/invitations")
    suspend fun invite(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Body input: InviteMemberDto): InvitationDto

    @POST("api/farm-invitations/accept")
    suspend fun accept(@Header("Authorization") auth: String,
        @Body input: AcceptInvitationDto): InvitationAcceptedDto

    @PUT("api/farms/{farmId}/members/{memberId}")
    suspend fun changeRole(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("memberId") memberId: String,
        @Body input: UpdateMemberDto)

    @DELETE("api/farms/{farmId}/members/{memberId}")
    suspend fun removeMember(@Header("Authorization") auth: String,
        @Path("farmId") farmId: String, @Path("memberId") memberId: String)
}

