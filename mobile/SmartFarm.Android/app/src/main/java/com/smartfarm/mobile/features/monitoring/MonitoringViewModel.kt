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
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import com.smartfarm.mobile.data.remote.dto.ActivityStockInputDto
import java.math.BigDecimal
import com.smartfarm.mobile.data.sync.SyncRepository
import com.smartfarm.mobile.data.remote.dto.ActivityInputDto
import com.smartfarm.mobile.data.remote.dto.ObservationInputDto
import com.smartfarm.mobile.data.remote.dto.ObservationFollowUpInputDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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
    val cachedAtUtcMillis: Long? = null,
    val supplies: List<InventoryItemDto> = emptyList(),
    val suppliesOffline: Boolean = false,
    val supplyError: String? = null
)

class MonitoringViewModel(private val repository: MonitoringRepository,
    private val sync: SyncRepository,
    private val inventory: InventoryRepository) : ViewModel() {
    private val mutable = MutableStateFlow(MonitoringUiState())
    val state = mutable.asStateFlow()
    // Bounded in-memory cache for already-authorized private photo responses.
    // No persistent disk cache; clear it when the user logs out.
    private val photoCache = object : LruCache<String, ByteArray>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }
    // Thumbnails are small and memory-only; keep network downloads bounded.
    private val thumbnailCache = object : LruCache<String, ByteArray>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }
    private val thumbnailRequests = Semaphore(2)
    private var previewJob: Job? = null
    private var loadJob: Job? = null

    fun clear() {
        previewJob?.cancel()
        previewJob = null
        loadJob?.cancel()
        loadJob = null
        photoCache.evictAll()
        thumbnailCache.evictAll()
        mutable.value = MonitoringUiState()
    }

    override fun onCleared() {
        clear()
        super.onCleared()
    }

    fun load(farmId: String, cycleId: String) {
        previewJob?.cancel()
        loadJob?.cancel()
        mutable.value = if (mutable.value.farmId == farmId && mutable.value.cycleId == cycleId)
            mutable.value.copy(busy = true, error = null, notice = null) else MonitoringUiState(
            farmId = farmId, cycleId = cycleId, busy = true,
            saveReceipts = mutable.value.saveReceipts)
        loadJob = viewModelScope.launch {
            try {
                refresh(farmId, cycleId)
                try {
                    val stock = inventory.listSnapshot(farmId)
                    if (mutable.value.farmId == farmId && mutable.value.cycleId == cycleId) {
                        mutable.value = mutable.value.copy(
                            supplies = stock.value.items.filter { it.archivedAtUtc == null },
                            suppliesOffline = stock.offline, supplyError = null)
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    if (error is HttpException && error.code() in setOf(401, 403, 404)) throw error
                    if (mutable.value.farmId == farmId && mutable.value.cycleId == cycleId)
                        mutable.value = mutable.value.copy(
                            supplyError = "Supplies unavailable. You can still log an activity without stock.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (mutable.value.farmId == farmId && mutable.value.cycleId == cycleId) {
                    if (error is HttpException && error.code() in setOf(401, 403, 404)) {
                        photoCache.evictAll()
                        thumbnailCache.evictAll()
                        mutable.value = mutable.value.copy(canRecord = false, activities = emptyList(),
                            observations = emptyList(), supplies = emptyList(), preview = null,
                            previewPhotoId = null, offline = false, cachedAtUtcMillis = null, supplyError = null)
                    }
                    mutable.value = mutable.value.copy(error = "Could not load monitoring history.")
                }
            } finally {
                if (mutable.value.farmId == farmId && mutable.value.cycleId == cycleId) {
                    mutable.value = mutable.value.copy(busy = false)
                }
            }
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(error = "Save failed. Check the values or device storage.")
            } finally {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun logActivity(kind: Int, notes: String, taskId: String? = null) = submit("activity") { farmId, cycleId ->
        sync.queueActivity(farmId, cycleId, ActivityInputDto(kind, notes.trim().ifBlank { null }, taskId))
    }

    fun logActivityWithStock(
        kind: Int, notes: String, item: InventoryItemDto, quantity: String, taskId: String? = null
    ) = submit("activity_stock") { farmId, cycleId ->
        val used = quantity.trim().toBigDecimal()
        require(kind in 2..3 &&
            item.category == if (kind == 2) 2 else 3)
        require(item.farmId == farmId && item.archivedAtUtc == null &&
            used > BigDecimal.ZERO && used.scale() <= 3 &&
            used <= item.quantityOnHand && item.rowVersion.isNotBlank())
        sync.queueActivityWithStock(farmId, cycleId, ActivityStockInputDto(
            ActivityInputDto(kind, notes.trim().ifBlank { null }, taskId),
            inventoryItemId = item.id, quantityUsed = used,
            rowVersion = item.rowVersion
        ))
    }

    fun observe(health: Int, growth: String, notes: String,
        uri: Uri?, resolver: ContentResolver) =
        observeIssue(health, growth, notes, 0, 0, uri, resolver)

    fun observeIssue(health: Int, growth: String, notes: String,
        category: Int, severity: Int, uri: Uri?, resolver: ContentResolver) =
        submit("observation") { farmId, cycleId ->
            require(category in 0..5 && severity in 0..3 &&
                (category == 0 && severity == 0 ||
                    category > 0 && severity > 0 && health in 2..3))
            val input = ObservationInputDto(health,
                growth.trim().ifBlank { null }, notes.trim().ifBlank { null },
                category, severity)
            if (uri == null) sync.queueObservation(farmId, cycleId, input)
            else sync.queueObservationWithPhoto(farmId, cycleId, input, uri, resolver)
        }

    // Follow-up changes are online-only. Do not turn them into unverified offline claims.
    fun updateFollowUp(observation: ObservationDto, status: Int, taskId: String?) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.offline || snapshot.farmId.isBlank() ||
            status !in 1..2 || observation.issueCategory == 0 ||
            observation.rowVersion.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                repository.followUp(snapshot.farmId, snapshot.cycleId, observation.id,
                    ObservationFollowUpInputDto(status, taskId, observation.rowVersion))
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId) {
                    refresh(snapshot.farmId, snapshot.cycleId)
                    mutable.value = mutable.value.copy(
                        notice = "Follow-up saved on the server.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(
                        error = "Follow-up could not be confirmed. Refresh before retrying.")
            } finally {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    // Attach to an existing observation even without a connection.
    fun uploadPhoto(observationId: String, uri: Uri, resolver: ContentResolver) =
        submit("photo") { farmId, cycleId ->
            sync.queuePhoto(farmId, cycleId, observationId, uri, resolver)
        }

    suspend fun thumbnail(photoId: String): ByteArray {
        val snapshot = mutable.value
        require(snapshot.farmId.isNotBlank() && snapshot.cycleId.isNotBlank())
        thumbnailCache.get(photoId)?.let { return it }

        return thumbnailRequests.withPermit {
            thumbnailCache.get(photoId)?.let { return@withPermit it }
            val bytes = photoCache.get(photoId) ?: withContext(Dispatchers.IO) {
                repository.imageBytes(snapshot.farmId, snapshot.cycleId, photoId)
            }
            val small = withContext(Dispatchers.Default) {
                PhotoThumbnailFactory.create(bytes)
            }
            // The composable is cancelled when scrolled away; never cache across logout.
            currentCoroutineContext().ensureActive()
            if (mutable.value.farmId != snapshot.farmId ||
                mutable.value.cycleId != snapshot.cycleId) {
                throw CancellationException("Monitoring context changed.")
            }
            if (bytes.size <= 5 * 1024 * 1024) photoCache.put(photoId, bytes)
            thumbnailCache.put(photoId, small)
            small
        }
    }

    fun dismissPreview() {
        previewJob?.cancel()
        previewJob = null
        if (mutable.value.previewPhotoId != null) {
            mutable.value = mutable.value.copy(
                preview = null, previewPhotoId = null, busy = false, error = null)
        }
    }

    fun preview(photoId: String) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        previewJob?.cancel()
        mutable.value = snapshot.copy(busy = true, error = null,
            preview = null, previewPhotoId = photoId)
        previewJob = viewModelScope.launch {
            try {
                // Retrofit authenticates the download; Glide decodes/cache-displays its bytes.
                val bytes = photoCache.get(photoId) ?: withContext(Dispatchers.IO) {
                    repository.imageBytes(snapshot.farmId, snapshot.cycleId, photoId)
                }
                if (!isActive || mutable.value.farmId != snapshot.farmId ||
                    mutable.value.cycleId != snapshot.cycleId ||
                    mutable.value.previewPhotoId != photoId) return@launch

                photoCache.put(photoId, bytes)
                mutable.value = mutable.value.copy(preview = bytes)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (mutable.value.farmId == snapshot.farmId &&
                    mutable.value.cycleId == snapshot.cycleId &&
                    mutable.value.previewPhotoId == photoId) {
                    mutable.value = mutable.value.copy(error = "Could not open photo.")
                }
            } finally {
                if (mutable.value.farmId == snapshot.farmId &&
                    mutable.value.cycleId == snapshot.cycleId &&
                    mutable.value.previewPhotoId == photoId) {
                    mutable.value = mutable.value.copy(busy = false)
                }
            }
        }
    }

    class Factory(private val repository: MonitoringRepository,
        private val sync: SyncRepository,
        private val inventory: InventoryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MonitoringViewModel(repository, sync, inventory) as T
    }
}

