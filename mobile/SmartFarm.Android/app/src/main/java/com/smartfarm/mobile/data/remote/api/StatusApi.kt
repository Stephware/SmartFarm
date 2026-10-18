package com.smartfarm.mobile.data.remote.api

import retrofit2.http.GET

data class StatusResponse(val app: String, val status: String)

interface StatusApi {
    @GET("api/status")
    suspend fun check(): StatusResponse
}

