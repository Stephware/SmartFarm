package com.smartfarm.mobile.features.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.repository.StatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatusUiState(
    val checking: Boolean = false,
    val message: String = "Backend: not checked"
)

class StatusViewModel(private val repository: StatusRepository) : ViewModel() {
    private val mutable = MutableStateFlow(StatusUiState())
    val state = mutable.asStateFlow()

    fun check() {
        if (mutable.value.checking) return
        mutable.value = StatusUiState(checking = true, message = "Checking backend...")
        viewModelScope.launch {
            mutable.value = try {
                if (repository.backendAvailable())
                    StatusUiState(message = "Backend reachable")
                else
                    StatusUiState(message = "Unexpected backend response")
            } catch (_: Exception) {
                StatusUiState(message = "Cannot connect. Start SmartFarm.Api on port 5081.")
            }
        }
    }

    class Factory(private val repository: StatusRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatusViewModel(repository) as T
    }
}

