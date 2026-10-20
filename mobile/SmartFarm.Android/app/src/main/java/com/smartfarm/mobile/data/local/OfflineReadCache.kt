package com.smartfarm.mobile.data.local

import com.google.gson.Gson
import com.smartfarm.mobile.data.local.dao.CachedResponseDao
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.repository.AuthRepository
import java.io.IOException
import java.lang.reflect.Type

data class CachedRead<T>(
    val value: T,
    val offline: Boolean,
    val savedAtUtcMillis: Long
)

// Always prefer the API. Only connection failures can fall back to a prior snapshot.
// HTTP 401/403/404 and server validation errors are never replaced with cached data.
class OfflineReadCache(
    private val authentication: AuthRepository,
    private val dao: CachedResponseDao,
    private val gson: Gson = Gson()
) {
    suspend fun <T> read(key: String, type: Type, fetch: suspend () -> T): CachedRead<T> {
        val userId = requireNotNull(authentication.currentUserId) { "Sign in to open cached data." }
        val live = try {
            fetch()
        } catch (network: IOException) {
            // Do not display snapshots belonging to a different signed-in user.
            if (authentication.currentUserId != userId) throw network
            val saved = dao.find(userId, key) ?: throw network
            return try {
                val value = gson.fromJson<T>(saved.json, type) ?: throw network
                CachedRead(value, true, saved.savedAtUtcMillis)
            } catch (_: com.google.gson.JsonParseException) {
                throw network
            }
        }
        val savedAt = System.currentTimeMillis()
        if (authentication.currentUserId == userId) {
            dao.save(CachedResponse(userId, key, gson.toJson(live), savedAt))
        }
        return CachedRead(live, false, savedAt)
    }

    suspend fun invalidate(key: String) {
        authentication.currentUserId?.let { dao.delete(it, key) }
    }

    suspend fun invalidatePrefix(prefix: String) {
        authentication.currentUserId?.let { dao.deleteByPrefix(it, prefix + "%") }
    }
}
