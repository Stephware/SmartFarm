package com.smartfarm.mobile.features.status

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.repository.StatusRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatusUiState(
    val checking: Boolean = false,
    val message: String = uiText(R.string.ui_backend_not_checked_1069b01, "Backend: not checked")
)

class StatusViewModel(private val repository: StatusRepository) : ViewModel() {
    private val mutable = MutableStateFlow(StatusUiState())
    val state = mutable.asStateFlow()

    fun check() {
        if (mutable.value.checking) return
        mutable.value = StatusUiState(checking = true, message = uiText(R.string.ui_checking_backend_21b843a, "Checking backend..."))
        viewModelScope.launch {
            mutable.value = try {
                if (repository.backendAvailable())
                    StatusUiState(message = uiText(R.string.ui_backend_reachable_d515fb8, "Backend reachable"))
                else
                    StatusUiState(message = uiText(R.string.ui_unexpected_backend_response_1b29f1b, "Unexpected backend response"))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                StatusUiState(message = uiText(R.string.ui_cannot_connect_check_your_api_connection_bd2a605, "Cannot connect. Check your API connection."))
            }
        }
    }

    class Factory(private val repository: StatusRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatusViewModel(repository) as T
    }
}

