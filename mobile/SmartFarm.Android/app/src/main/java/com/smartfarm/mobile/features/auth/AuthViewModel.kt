package com.smartfarm.mobile.features.auth

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.NetworkErrorMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val account: AccountResponse? = null,
    val recoveryBusy: Boolean = false,
    val recoveryRequested: Boolean = false,
    val recoveryError: String? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                val user = repository.login(email, password)
                mutableState.value = AuthUiState(account = user)
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = AuthUiState(error = NetworkErrorMessages.forLogin(error))
            }
        }
    }

    fun register(fullName: String, email: String, password: String, onSuccess: () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                repository.register(fullName, email, password)
                mutableState.value = AuthUiState(notice = uiText(R.string.ui_account_created_you_can_now_sign_in_3631ebf, "Account created. You can now sign in."))
                onSuccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = AuthUiState(error = NetworkErrorMessages.forRegistration(error))
            }
        }
    }

    fun requestPasswordReset(email: String) {
        if (mutableState.value.recoveryBusy || mutableState.value.recoveryRequested) return
        mutableState.value = mutableState.value.copy(
            recoveryBusy = true, recoveryError = null, recoveryRequested = false
        )
        viewModelScope.launch {
            try {
                repository.requestPasswordReset(email)
                mutableState.value = mutableState.value.copy(
                    recoveryBusy = false, recoveryRequested = true, recoveryError = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    recoveryBusy = false,
                    recoveryError = NetworkErrorMessages.forRequest(
                        error, uiText(R.string.ui_please_try_again_later_02e8cd8, "Please try again later."))
                )
            }
        }
    }

    fun clearRecoveryFeedback() {
        mutableState.value = mutableState.value.copy(
            recoveryBusy = false, recoveryRequested = false, recoveryError = null)
    }

    fun logout() {
        repository.logout()
        mutableState.value = AuthUiState()
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
    }
}

