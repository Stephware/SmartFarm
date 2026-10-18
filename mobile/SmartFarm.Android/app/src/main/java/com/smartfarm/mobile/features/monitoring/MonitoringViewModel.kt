package com.smartfarm.mobile.features.monitoring

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.ActivityDto
import com.smartfarm.mobile.data.remote.dto.ObservationDto
import com.smartfarm.mobile.data.repository.MonitoringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class MonitoringUiState(
    val farmId: String = "",
    val cycleId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val canRecord: Boolean = false,
    val activities: List<ActivityDto> = emptyList(),
    val observations: List<ObservationDto> = emptyList(),
    val preview: Bitmap? = null
)

class MonitoringViewModel(private val repository: MonitoringRepository) : ViewModel() {
    private val mutable = MutableStateFlow(MonitoringUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = MonitoringUiState() }

    fun load(farmId: String, cycleId: String) {
        mutable.value = MonitoringUiState(farmId = farmId, cycleId = cycleId, busy = true)
        viewModelScope.launch {
            try { refresh(farmId, cycleId) }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = "Could not load monitoring history.") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    private suspend fun refresh(farmId: String, cycleId: String) {
        val data = repository.list(farmId, cycleId)
        mutable.value = mutable.value.copy(
            canRecord = data.canRecord, activities = data.activities,
            observations = data.observations)
    }

    private fun submit(action: suspend (String, String) -> Unit) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank() || !snapshot.canRecord) return
        mutable.value = snapshot.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                action(snapshot.farmId, snapshot.cycleId)
                refresh(snapshot.farmId, snapshot.cycleId)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Save failed. Check the photo, crop status and connection.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun logActivity(kind: Int, notes: String) = submit { farmId, cycleId ->
        repository.activity(farmId, cycleId, kind, notes.trim().ifBlank { null })
    }

    fun observe(health: Int, growth: String, notes: String) = submit { farmId, cycleId ->
        repository.observation(farmId, cycleId, health,
            growth.trim().ifBlank { null }, notes.trim().ifBlank { null })
    }

    fun uploadPhoto(observationId: String, uri: Uri, resolver: ContentResolver) =
        submit { farmId, cycleId ->
            val mime = requireNotNull(resolver.getType(uri))
            require(mime in setOf("image/jpeg", "image/png", "image/webp"))
            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { input ->
                    val bytes = ByteArrayOutputStream()
                    val block = ByteArray(8192)
                    while (true) {
                        val read = input.read(block)
                        if (read < 0) break
                        bytes.write(block, 0, read)
                        require(bytes.size() <= 5 * 1024 * 1024) { "Photo too large" }
                    }
                    bytes.toByteArray()
                } ?: error("Cannot open photo")
            }
            require(bytes.isNotEmpty())
            repository.upload(farmId, cycleId, observationId, bytes, mime)
        }

    fun preview(photoId: String) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val data = repository.imageBytes(snapshot.farmId, snapshot.cycleId, photoId)
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeByteArray(data, 0, data.size, options)
                        ?: error("Invalid image")
                }
                mutable.value = mutable.value.copy(preview = bitmap)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not open photo.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    class Factory(private val repository: MonitoringRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MonitoringViewModel(repository) as T
    }
}

