package com.tangai.memento.feature.auth.login

data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
