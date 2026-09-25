package com.tangai.memento.feature.home.presentation.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tangai.memento.feature.home.presentation.viewmodel.ProfileUiState
import com.tangai.memento.feature.home.presentation.viewmodel.ProfileViewModel
import com.tangai.memento.ui.MementoScreenHeader
import com.tangai.memento.ui.MementoSectionTitle

@Composable
fun ProfileScreen(
    isDarkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isEditing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage == "Profile updated.") isEditing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MementoScreenHeader(title = "Profile")

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        ProfilePhotoHeader(
            displayName = state.displayName,
            avatarPath = state.user?.avatarPath
        )

        InviteCard(
            state = state,
            onRetry = viewModel::retryInviteCode,
            onShare = {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Connect with me on Memento using code ${state.inviteCode}."
                    )
                }
                context.startActivity(Intent.createChooser(share, "Share invite code"))
            }
        )

        state.errorMessage?.let { message ->
            StatusCard(message = message, isError = true)
        }
        state.successMessage?.let { message ->
            StatusCard(message = message, isError = false)
        }

        ProfileInformationCard(
            state = state,
            isEditing = isEditing,
            onEdit = { isEditing = true },
            onCancel = {
                viewModel.discardChanges()
                isEditing = false
            },
            onSave = viewModel::saveProfile,
            onDisplayNameChanged = viewModel::onDisplayNameChanged,
            onUsernameChanged = viewModel::onUsernameChanged,
            onBioChanged = viewModel::onBioChanged
        )

        SettingsCard(
            isDarkTheme = isDarkTheme,
            onDarkThemeChanged = onDarkThemeChanged,
            email = state.email,
            onLogout = onLogout
        )

        if (state.user == null) {
            OutlinedButton(
                onClick = viewModel::loadProfile,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try again")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InviteCard(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MementoSectionTitle(
                text = "Invite someone",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                "Share your private code with someone you trust.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )

            when {
                state.isLoadingInviteCode -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        "Loading invite code…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                state.inviteCodeError != null -> {
                    Text(
                        state.inviteCodeError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) {
                        Text("Try again")
                    }
                }

                state.inviteCode.isNotBlank() -> {
                    Text(
                        text = state.inviteCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.IosShare, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Share code")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInformationCard(
    state: ProfileUiState,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MementoSectionTitle(
                    text = "Personal details",
                    modifier = Modifier.weight(1f)
                )
                if (!isEditing) {
                    TextButton(onClick = onEdit) { Text("Edit") }
                }
            }

            if (isEditing) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = onDisplayNameChanged,
                        label = { Text("Display name") },
                        supportingText = { state.displayNameError?.let { Text(it) } },
                        isError = state.displayNameError != null,
                        enabled = !state.isSaving,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = onUsernameChanged,
                        label = { Text("Username") },
                        prefix = { Text("@") },
                        supportingText = {
                            Text(state.usernameError ?: "2–30 characters: letters, numbers, . or _")
                        },
                        isError = state.usernameError != null,
                        enabled = !state.isSaving,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    OutlinedTextField(
                        value = state.bio,
                        onValueChange = onBioChanged,
                        label = { Text("Bio") },
                        supportingText = { Text(state.bioError ?: "${state.bio.length}/500") },
                        isError = state.bioError != null,
                        enabled = !state.isSaving,
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onSave,
                            enabled = state.hasChanges && !state.isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(if (state.isSaving) "Saving…" else "Save")
                        }
                    }
                }
            } else {
                Column {
                    ProfileDetailItem(
                        label = "Display name",
                        value = state.displayName.ifBlank { "Not set" }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileDetailItem(
                        label = "Username",
                        value = state.username.takeIf(String::isNotBlank)?.let { "@$it" }
                            ?: "Not set"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileDetailItem(
                        label = "Email",
                        value = state.email.ifBlank { "Not available" }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ProfileDetailItem(
                        label = "Bio",
                        value = state.bio.ifBlank { "No bio yet" },
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoHeader(displayName: String, avatarPath: String?) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
            if (!avatarPath.isNullOrBlank()) {
                AsyncImage(
                    model = avatarPath,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun ProfileDetailItem(label: String, value: String, maxLines: Int = 1) {
    ListItem(
        overlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

@Composable
private fun SettingsCard(
    isDarkTheme: Boolean,
    onDarkThemeChanged: (Boolean) -> Unit,
    email: String,
    onLogout: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            MementoSectionTitle(
                text = "Settings",
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp)
            )
            ListItem(
                headlineContent = { Text("Dark mode") },
                supportingContent = {
                    Text(
                        "Use a darker color palette",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onDarkThemeChanged
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            ListItem(
                headlineContent = {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                },
                supportingContent = {
                    Text(
                        email.ifBlank { "Current account" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Outlined.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.clickable(onClick = onLogout)
            )
        }
    }
}

@Composable
private fun StatusCard(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}
