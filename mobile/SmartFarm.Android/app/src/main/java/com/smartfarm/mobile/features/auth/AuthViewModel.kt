package com.smartfarm.mobile.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val account: AccountResponse? = null
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit) = run {
        if (mutableState.value.busy) return@run
        mutableState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                val user = repository.login(email, password)
                mutableState.value = AuthUiState(account = user)
                onSuccess()
            } catch (_: Exception) {
                mutableState.value = AuthUiState(error = "Sign in failed. Check your credentials and connection.")
            }
        }
    }

    fun register(fullName: String, email: String, password: String, confirmation: String, onSuccess: () -> Unit) = run {
        if (mutableState.value.busy) return@run
        mutableState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                repository.register(fullName, email, password, confirmation)
                mutableState.value = AuthUiState(notice = "Account created. You can now sign in.")
                onSuccess()
            } catch (_: Exception) {
                mutableState.value = AuthUiState(error = "Registration failed. Check your details and connection.")
            }
        }
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

