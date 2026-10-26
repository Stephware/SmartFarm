package com.smartfarm.mobile.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.NotificationDto
import com.smartfarm.mobile.data.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskUiState(val farmId: String? = null, val busy: Boolean = false, val error: String? = null,
    val tasks: List<TaskDto> = emptyList(), val notifications: List<NotificationDto> = emptyList())

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val mutable = MutableStateFlow(TaskUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun load(farmId: String? = null) {
        job?.cancel()
        mutable.value = TaskUiState(farmId = farmId)
        request(farmId)
    }

    private fun request(farmId: String?, change: suspend () -> Unit = {}) {
        mutable.value = mutable.value.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                change()
                mutable.value = if (farmId == null) TaskUiState(notifications = repository.notifications())
                    else TaskUiState(farmId = farmId, tasks = repository.tasks(farmId))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.value = mutable.value.copy(busy = false,
                    error = "Could not refresh these records. Check your connection and try again before repeating an update.")
            }
        }
    }

    fun complete(task: TaskDto) {
        if (!mutable.value.busy) request(task.farmId) { repository.complete(task) }
    }
    fun read(id: String) {
        if (!mutable.value.busy) request(null) { repository.read(id) }
    }
    fun clear() { job?.cancel(); mutable.value = TaskUiState() }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TaskViewModel(repository) as T
    }
}
