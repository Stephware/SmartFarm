package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.remote.dto.AuthTokens
import com.smartfarm.mobile.data.remote.dto.LoginRequest
import com.smartfarm.mobile.data.remote.dto.RefreshRequest
import com.smartfarm.mobile.data.remote.dto.RegisterRequest
import retrofit2.HttpException
import com.google.gson.JsonParser

class FarmCodeException(message: String) : IllegalArgumentException(message)

// Tokens deliberately remain in memory until secure persisted storage is designed.
// Restarting the app requires signing in again; never save tokens in Room/plain preferences.
class AuthRepository(private val onLogin: () -> Unit = {}, private val api: () -> AuthApi) {
    @Volatile var currentUserId: String? = null
        private set
    private var session: AuthTokens? = null

    suspend fun register(name: String, email: String, password: String, confirm: String,
        joinFarm: Boolean = false, farmCode: String? = null) {
        require(!joinFarm || !farmCode.isNullOrBlank()) { "A farm code is required." }
        val response = api().register(RegisterRequest(name.trim(), email.trim(), password, confirm, joinFarm,
            farmCode?.trim().takeIf { joinFarm }))
        if (!response.isSuccessful) {
            val codeError = runCatching {
                val json = JsonParser.parseString(response.errorBody()?.string()).asJsonObject
                json.get("farmCodeError")?.takeUnless { it.isJsonNull }?.asString
                    ?: json.get("errors")?.takeIf { it.isJsonObject }?.asJsonObject
                        ?.getAsJsonArray("FarmCode")?.firstOrNull()?.asString
            }.getOrNull()
            if (joinFarm && !codeError.isNullOrBlank()) throw FarmCodeException(codeError)
            throw HttpException(response)
        }
    }

    suspend fun login(email: String, password: String): AccountResponse {
        val client = api()
        val tokens = client.login(LoginRequest(email.trim(), password))
        val account = client.account("Bearer ${tokens.accessToken}")
        session = tokens
        currentUserId = account.id
        onLogin()
        return account
    }

    suspend fun account(): AccountResponse {
        val client = api()
        var tokens = checkNotNull(session) { "Please sign in." }
        return try {
            client.account("Bearer ${tokens.accessToken}")
        } catch (exception: HttpException) {
            if (exception.code() != 401) throw exception
            tokens = client.refresh(RefreshRequest(tokens.refreshToken))
            val result = client.account("Bearer ${tokens.accessToken}")
            session = tokens
            result
        }
    }

    suspend fun <T> authorized(block: suspend (String) -> T): T {
        var active = checkNotNull(session) { "Please log in." }
        return try {
            block("Bearer ${active.accessToken}")
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val client = api()
            active = client.refresh(RefreshRequest(active.refreshToken))
            val result = block("Bearer ${active.accessToken}")
            session = active
            result
        }
    }

    fun logout() {
        currentUserId = null
        session = null
    }
}


