package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.feature.auth.domain.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val  authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAccountChanged(account: String) {
        _uiState.value = _uiState.value.copy(account = account, errorMessage = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onPasswordVisibilityToggle() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun onLoginClick(onSuccess: () -> Unit) {
        val account = _uiState.value.account.trim()
        val password = _uiState.value.password

        if (account.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter both email and password"
            )
            return
        }

        if (!account.contains("@")) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter the email used for Firebase account."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(account, password)
            if (result.isSuccess) {
                authRepository.syncUsers().onFailure {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Failed to sync users"
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
            if (result.isSuccess) {
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }
}
