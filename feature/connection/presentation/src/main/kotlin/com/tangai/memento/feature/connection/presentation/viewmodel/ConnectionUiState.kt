package com.tangai.memento.feature.connection.presentation.viewmodel

import com.tangai.memento.domain.model.User

data class ConnectionUiState(
    val query: String = "",
    val searchResults: List<User> = emptyList(),
    val connectedUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
