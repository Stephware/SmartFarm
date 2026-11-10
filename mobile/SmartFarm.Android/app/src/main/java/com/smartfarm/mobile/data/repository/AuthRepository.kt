package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.remote.dto.AuthTokens
import com.smartfarm.mobile.data.remote.dto.LoginRequest
import com.smartfarm.mobile.data.remote.dto.RefreshRequest
import com.smartfarm.mobile.data.remote.dto.RegisterRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

// Tokens remain in memory. Restarting the app requires signing in again.
// A mutex serializes refresh; the generation prevents any in-flight request
// from reviving credentials or returning private data after logout/account switch.
class AuthRepository(private val onLogin: () -> Unit = {}, private val api: () -> AuthApi) {
    private val sessionLock = Any()
    private val refreshMutex = Mutex()
    private var session: AuthTokens? = null
    private var generation = 0L

    @Volatile var currentUserId: String? = null
        private set

    private data class SessionSnapshot(val tokens: AuthTokens, val generation: Long)

    private fun activeSession(): SessionSnapshot = synchronized(sessionLock) {
        SessionSnapshot(checkNotNull(session) { "Please sign in." }, generation)
    }

    private fun checkSession(snapshot: SessionSnapshot) = synchronized(sessionLock) {
        check(generation == snapshot.generation && session != null) {
            "Session changed. Please sign in again."
        }
    }

    suspend fun register(name: String, email: String, password: String) {
        val response = api().register(RegisterRequest(name.trim(), email.trim(), password))
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun login(email: String, password: String): AccountResponse {
        val startingGeneration = synchronized(sessionLock) { generation }
        val client = api()
        val tokens = client.login(LoginRequest(email.trim(), password))
        val account = client.account("Bearer ${tokens.accessToken}")
        val committedGeneration = synchronized(sessionLock) {
            check(generation == startingGeneration) {
                "Session changed while signing in. Please try again."
            }
            session = tokens
            currentUserId = account.id
            ++generation
        }
        // Do not trigger login work if another thread has already logged out.
        if (synchronized(sessionLock) { generation == committedGeneration && session != null }) {
            onLogin()
        }
        return account
    }

    suspend fun account(): AccountResponse = authorized { api().account(it) }

    /**
     * A 401 can occur for multiple simultaneous requests using the old token.
     * Only the first one rotates it; waiting callers reuse the newest token.
     */
    private suspend fun refreshAfterUnauthorized(original: SessionSnapshot): SessionSnapshot =
        refreshMutex.withLock {
            val active = activeSession()
            check(active.generation == original.generation) {
                "Session changed. Please sign in again."
            }
            if (active.tokens != original.tokens) return@withLock active
            val fresh = api().refresh(RefreshRequest(active.tokens.refreshToken))
            synchronized(sessionLock) {
                check(generation == original.generation && session == active.tokens) {
                    "Session changed. Please sign in again."
                }
                session = fresh
            }
            SessionSnapshot(fresh, original.generation)
        }

    suspend fun <T> authorized(block: suspend (String) -> T): T {
        val original = activeSession()
        val result = try {
            block("Bearer ${original.tokens.accessToken}")
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val refreshed = refreshAfterUnauthorized(original)
            // Logout after refresh must prevent even a retry with the old account.
            checkSession(refreshed)
            val retried = block("Bearer ${refreshed.tokens.accessToken}")
            checkSession(refreshed)
            return retried
        }
        checkSession(original)
        return result
    }

    fun logout() = synchronized(sessionLock) {
        ++generation
        session = null
        currentUserId = null
    }
}
