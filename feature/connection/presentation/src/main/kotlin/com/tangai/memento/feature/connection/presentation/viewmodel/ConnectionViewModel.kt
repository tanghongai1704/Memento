package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
feeatimport com.tangai.memento.domain.model.User
import com.tangai.memento.feature.auth.domain.AuthRepository
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState(isLoading = true))
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()
    private var usersBootstrapped = false

    init {
        bootstrapUsersThenLoad()
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
                        userLookup = _uiState.value.userLookup + users.associateBy(User::id),
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
                    loadSentPendingRequests()
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
            connectionRepository.acceptConnectionRequest(requestId).fold(
                onSuccess = {
                    loadConnections()
                    loadPendingRequests()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Failed to accept request"
                    )
                }
            )
        }
    }

    fun rejectConnectionRequest(requestId: String) {
        viewModelScope.launch {
            connectionRepository.rejectConnectionRequest(requestId).fold(
                onSuccess = {
                    loadPendingRequests()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Failed to reject request"
                    )
                }
            )
        }
    }

    private fun loadConnections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(connectionState = ScreenState.Loading)
            val result = connectionRepository.getCurrentUserConnections()
            result.fold(
                onSuccess = { connections ->
                    val memberIds = connections.flatMap { it.members.map { m -> m.userId } }.distinct()
                    val missingIds = memberIds.filterNot { _uiState.value.userLookup.containsKey(it) }
                    
                    val users = connections.flatMap { connection ->
                        connection.members.mapNotNull { member ->
                            _uiState.value.userLookup[member.userId]
                        }
                    }.distinctBy(User::id)
                    
                    _uiState.value = _uiState.value.copy(
                        connectedUsers = users,
                        userLookup = _uiState.value.userLookup + users.associateBy(User::id),
                        connectionState = if (connections.isEmpty()) ScreenState.Empty else ScreenState.Success,
                        isLoading = false
                    )
                    
                    if (missingIds.isNotEmpty()) {
                        loadMissingUsers(missingIds)
                    }
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
                    val userIds = requests.flatMap { listOf(it.senderId, it.receiverId) }
                    val missingIds = userIds.filterNot { _uiState.value.userLookup.containsKey(it) }
                    
                    val lookupUpdates = userIds.associateWith { id ->
                        _uiState.value.userLookup[id] ?: User(id, id)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        pendingRequests = requests,
                        incomingRequests = requests,
                        userLookup = _uiState.value.userLookup + lookupUpdates,
                        requestState = if (requests.isEmpty()) ScreenState.Empty else ScreenState.Success
                    )
                    
                    if (missingIds.isNotEmpty()) {
                        loadMissingUsers(missingIds)
                    }
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

    private fun loadSentPendingRequests() {
        viewModelScope.launch {
            connectionRepository.getSentPendingRequests().fold(
                onSuccess = { requests ->
                    val lookupUpdates = requests.flatMap {
                        listOf(it.senderId, it.receiverId)
                    }.associateWith { id ->
                        _uiState.value.userLookup[id] ?: User(id, id)
                    }
                    _uiState.value = _uiState.value.copy(
                        sentPendingRequests = requests,
                        userLookup = _uiState.value.userLookup + lookupUpdates
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Failed to load sent requests"
                    )
                }
            )
        }
    }

    private fun bootstrapUsersThenLoad() {
        viewModelScope.launch {
            if (!usersBootstrapped) {
                _uiState.value = _uiState.value.copy(isBootstrappingUsers = true)
                authRepository.syncUsers().onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to sync users: ${error.message ?: "Unknown error"}"
                    )
                }
                usersBootstrapped = true
                _uiState.value = _uiState.value.copy(isBootstrappingUsers = false)
            }
            loadConnections()
            loadPendingRequests()
            loadSentPendingRequests()
        }
    }

    private fun loadMissingUsers(userIds: List<String>) {
        viewModelScope.launch {
            connectionRepository.searchUsers("").fold(
                onSuccess = { allUsers ->
                    val usersToAdd = allUsers.filter { userIds.contains(it.id) }
                    if (usersToAdd.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            userLookup = _uiState.value.userLookup + usersToAdd.associateBy(User::id)
                        )
                    }
                },
                onFailure = {}
            )
        }
    }
}
