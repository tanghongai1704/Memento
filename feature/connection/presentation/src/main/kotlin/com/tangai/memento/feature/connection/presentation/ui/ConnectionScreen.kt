package com.tangai.memento.feature.connection.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.connection.presentation.viewmodel.ConnectionViewModel
import com.tangai.memento.ui.MementoScreenHeader
import com.tangai.memento.ui.MementoSectionTitle

@Composable
fun ConnectionScreen(
    onOpenConnectionFeed: (String) -> Unit,
    viewModel: ConnectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val disconnectTarget = state.connectedUsers.firstOrNull { it.id == state.disconnectTargetUserId }

    if (disconnectTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDisconnect,
            title = { Text("Disconnect from ${disconnectTarget.displayName}?") },
            text = { Text("Shared moments from this connection will no longer be available. Reconnecting later starts a new shared timeline.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDisconnect,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDisconnect) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MementoScreenHeader(
            title = "Connections",
            subtitle = "Private access with a direct invite code"
        )

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MementoSectionTitle(text = "Connect with a code")
                Text(
                    "Enter the permanent invite code shown on the other person’s profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.redeemCode,
                    onValueChange = viewModel::onRedeemCodeChanged,
                    label = { Text("Invite code") },
                    placeholder = { Text("XXXX-XXXX") },
                    visualTransformation = InviteCodeVisualTransformation,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = viewModel::redeemInvite,
                    enabled = !state.isRedeemRunning && state.redeemCode.length == 8,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isRedeemRunning) "Connecting…" else "Connect")
                }
            }
        }

        if (state.isLoading || state.isRedeemRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        state.errorMessage?.let { message ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        state.successMessage?.let { message ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        MementoSectionTitle(text = "Connected users")

        if (!state.isLoading && state.connectedUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No connections yet. Add someone with their invite code to start sharing private moments.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.connectedUsers, key = { "connected-" + it.id }) { user ->
                    val connectionId = state.connectionIdsByUserId[user.id]
                    Card(
                        onClick = { connectionId?.let(onOpenConnectionFeed) },
                        enabled = connectionId != null,
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = user.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            },
                            trailingContent = {
                                TextButton(
                                    onClick = { viewModel.requestDisconnect(user.id) },
                                    enabled = connectionId != null && state.disconnectingUserId != user.id
                                ) {
                                    Text(if (state.disconnectingUserId == user.id) "Working…" else "Disconnect")
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    }
                }
            }
        }
    }
}

private val InviteCodeVisualTransformation = VisualTransformation { text ->
    val rawCode = text.text.take(8)
    val displayedCode = if (rawCode.length > 4) {
        rawCode.take(4) + "-" + rawCode.drop(4)
    } else {
        rawCode
    }
    TransformedText(
        text = AnnotatedString(displayedCode),
        offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= 4) offset else offset + 1

            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= 4) offset else offset - 1
        }
    )
}
