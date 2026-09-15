package com.tangai.memento.feature.connection.presentation.viewmodel

import com.tangai.memento.domain.model.User

data class ConnectionUiState(
    val redeemCode: String = "",
    val connectedUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isRedeemRunning: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
