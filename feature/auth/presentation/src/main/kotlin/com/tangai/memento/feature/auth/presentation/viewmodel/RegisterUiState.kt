package com.tangai.memento.feature.auth.presentation.viewmodel

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val isRegisterSuccess: Boolean = false
)

sealed interface RegisterEffect {
    data object NavigateToHome : RegisterEffect
}
