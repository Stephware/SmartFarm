package com.smartfarm.mobile.features.syncstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.sync.StockRetryPreview
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SyncStatusViewModel(private val repository: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow<List<PendingOperation>>(emptyList())
    val operations = mutable.asStateFlow()
    private val photoMutable = MutableStateFlow<List<PendingPhoto>>(emptyList())
    val photos = photoMutable.asStateFlow()
    private val previewMutable = MutableStateFlow<StockRetryPreview?>(null)
    val stockPreview = previewMutable.asStateFlow()
    private val errorMutable = MutableStateFlow<String?>(null)
    val error = errorMutable.asStateFlow()
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
    }

    fun retry(id: String) = viewModelScope.launch { repository.retry(id) }
    fun discard(id: String) = viewModelScope.launch { repository.discard(id) }
    fun retryPhoto(id: String) = viewModelScope.launch { repository.retryPhoto(id) }
    fun discardPhoto(id: String) = viewModelScope.launch { repository.discardPhoto(id) }
    fun syncNow() = repository.schedule()

    fun reviewStock(id: String) = viewModelScope.launch {
        errorMutable.value = null
        try {
            previewMutable.value = repository.previewStockRetry(id)
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
