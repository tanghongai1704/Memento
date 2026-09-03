package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.ConnectionRequest
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState(isLoading = true))
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        loadConnections()
        loadPendingRequests()
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun searchUsers() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                searchState = ScreenState.Empty
            )
            return
        }
        _uiState.value = _uiState.value.copy(searchState = ScreenState.Loading, errorMessage = null)
        viewModelScope.launch {
            connectionRepository.searchUsers(query).fold(
                onSuccess = { users ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = users,
                        searchState = if (users.isEmpty()) ScreenState.Empty else ScreenState.Success
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        searchResults = emptyList(),
                        searchState = ScreenState.Error,
                        errorMessage = it.message ?: "Search failed"
                    )
                }
            )
        }
    }

    fun sendConnectionRequest(receiverId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(requestState = ScreenState.Loading, errorMessage = null)
            connectionRepository.sendConnectionRequest(receiverId, null).fold(
                onSuccess = {
                    loadPendingRequests()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        requestState = ScreenState.Error,
                        errorMessage = it.message ?: "Failed to send request"
                    )
                }
            )
        }
    }

    fun acceptConnectionRequest(requestId: String) {
        viewModelScope.launch {
            connectionRepository.acceptConnectionRequest(requestId)
            loadConnections()
            loadPendingRequests()
        }
    }

    fun rejectConnectionRequest(requestId: String) {
        viewModelScope.launch {
            connectionRepository.rejectConnectionRequest(requestId)
            loadPendingRequests()
        }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionState = ScreenState.Loading)
            val result = connectionRepository.getCurrentUserConnections()
            result.fold(
                onSuccess = { connections ->
                    _uiState.value = _uiState.value.copy(
                        connectedUsers = connections.flatMap { connection ->
                            connection.members.mapNotNull { member ->
                                if (member.userId == connection.createdBy) null else User(member.userId, member.userId)
                            }
                        }.distinctBy(User::id),
                        connectionState = if (connections.isEmpty()) ScreenState.Empty else ScreenState.Success,
                        isLoading = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        connectionState = ScreenState.Error,
                        errorMessage = it.message ?: "Failed to load connections",
                        isLoading = false
                    )
                }
            )
        }
    }

    private fun loadPendingRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(requestState = ScreenState.Loading)
            val result = connectionRepository.getPendingRequests()
            result.fold(
                onSuccess = { requests ->
                    _uiState.value = _uiState.value.copy(
                        pendingRequests = requests,
                        incomingRequests = requests,
                        requestState = if (requests.isEmpty()) ScreenState.Empty else ScreenState.Success
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        requestState = ScreenState.Error,
                        errorMessage = it.message ?: "Failed to load requests"
                    )
                }
            )
        }
    }
}
