package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.domain.model.User
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
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ConnectionUiState(isLoading = true))

    val uiState: StateFlow<ConnectionUiState> =
        _uiState.asStateFlow()

    private var activeUserId: String? = null

    private var authInitialized = false

    private val authStateListener =
        FirebaseAuth.AuthStateListener { auth ->

            val newUserId = auth.currentUser?.uid

            if (!authInitialized || newUserId != activeUserId) {

                authInitialized = true
                activeUserId = newUserId

                _uiState.value = ConnectionUiState(
                    isLoading = newUserId != null
                )

                if (newUserId != null) {
                    bootstrapUsersThenLoad(newUserId)
                }
            }
        }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    fun onQueryChanged(query: String) {
        _uiState.value =
            _uiState.value.copy(query = query)
    }

    fun searchUsers() {

        val query =
            _uiState.value.query.trim()

        if (query.isEmpty()) {
            _uiState.value =
                _uiState.value.copy(
                    searchResults = emptyList(),
                    searchState = ScreenState.Empty
                )
            return
        }

        val currentUserId =
            firebaseAuth.currentUser?.uid
                ?: return

        _uiState.value =
            _uiState.value.copy(
                searchState = ScreenState.Loading,
                errorMessage = null
            )

        viewModelScope.launch {

            connectionRepository
                .searchUsers(query)
                .fold(

                    onSuccess = { users ->

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            val connectedIds =
                                _uiState.value
                                    .connectedUsers
                                    .map { it.id }
                                    .toSet()

                            val filteredUsers =
                                users.filterNot { user ->

                                    user.id == currentUserId ||
                                            user.id in connectedIds
                                }

                            _uiState.value =
                                _uiState.value.copy(

                                    searchResults = filteredUsers,

                                    userLookup =
                                        _uiState.value.userLookup +
                                                filteredUsers.associateBy(User::id),

                                    searchState =
                                        if (filteredUsers.isEmpty()) {
                                            ScreenState.Empty
                                        } else {
                                            ScreenState.Success
                                        }
                                )
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    searchResults = emptyList(),
                                    searchState = ScreenState.Error,
                                    errorMessage =
                                        error.message ?: "Search failed"
                                )
                        }
                    }
                )
        }
    }

    fun sendConnectionRequest(receiverId: String) {

        val currentUserId =
            firebaseAuth.currentUser?.uid
                ?: return

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    requestState = ScreenState.Loading,
                    errorMessage = null
                )

            connectionRepository
                .sendConnectionRequest(receiverId, null)
                .fold(

                    onSuccess = {

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            loadPendingRequests(currentUserId)
                            loadSentPendingRequests(currentUserId)
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    requestState = ScreenState.Error,
                                    errorMessage =
                                        error.message
                                            ?: "Failed to send request"
                                )
                        }
                    }
                )
        }
    }

    fun acceptConnectionRequest(requestId: String) {

        val currentUserId =
            firebaseAuth.currentUser?.uid
                ?: return

        viewModelScope.launch {

            connectionRepository
                .acceptConnectionRequest(requestId)
                .fold(

                    onSuccess = {

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            loadConnections(currentUserId)
                            loadPendingRequests(currentUserId)
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    errorMessage =
                                        error.message
                                            ?: "Failed to accept request"
                                )
                        }
                    }
                )
        }
    }


    fun rejectConnectionRequest(requestId: String) {

        val currentUserId =
            firebaseAuth.currentUser?.uid
                ?: return

        viewModelScope.launch {

            connectionRepository
                .rejectConnectionRequest(requestId)
                .fold(

                    onSuccess = {

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            loadPendingRequests(currentUserId)
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == currentUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    errorMessage =
                                        error.message
                                            ?: "Failed to reject request"
                                )
                        }
                    }
                )
        }
    }

    private fun loadConnections(expectedUserId: String) {

        viewModelScope.launch {

            if (firebaseAuth.currentUser?.uid != expectedUserId) {
                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    connectionState = ScreenState.Loading
                )

            connectionRepository
                .loadConnections()
                .fold(

                    onSuccess = { users ->

                        if (firebaseAuth.currentUser?.uid == expectedUserId) {

                            _uiState.value =
                                _uiState.value.copy(

                                    connectedUsers = users,

                                    userLookup =
                                        _uiState.value.userLookup +
                                                users.associateBy(User::id),

                                    connectionState =
                                        if (users.isEmpty()) {
                                            ScreenState.Empty
                                        } else {
                                            ScreenState.Success
                                        },

                                    isLoading = false
                                )
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == expectedUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    connectionState = ScreenState.Error,
                                    errorMessage =
                                        error.message
                                            ?: "Failed to load connections",
                                    isLoading = false
                                )
                        }
                    }
                )
        }
    }


    private fun loadPendingRequests(
        expectedUserId: String
    ) {

        viewModelScope.launch {

            if (firebaseAuth.currentUser?.uid != expectedUserId) {
                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    requestState = ScreenState.Loading
                )

            connectionRepository
                .getPendingRequests()
                .fold(

                    onSuccess = { requests ->

                        if (firebaseAuth.currentUser?.uid == expectedUserId) {

                            val userIds =
                                requests.flatMap {
                                    listOf(
                                        it.senderId,
                                        it.receiverId
                                    )
                                }

                            val missingIds =
                                userIds.filterNot {
                                    _uiState.value
                                        .userLookup
                                        .containsKey(it)
                                }

                            val lookupUpdates =
                                userIds.associateWith { id ->

                                    _uiState.value.userLookup[id]
                                        ?: User(id, id)
                                }

                            _uiState.value =
                                _uiState.value.copy(

                                    pendingRequests = requests,

                                    incomingRequests = requests,

                                    userLookup =
                                        _uiState.value.userLookup +
                                                lookupUpdates,

                                    requestState =
                                        if (requests.isEmpty()) {
                                            ScreenState.Empty
                                        } else {
                                            ScreenState.Success
                                        }
                                )

                            if (missingIds.isNotEmpty()) {

                                loadMissingUsers(
                                    missingIds,
                                    expectedUserId
                                )
                            }
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == expectedUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    requestState = ScreenState.Error,
                                    errorMessage =
                                        error.message
                                            ?: "Failed to load requests"
                                )
                        }
                    }
                )
        }
    }

    private fun loadSentPendingRequests(
        expectedUserId: String
    ) {

        viewModelScope.launch {

            if (firebaseAuth.currentUser?.uid != expectedUserId) {
                return@launch
            }

            connectionRepository
                .getSentPendingRequests()
                .fold(

                    onSuccess = { requests ->

                        if (firebaseAuth.currentUser?.uid == expectedUserId) {

                            val lookupUpdates =
                                requests
                                    .flatMap {
                                        listOf(
                                            it.senderId,
                                            it.receiverId
                                        )
                                    }
                                    .associateWith { id ->

                                        _uiState.value.userLookup[id]
                                            ?: User(id, id)
                                    }

                            _uiState.value =
                                _uiState.value.copy(

                                    sentPendingRequests = requests,

                                    userLookup =
                                        _uiState.value.userLookup +
                                                lookupUpdates
                                )
                        }
                    },

                    onFailure = { error ->

                        if (firebaseAuth.currentUser?.uid == expectedUserId) {

                            _uiState.value =
                                _uiState.value.copy(
                                    errorMessage =
                                        error.message
                                            ?: "Failed to load sent requests"
                                )
                        }
                    }
                )
        }
    }


    private fun bootstrapUsersThenLoad(
        expectedUserId: String
    ) {

        viewModelScope.launch {

            _uiState.value =
                _uiState.value.copy(
                    isBootstrappingUsers = true
                )

            authRepository
                .syncUsers()
                .onFailure { error ->

                    if (
                        firebaseAuth.currentUser?.uid ==
                        expectedUserId
                    ) {

                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage =
                                    "Failed to sync users: ${
                                        error.message
                                            ?: "Unknown error"
                                    }"
                            )
                    }
                }

            if (
                firebaseAuth.currentUser?.uid !=
                expectedUserId
            ) {
                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    isBootstrappingUsers = false
                )

            loadConnections(expectedUserId)
            loadPendingRequests(expectedUserId)
            loadSentPendingRequests(expectedUserId)
        }
    }


    private fun loadMissingUsers(
        userIds: List<String>,
        expectedUserId: String
    ) {

        viewModelScope.launch {

            connectionRepository
                .searchUsers("")
                .fold(

                    onSuccess = { allUsers ->

                        if (
                            firebaseAuth.currentUser?.uid ==
                            expectedUserId
                        ) {

                            val usersToAdd =
                                allUsers.filter {
                                    it.id in userIds
                                }

                            if (usersToAdd.isNotEmpty()) {

                                _uiState.value =
                                    _uiState.value.copy(

                                        userLookup =
                                            _uiState.value.userLookup +
                                                    usersToAdd.associateBy(
                                                        User::id
                                                    )
                                    )
                            }
                        }
                    },

                    onFailure = {
                    }
                )
        }
    }

    override fun onCleared() {

        firebaseAuth.removeAuthStateListener(
            authStateListener
        )

        super.onCleared()
    }
}
