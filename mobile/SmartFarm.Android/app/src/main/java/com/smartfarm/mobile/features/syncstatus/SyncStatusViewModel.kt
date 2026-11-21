package com.smartfarm.mobile.features.syncstatus

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
        uiText(R.string.ui_retry_scheduled_this_record_is_not_confirmed_until_i_cd1f1ac, "Retry scheduled. This record is not confirmed until its sync finishes."),
        uiText(R.string.ui_could_not_schedule_the_retry_your_local_record_is_st_1953c50, "Could not schedule the retry. Your local record is still saved.")) {
        repository.retry(id)
    }
    fun discard(id: String) = act(
        uiText(R.string.ui_the_selected_unsynced_record_was_discarded_from_this_be3bd29, "The selected unsynced record was discarded from this phone."),
        uiText(R.string.ui_could_not_discard_the_record_your_local_copy_remains_ad164e1, "Could not discard the record. Your local copy remains saved.")) {
        repository.discard(id)
    }
    fun retryPhoto(id: String) = act(
        uiText(R.string.ui_photo_retry_scheduled_it_is_not_yet_confirmed_by_the_39737fd, "Photo retry scheduled. It is not yet confirmed by the server."),
        uiText(R.string.ui_could_not_schedule_the_photo_retry_your_local_photo__47a43e4, "Could not schedule the photo retry. Your local photo remains saved.")) {
        repository.retryPhoto(id)
    }
    fun discardPhoto(id: String) = act(
        uiText(R.string.ui_the_selected_unsynced_photo_was_discarded_from_this__f1f252b, "The selected unsynced photo was discarded from this phone."),
        uiText(R.string.ui_could_not_discard_the_photo_your_local_photo_remains_4216493, "Could not discard the photo. Your local photo remains saved.")) {
        repository.discardPhoto(id)
    }
    fun syncNow() {
        errorMutable.value = null
        noticeMutable.value = null
        try {
            repository.schedule()
            noticeMutable.value = uiText(R.string.ui_sync_requested_queued_records_will_upload_when_a_con_8f0fb53, "Sync requested. Queued records will upload when a connection is available.")
        } catch (_: Exception) {
            errorMutable.value = uiText(R.string.ui_could_not_schedule_sync_all_local_records_remain_sav_714d773, "Could not schedule sync. All local records remain saved.")
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
            errorMutable.value = error.message ?: uiText(R.string.ui_cannot_load_the_current_stock_balance_26a2453, "Cannot load the current stock balance.")
        }
    }

    fun approveStockRetry() = viewModelScope.launch {
        val preview = previewMutable.value ?: return@launch
        errorMutable.value = null
        try {
            repository.confirmStockRetry(preview)
            previewMutable.value = null
            noticeMutable.value = uiText(R.string.ui_stock_retry_scheduled_the_balance_remains_unconfirme_1b39009, "Stock retry scheduled. The balance remains unconfirmed until sync finishes.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            previewMutable.value = null
            errorMutable.value = error.message ?: uiText(R.string.ui_stock_changed_again_review_and_retry_7b546a7, "Stock changed again. Review and retry.")
        }
    }

    fun cancelReview() { previewMutable.value = null }

    class Factory(private val repository: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SyncStatusViewModel(repository) as T
    }
}
