package com.smartfarm.mobile.data.remote.api

import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.remote.dto.AuthTokens
import com.smartfarm.mobile.data.remote.dto.LoginRequest
import com.smartfarm.mobile.data.remote.dto.RefreshRequest
import com.smartfarm.mobile.data.remote.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthTokens

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthTokens

    @GET("api/auth/me")
    suspend fun account(@Header("Authorization") bearer: String): AccountResponse
}

