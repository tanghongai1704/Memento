package com.tangai.memento.feature.connection.presentation.viewmodel

import com.tangai.memento.domain.model.User

data class ConnectionUiState(
    val redeemCode: String = "",
    val connectedUsers: List<User> = emptyList(),
    val connectionIdsByUserId: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isRedeemRunning: Boolean = false,
    val disconnectTargetUserId: String? = null,
    val disconnectingUserId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
