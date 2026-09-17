package com.tangai.memento.feature.connection.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.connection.presentation.viewmodel.ConnectionViewModel

@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val disconnectTarget = state.connectedUsers.firstOrNull { it.id == state.disconnectTargetUserId }

    if (disconnectTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDisconnect,
            title = { Text("Disconnect from ${disconnectTarget.displayName}?") },
            text = { Text("Shared moments from this connection will no longer be available. Reconnecting later starts a new history.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDisconnect) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDisconnect) { Text("Cancel") }
            }
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Connections",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) }
        item { Text("Connect with a code", style = MaterialTheme.typography.titleLarge) }
        item { Text("Enter the permanent invite code shown on the other person's profile.") }
        item {
            OutlinedTextField(
                value = state.redeemCode,
                onValueChange = viewModel::onRedeemCodeChanged,
                label = { Text("Invite code") },
                placeholder = { Text("XXXX-XXXX") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = viewModel::redeemInvite,
                enabled = !state.isRedeemRunning && state.redeemCode.replace("-", "").length == 8
            ) { Text(if (state.isRedeemRunning) "Connecting…" else "Connect") }
        }

        if (state.isLoading || state.isRedeemRunning) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        state.errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        state.successMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }

        item { HorizontalDivider() }
        item { Text("Connected users", style = MaterialTheme.typography.titleLarge) }
        if (!state.isLoading && state.connectedUsers.isEmpty()) item { Text("No connections yet") }
        items(state.connectedUsers, key = { "connected-" + it.id }) { user ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${user.displayName} (@${user.username})",
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                TextButton(
                    onClick = { viewModel.requestDisconnect(user.id) },
                    enabled = state.connectionIdsByUserId[user.id] != null &&
                        state.disconnectingUserId != user.id
                ) {
                    Text(if (state.disconnectingUserId == user.id) "Disconnecting…" else "Disconnect")
                }
            }
        }
    }
}
