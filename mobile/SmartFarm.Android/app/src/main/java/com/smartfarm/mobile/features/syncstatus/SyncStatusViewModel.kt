package com.smartfarm.mobile.features.syncstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.sync.StockRetryPreview
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SyncStatusViewModel(private val repository: SyncRepository) : ViewModel() {
    val progress = repository.progress
    private val mutable = MutableStateFlow<List<PendingOperation>>(emptyList())
    val operations = mutable.asStateFlow()
    private val photoMutable = MutableStateFlow<List<PendingPhoto>>(emptyList())
    val photos = photoMutable.asStateFlow()
    private val previewMutable = MutableStateFlow<StockRetryPreview?>(null)
    val stockPreview = previewMutable.asStateFlow()
    private val errorMutable = MutableStateFlow<String?>(null)
    val error = errorMutable.asStateFlow()
    private val noticeMutable = MutableStateFlow<String?>(null)
    val notice = noticeMutable.asStateFlow()
    private var collector: Job? = null

    fun load() {
        collector?.cancel()
        collector = viewModelScope.launch {
            combine(repository.watch(), repository.watchPhotos()) { ops, pictures ->
                ops to pictures
            }.collect { (ops, pictures) ->
                mutable.value = ops
                photoMutable.value = pictures
            }
        }
    }

    fun clear() {
        collector?.cancel()
        collector = null
        mutable.value = emptyList()
        photoMutable.value = emptyList()
        previewMutable.value = null
        errorMutable.value = null
        noticeMutable.value = null
    }

    private fun act(
        success: String, failure: String, action: suspend () -> Unit
    ) = viewModelScope.launch {
        errorMutable.value = null
        noticeMutable.value = null
        try {
            action()
            noticeMutable.value = success
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            errorMutable.value = failure
        }
    }

    fun retry(id: String) = act(
        "Retry scheduled. This record is not confirmed until its sync finishes.",
        "Could not schedule the retry. Your local record is still saved.") {
        repository.retry(id)
    }
    fun discard(id: String) = act(
        "The selected unsynced record was discarded from this phone.",
        "Could not discard the record. Your local copy remains saved.") {
        repository.discard(id)
    }
    fun retryPhoto(id: String) = act(
        "Photo retry scheduled. It is not yet confirmed by the server.",
        "Could not schedule the photo retry. Your local photo remains saved.") {
        repository.retryPhoto(id)
    }
    fun discardPhoto(id: String) = act(
        "The selected unsynced photo was discarded from this phone.",
        "Could not discard the photo. Your local photo remains saved.") {
        repository.discardPhoto(id)
    }
    fun syncNow() {
        errorMutable.value = null
        noticeMutable.value = null
        try {
            repository.schedule()
            noticeMutable.value = "Sync requested. Queued records will upload when a connection is available."
        } catch (_: Exception) {
            errorMutable.value = "Could not schedule sync. All local records remain saved."
        }
    }

    fun reviewStock(id: String) = viewModelScope.launch {
        errorMutable.value = null
        noticeMutable.value = null
        try {
            previewMutable.value = repository.previewStockRetry(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            errorMutable.value = error.message ?: "Cannot load the current stock balance."
        }
    }

    fun approveStockRetry() = viewModelScope.launch {
        val preview = previewMutable.value ?: return@launch
        errorMutable.value = null
        try {
            repository.confirmStockRetry(preview)
            previewMutable.value = null
            noticeMutable.value = "Stock retry scheduled. The balance remains unconfirmed until sync finishes."
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            previewMutable.value = null
            errorMutable.value = error.message ?: "Stock changed again. Review and retry."
        }
    }

    fun cancelReview() { previewMutable.value = null }

    class Factory(private val repository: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SyncStatusViewModel(repository) as T
    }
}
