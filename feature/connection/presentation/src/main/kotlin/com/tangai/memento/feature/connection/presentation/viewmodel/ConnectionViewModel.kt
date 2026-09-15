package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
            state.value = state.value.copy(isLoading = true)
            repository.observeConnections().collectLatest { result ->
                if (auth.currentUser?.uid == uid) state.value = state.value.copy(
                    connectedUsers = result.getOrDefault(state.value.connectedUsers),
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.userMessage()
                )
            }
        }
    }
    init { auth.addAuthStateListener(listener) }
    fun onRedeemCodeChanged(value: String) {
        val normalized = value.uppercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(8)
        val formatted = if (normalized.length > 4) normalized.take(4) + "-" + normalized.drop(4) else normalized
        state.value = state.value.copy(redeemCode = formatted, errorMessage = null, successMessage = null)
    }
    fun redeemInvite() {
        val uid = auth.currentUser?.uid ?: return
        val code = state.value.redeemCode
        if (code.replace("-", "").length != 8) {
            state.value = state.value.copy(errorMessage = "Enter the complete 8-character invite code.")
            return
        }
        viewModelScope.launch {
            state.value = state.value.copy(isRedeemRunning = true, errorMessage = null, successMessage = null)
            val result = repository.redeemDirectInvite(code)
            if (auth.currentUser?.uid == uid) {
                val users = if (result.isSuccess) repository.loadConnections().getOrDefault(state.value.connectedUsers)
                    else state.value.connectedUsers
                state.value = state.value.copy(
                    redeemCode = if (result.isSuccess) "" else code, connectedUsers = users,
                    isRedeemRunning = false,
                    successMessage = if (result.isSuccess) "Connected successfully." else null,
                    errorMessage = result.exceptionOrNull()?.userMessage()
                )
            }
        }
    }
    override fun onCleared() {
        connectionObservationJob?.cancel()
        auth.removeAuthStateListener(listener)
    }
    private fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() }
        ?: "Something went wrong. Please try again."
}
