package com.tangai.memento.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.auth.domain.model.ValidationResult
import com.tangai.memento.feature.auth.domain.usecase.ValidateEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSent: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val validateEmail: ValidateEmailUseCase
) : ViewModel() {
    private val state = MutableStateFlow(
        ForgotPasswordUiState(email = repository.currentUserEmail().orEmpty())
    )
    val uiState = state.asStateFlow()

    fun onEmailChanged(email: String) {
        state.value = state.value.copy(
            email = email,
            emailError = null,
            errorMessage = null,
            isSent = false
        )
    }

    fun sendResetEmail() {
        val email = state.value.email.trim()
        val validation = validateEmail(email)
        if (validation is ValidationResult.Invalid) {
            state.value = state.value.copy(emailError = validation.reason)
            return
        }
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, errorMessage = null)
            repository.sendPasswordResetEmail(email).fold(
                onSuccess = {
                    state.value = state.value.copy(isLoading = false, isSent = true)
                },
                onFailure = { error ->
                    state.value = state.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not send reset email."
                    )
                }
            )
        }
    }
}
