package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.SyncWriteRequest
import com.smartfarm.mobile.data.remote.dto.SyncWriteResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface SyncApi {
    @POST("api/sync/operations/{operationId}")
    suspend fun submit(
        @Header("Authorization") bearer: String,
        @Path("operationId") operationId: String,
        @Body input: SyncWriteRequest
    ): SyncWriteResponse
}
