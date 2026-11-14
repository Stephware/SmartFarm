package com.smartfarm.mobile.data.local

import com.google.gson.Gson
import com.smartfarm.mobile.data.local.dao.CachedResponseDao
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.repository.AuthRepository
import java.io.IOException
import java.lang.reflect.Type
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

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
    private val cacheLock = Mutex()
    private val revisions = mutableMapOf<Pair<String, String>, Long>()

    suspend fun <T> read(key: String, type: Type, fetch: suspend () -> T): CachedRead<T> {
        val userId = requireNotNull(authentication.currentUserId) { "Sign in to open cached data." }
        val resource = userId to key
        val revision = cacheLock.withLock { revisions.getOrPut(resource) { 0L } }
        val live = try {
            fetch()
        } catch (denied: HttpException) {
            if (denied.code() in setOf(401, 403, 404)) invalidate(userId, key)
            throw denied
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
        cacheLock.withLock {
            // A read started before invalidation must never restore its superseded snapshot.
            if (authentication.currentUserId == userId && revisions[resource] == revision)
                dao.save(CachedResponse(userId, key, gson.toJson(live), savedAt))
        }
        return CachedRead(live, false, savedAt)
    }

    suspend fun invalidate(key: String) {
        authentication.currentUserId?.let { invalidate(it, key) }
    }

    suspend fun invalidatePrefix(prefix: String) {
        authentication.currentUserId?.let { invalidate(it, prefix, prefix = true) }
    }

    internal suspend fun invalidate(userId: String, key: String, prefix: Boolean = false) {
        cacheLock.withLock {
            val affected = if (prefix) revisions.keys.filter { it.first == userId && it.second.startsWith(key) }
                else listOf(userId to key)
            affected.forEach { revisions[it] = (revisions[it] ?: 0L) + 1 }
            if (prefix) dao.deleteByPrefix(userId, key + "%") else dao.delete(userId, key)
        }
    }
}
