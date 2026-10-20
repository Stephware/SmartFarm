package com.smartfarm.mobile.features.syncstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SyncStatusViewModel(private val repository: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow<List<PendingOperation>>(emptyList())
    val operations = mutable.asStateFlow()
    private var collector: Job? = null

    fun load() {
        collector?.cancel()
        collector = viewModelScope.launch {
            repository.watch().collect { mutable.value = it }
        }
    }

    fun clear() {
        collector?.cancel()
        collector = null
        mutable.value = emptyList()
    }

    fun retry(id: String) = viewModelScope.launch { repository.retry(id) }
    fun discard(id: String) = viewModelScope.launch { repository.discard(id) }
    fun syncNow() = repository.schedule()

    class Factory(private val repository: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SyncStatusViewModel(repository) as T
    }
}
