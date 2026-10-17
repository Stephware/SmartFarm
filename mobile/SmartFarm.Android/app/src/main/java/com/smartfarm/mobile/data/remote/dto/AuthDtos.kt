package com.smartfarm.mobile.data.remote.dto

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)

data class RefreshRequest(val refreshToken: String)

data class AuthTokens(
    val tokenType: String,
    val accessToken: String,
    val expiresIn: Long,
    val refreshToken: String
)

data class AccountResponse(val id: String, val fullName: String, val email: String)

