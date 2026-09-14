package com.tangai.memento.feature.connection.presentation.viewmodel

import com.tangai.memento.domain.model.User

data class ConnectionUiState(
    val inviteCode: String? = null,
    val inviteExpiresAtMillis: Long? = null,
    val redeemCode: String = "",
    val connectedUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isInviteActionRunning: Boolean = false,
    val isRedeemRunning: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
