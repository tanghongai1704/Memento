package com.tangai.memento.feature.connection.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.connection.presentation.viewmodel.ConnectionViewModel

@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Connections", style = MaterialTheme.typography.headlineLarge) }
        item { Text("Invite codes are coming soon.") }
        item { OutlinedTextField(value = state.query, onValueChange = viewModel::onQueryChanged,
            label = { Text("Exact username") }, modifier = Modifier.fillMaxWidth()) }
        item { Button(onClick = viewModel::searchUsers, enabled = !state.isLoading) { Text("Search") } }
        if (state.isLoading) item { CircularProgressIndicator() }
        state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        items(state.searchResults, key = { "search-" + it.id }) { Text("${it.displayName} (@${it.username})") }
        item { Text("Connected users", style = MaterialTheme.typography.titleLarge) }
        if (!state.isLoading && state.connectedUsers.isEmpty()) item { Text("No connections yet") }
        items(state.connectedUsers, key = { "connected-" + it.id }) { Text(it.displayName) }
    }
}
