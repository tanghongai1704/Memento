package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val repository: ConnectionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val state = MutableStateFlow(ConnectionUiState())
    val uiState = state.asStateFlow()
    private val listener = FirebaseAuth.AuthStateListener {
        state.value = ConnectionUiState()
        val uid = it.currentUser?.uid
        if (uid != null) viewModelScope.launch {
            state.value = state.value.copy(isLoading = true)
            val result = repository.loadConnections()
            if (auth.currentUser?.uid == uid) state.value = state.value.copy(
                connectedUsers = result.getOrDefault(emptyList()), isLoading = false,
                errorMessage = result.exceptionOrNull()?.message)
        }
    }
    init { auth.addAuthStateListener(listener) }
    fun onQueryChanged(query: String) { state.value = state.value.copy(query = query, searchResults = emptyList()) }
    fun searchUsers() {
        val uid = auth.currentUser?.uid ?: return
        val query = state.value.query
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true, errorMessage = null)
            val result = repository.searchUsers(query)
            if (auth.currentUser?.uid == uid) state.value = state.value.copy(
                searchResults = if (state.value.query == query) result.getOrDefault(emptyList()) else emptyList(),
                isLoading = false, errorMessage = result.exceptionOrNull()?.message)
        }
    }
    override fun onCleared() { auth.removeAuthStateListener(listener) }
}
