package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tangai.memento.feature.connection.domain.CachedConnectionUsers
import com.tangai.memento.feature.connection.domain.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val repository: ConnectionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val state = MutableStateFlow(ConnectionUiState())
    val uiState = state.asStateFlow()
    private var connectionObservationJob: Job? = null
    private val listener = FirebaseAuth.AuthStateListener {
        connectionObservationJob?.cancel()
        state.value = ConnectionUiState()
        val uid = it.currentUser?.uid
        if (uid != null) connectionObservationJob = viewModelScope.launch {
            val cached = repository.loadCachedConnectionUsers().getOrNull()
            if (auth.currentUser?.uid != uid) return@launch
            cached?.let(::showConnections)
            state.value = state.value.copy(isLoading = false)

            repository.observeConnections().collectLatest { result ->
                if (auth.currentUser?.uid == uid) {
                    result.onSuccess {
                        val refreshedCache = repository.loadCachedConnectionUsers().getOrNull()
                        if (refreshedCache != null) {
                            showConnections(refreshedCache)
                        } else {
                            state.value = state.value.copy(
                                isLoading = false,
                                errorMessage = "Could not read your saved connections."
                            )
                        }
                    }
                        .onFailure { error -> state.value = state.value.copy(
                            isLoading = false,
                            errorMessage = error.userMessage()
                        ) }
                }
            }
        }
    }
    init { auth.addAuthStateListener(listener) }
    fun onRedeemCodeChanged(value: String) {
        val normalized = value.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(8)
        state.value = state.value.copy(
            redeemCode = normalized,
            errorMessage = null,
            successMessage = null
        )
    }
    fun redeemInvite() {
        val uid = auth.currentUser?.uid ?: return
        val code = state.value.redeemCode
        if (code.length != 8) {
            state.value = state.value.copy(errorMessage = "Enter the complete 8-character invite code.")
            return
        }
        viewModelScope.launch {
            state.value = state.value.copy(isRedeemRunning = true, errorMessage = null, successMessage = null)
            val result = repository.redeemDirectInvite(code)
            if (auth.currentUser?.uid == uid) {
                state.value = state.value.copy(
                    redeemCode = if (result.isSuccess) "" else code,
                    isRedeemRunning = false,
                    successMessage = if (result.isSuccess) "Connected successfully." else null,
                    errorMessage = result.exceptionOrNull()?.userMessage()
                )
            }
        }
    }

    fun requestDisconnect(userId: String) {
        state.value = state.value.copy(
            disconnectTargetUserId = userId,
            errorMessage = null,
            successMessage = null
        )
    }

    fun cancelDisconnect() {
        state.value = state.value.copy(disconnectTargetUserId = null)
    }

    fun confirmDisconnect() {
        val uid = auth.currentUser?.uid ?: return
        val userId = state.value.disconnectTargetUserId ?: return
        val connectionId = state.value.connectionIdsByUserId[userId] ?: return
        viewModelScope.launch {
            state.value = state.value.copy(
                disconnectTargetUserId = null,
                disconnectingUserId = userId,
                errorMessage = null,
                successMessage = null
            )
            repository.disconnectDirect(connectionId)
                .onSuccess {
                    if (auth.currentUser?.uid == uid) {
                        repository.loadCachedConnectionUsers().getOrNull()?.let(::showConnections)
                        state.value = state.value.copy(
                            disconnectingUserId = null,
                            successMessage = "Disconnected successfully."
                        )
                    }
                }
                .onFailure { error ->
                    if (auth.currentUser?.uid == uid) state.value = state.value.copy(
                        disconnectingUserId = null,
                        errorMessage = error.userMessage()
                    )
                }
        }
    }

    private fun showConnections(cached: CachedConnectionUsers) {
        state.value = state.value.copy(
            connectedUsers = cached.users,
            connectionIdsByUserId = cached.connectionIdsByUserId,
            isLoading = false,
            errorMessage = null
        )
    }
    override fun onCleared() {
        connectionObservationJob?.cancel()
        auth.removeAuthStateListener(listener)
    }
    private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() }
        ?: "Something went wrong. Please try again."
}
