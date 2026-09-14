package com.tangai.memento.feature.connection.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.connection.presentation.viewmodel.ConnectionViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Connections", style = MaterialTheme.typography.headlineLarge) }
        item { Text("Invite someone", style = MaterialTheme.typography.titleLarge) }
        item { Text("Create a private code. It expires after 10 minutes and can be used once.") }
        state.inviteCode?.let { code ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(code, style = MaterialTheme.typography.headlineMedium)
                        state.inviteExpiresAtMillis?.let {
                            Text("Expires ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Join me on Memento with code $code. It expires in 10 minutes."
                                    )
                                }
                                context.startActivity(Intent.createChooser(share, "Share invite"))
                            }) { Text("Share") }
                            OutlinedButton(
                                onClick = viewModel::revokeInvite,
                                enabled = !state.isInviteActionRunning
                            ) { Text("Revoke") }
                        }
                        TextButton(onClick = viewModel::createInvite, enabled = !state.isInviteActionRunning) {
                            Text("Create a new code")
                        }
                    }
                }
            }
        } ?: item {
            Button(onClick = viewModel::createInvite, enabled = !state.isInviteActionRunning) {
                Text(if (state.isInviteActionRunning) "Creating…" else "Create invite code")
            }
        }

        item { HorizontalDivider() }
        item { Text("Enter an invite", style = MaterialTheme.typography.titleLarge) }
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

        if (state.isLoading || state.isInviteActionRunning || state.isRedeemRunning) {
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
        items(state.connectedUsers, key = { "connected-" + it.id }) {
            Text("${it.displayName} (@${it.username})")
        }
    }
}
