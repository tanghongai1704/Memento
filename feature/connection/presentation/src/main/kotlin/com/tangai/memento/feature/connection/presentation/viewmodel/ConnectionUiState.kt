package com.tangai.memento.feature.connection.presentation.viewmodel

import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.ConnectionRequest
import com.tangai.memento.domain.model.User

data class ConnectionUiState(
    val query: String = "",
    val searchResults: List<User> = emptyList(),
    val connectedUsers: List<User> = emptyList(),
    val pendingRequests: List<ConnectionRequest> = emptyList(),
    val incomingRequests: List<ConnectionRequest> = emptyList(),
    val sentPendingRequests: List<ConnectionRequest> = emptyList(),
    val userLookup: Map<String, User> = emptyMap(),
    val selectedConnection: Connection? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchState: ScreenState = ScreenState.Empty,
    val connectionState: ScreenState = ScreenState.Empty,
    val requestState: ScreenState = ScreenState.Empty
)

enum class ScreenState {
    Loading,
    Empty,
    Success,
    Error
}
