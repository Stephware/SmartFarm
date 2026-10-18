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

interface InventoryApi {
    @GET("api/farms/{farmId}/inventory")
    suspend fun list(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String): InventoryWorkspaceDto

    @GET("api/farms/{farmId}/inventory/activities")
    suspend fun activities(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String): List<InventoryActivityOptionDto>

    @POST("api/farms/{farmId}/inventory")
    suspend fun create(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Body input: InventoryItemInputDto): InventoryItemDto

    @GET("api/farms/{farmId}/inventory/{itemId}")
    suspend fun item(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("itemId") itemId: String): InventoryItemDto

    @PUT("api/farms/{farmId}/inventory/{itemId}")
    suspend fun update(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("itemId") itemId: String,
        @Body input: InventoryItemInputDto): InventoryItemDto

    @HTTP(method = "DELETE", path = "api/farms/{farmId}/inventory/{itemId}", hasBody = true)
    suspend fun archive(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("itemId") itemId: String,
        @Body input: RowVersionDto)

    @GET("api/farms/{farmId}/inventory/{itemId}/transactions")
    suspend fun history(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("itemId") itemId: String): List<InventoryTransactionDto>

    @POST("api/farms/{farmId}/inventory/{itemId}/transactions")
    suspend fun post(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("itemId") itemId: String,
        @Body input: InventoryMovementDto): InventoryItemDto

    @POST("api/farms/{farmId}/inventory/{itemId}/transactions/{transactionId}/reverse")
    suspend fun reverse(@Header("Authorization") bearer: String,
        @Path("farmId") farmId: String, @Path("itemId") itemId: String,
        @Path("transactionId") transactionId: String,
        @Body input: InventoryReversalDto): InventoryItemDto
}

