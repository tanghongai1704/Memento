package com.tangai.memento.feature.connection.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val _connections = MutableStateFlow<List<User>>(emptyList())
    val connections: StateFlow<List<User>> = _connections.asStateFlow()

    init {
        loadConnections()
    }

    private fun loadConnections() {
        viewModelScope.launch {
            val result = connectionRepository.loadConnections()
            _connections.value = result.getOrElse { emptyList() }
        }
    }
}
