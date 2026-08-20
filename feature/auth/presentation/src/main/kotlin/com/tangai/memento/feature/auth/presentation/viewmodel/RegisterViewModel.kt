package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.feature.auth.domain.model.RegisterException
import com.tangai.memento.feature.auth.domain.model.ValidationResult
import com.tangai.memento.feature.auth.domain.usecase.SignUpUseCase
import com.tangai.memento.feature.auth.domain.usecase.ValidateRegisterInputUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val validateRegisterInputUseCase: ValidateRegisterInputUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<RegisterEffect>()
    val effect: SharedFlow<RegisterEffect> = _effect.asSharedFlow()

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null,
            generalError = null
        )
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            generalError = null
        )
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = null,
            generalError = null
        )
    }

    fun onRegisterClick() {
        val state = _uiState.value
        val validation = validateRegisterInputUseCase(
            email = state.email.trim(),
            password = state.password,
            confirmPassword = state.confirmPassword
        )
        if (!validation.isValid) {
            _uiState.value = state.copy(
                emailError = (validation.email as? ValidationResult.Invalid)?.reason,
                passwordError = (validation.password as? ValidationResult.Invalid)?.reason,
                confirmPasswordError = (validation.confirmPassword as? ValidationResult.Invalid)?.reason
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                generalError = null,
                isRegisterSuccess = false
            )
            val result = signUpUseCase(state.email.trim(), state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegisterSuccess = true
                    )
                    _effect.emit(RegisterEffect.NavigateToHome)
                },
                onFailure = { throwable ->
                    val errorMessage = if (throwable is RegisterException) {
                        throwable.error.message
                    } else {
                        throwable.message ?: "Register failed."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = errorMessage
                    )
                }
            )
        }
    }
}
