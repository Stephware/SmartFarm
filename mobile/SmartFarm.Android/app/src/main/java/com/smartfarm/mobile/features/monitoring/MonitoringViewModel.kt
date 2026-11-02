package com.smartfarm.mobile.features.monitoring

import android.content.ContentResolver
import android.util.LruCache
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.ActivityDto
import com.smartfarm.mobile.data.remote.dto.ObservationDto
import com.smartfarm.mobile.data.repository.MonitoringRepository
import com.smartfarm.mobile.data.sync.SyncRepository
import com.smartfarm.mobile.data.remote.dto.ActivityInputDto
import com.smartfarm.mobile.data.remote.dto.ObservationInputDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MonitoringUiState(
    val farmId: String = "",
    val cycleId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val saveReceipts: Map<String, String> = emptyMap(),
    val canRecord: Boolean = false,
    val activities: List<ActivityDto> = emptyList(),
    val observations: List<ObservationDto> = emptyList(),
    val preview: ByteArray? = null,
    val previewPhotoId: String? = null,
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class MonitoringViewModel(private val repository: MonitoringRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(MonitoringUiState())
    val state = mutable.asStateFlow()
    // Bounded in-memory cache for already-authorized private photo responses.
    // No persistent disk cache; clear it when the user logs out.
    private val photoCache = object : LruCache<String, ByteArray>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    fun clear() {
        photoCache.evictAll()
        mutable.value = MonitoringUiState()
    }

    fun load(farmId: String, cycleId: String) {
        mutable.value = MonitoringUiState(farmId = farmId, cycleId = cycleId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try { refresh(farmId, cycleId) }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = "Could not load monitoring history.") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    private suspend fun refresh(farmId: String, cycleId: String) {
        val snapshot = repository.listSnapshot(farmId, cycleId)
        val data = snapshot.value
        mutable.value = mutable.value.copy(
            canRecord = data.canRecord, activities = data.activities,
            observations = data.observations, offline = snapshot.offline,
            cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
    }

    private fun submit(form: String, action: suspend (String, String) -> String) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank() || !snapshot.canRecord) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = action(snapshot.farmId, snapshot.cycleId)
                val current = mutable.value
                mutable.value = current.copy(saveReceipts = current.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.cycleId}/$form" to id))
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(notice = "Saved on this device. It will sync when online.")
            } catch (_: Exception) {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(error = "Save failed. Check the values or device storage.")
            } finally {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun logActivity(kind: Int, notes: String) = submit("activity") { farmId, cycleId ->
        sync.queueActivity(farmId, cycleId, ActivityInputDto(kind, notes.trim().ifBlank { null }))
    }

    fun observe(health: Int, growth: String, notes: String,
        uri: Uri?, resolver: ContentResolver) = submit("observation") { farmId, cycleId ->
        val input = ObservationInputDto(health,
            growth.trim().ifBlank { null }, notes.trim().ifBlank { null })
        if (uri == null) sync.queueObservation(farmId, cycleId, input)
        else sync.queueObservationWithPhoto(farmId, cycleId, input, uri, resolver)
    }

    // Attach to an existing observation even without a connection.
    fun uploadPhoto(observationId: String, uri: Uri, resolver: ContentResolver) =
        submit("photo") { farmId, cycleId ->
            sync.queuePhoto(farmId, cycleId, observationId, uri, resolver)
        }

    fun preview(photoId: String) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null,
            preview = null, previewPhotoId = null)
        viewModelScope.launch {
            try {
                // Authenticated Retrofit download; Glide handles decoding and bitmap cache.
                val bytes = photoCache.get(photoId) ?: withContext(Dispatchers.IO) {
                    repository.imageBytes(snapshot.farmId, snapshot.cycleId, photoId)
                }.also { photoCache.put(photoId, it) }
                mutable.value = mutable.value.copy(preview = bytes, previewPhotoId = photoId)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not open photo.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    class Factory(private val repository: MonitoringRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MonitoringViewModel(repository, sync) as T
    }
}

