package com.tangai.memento.feature.auth.presentation.viewmodel

data class SignupUiState(
    val username: String = "",
    val account: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
