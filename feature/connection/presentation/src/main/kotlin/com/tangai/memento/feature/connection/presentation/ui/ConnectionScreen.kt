package com.tangai.memento.feature.connection.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.connection.presentation.viewmodel.ConnectionViewModel
import com.tangai.memento.feature.connection.presentation.viewmodel.ScreenState

@Composable
fun ConnectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: ConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Connections")
            Button(onClick = onNavigateBack) { Text("Back") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search user") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = viewModel::searchUsers) {
            Text("Search")
        }

        when (uiState.searchState) {
            ScreenState.Loading -> LoadingBlock("Searching users...")
            ScreenState.Error -> ErrorBlock(uiState.errorMessage ?: "Search failed")
            ScreenState.Empty -> EmptyBlock("No search result")
            ScreenState.Success -> UserList(
                title = "Search Result",
                users = uiState.searchResults,
                emptyText = "No users found",
                sentPendingReceiverIds = uiState.sentPendingRequests.map { it.receiverId }.toSet(),
                connectedUserIds = uiState.connectedUsers.map { it.id }.toSet(),
                onAction = { viewModel.sendConnectionRequest(it.id) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.requestState) {
            ScreenState.Loading -> LoadingBlock("Loading requests...")
            ScreenState.Error -> ErrorBlock(uiState.errorMessage ?: "Request load failed")
            ScreenState.Empty -> EmptyBlock("No pending requests")
            ScreenState.Success -> RequestList(
                title = "Incoming Requests",
                requests = uiState.incomingRequests,
                onAccept = viewModel::acceptConnectionRequest,
                onReject = viewModel::rejectConnectionRequest
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (uiState.connectionState) {
            ScreenState.Loading -> LoadingBlock("Loading connections...")
            ScreenState.Error -> ErrorBlock(uiState.errorMessage ?: "Connection load failed")
            ScreenState.Empty -> EmptyBlock("No connected users")
            ScreenState.Success -> UserList(
                title = "Connected Users",
                users = uiState.connectedUsers,
                emptyText = "No connected users",
                sentPendingReceiverIds = emptySet(),
                connectedUserIds = uiState.connectedUsers.map { it.id }.toSet(),
                onAction = {}
            )
        }
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(message)
        }
    }
}

@Composable
private fun EmptyBlock(message: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(message)
    }
}

@Composable
private fun ErrorBlock(message: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(message)
    }
}

@Composable
private fun UserList(
    title: String,
    users: List<com.tangai.memento.domain.model.User>,
    emptyText: String,
    sentPendingReceiverIds: Set<String> = emptySet(),
    connectedUserIds: Set<String> = emptySet(),
    onAction: (com.tangai.memento.domain.model.User) -> Unit
) {
    Column {
        Text(title)
        if (users.isEmpty()) {
            EmptyBlock(emptyText)
        } else {
            LazyColumn {
                items(users) { user ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(user.username)
                            val isConnected = connectedUserIds.contains(user.id)
                            val isPending = sentPendingReceiverIds.contains(user.id)
                            val buttonLabel = when {
                                isConnected -> "Connected"
                                isPending -> "Pending"
                                else -> "Request"
                            }
                            Button(
                                enabled = !isConnected && !isPending,
                                onClick = { onAction(user) }
                            ) {
                                Text(buttonLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestList(
    title: String,
    requests: List<com.tangai.memento.domain.model.ConnectionRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column {
        Text(title)
        LazyColumn {
            items(requests) { request ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(request.senderId)
                        Row {
                            Button(onClick = { onAccept(request.id) }) { Text("Accept") }
                            Button(onClick = { onReject(request.id) }) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}
