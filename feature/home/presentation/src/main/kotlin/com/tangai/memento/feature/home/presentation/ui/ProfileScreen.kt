package com.tangai.memento.feature.home.presentation.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.home.presentation.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )

        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.email.isNotBlank()) {
            Text(state.email, style = MaterialTheme.typography.bodyMedium)
        }
        if (state.inviteCode.isNotBlank()) {
            Text("Your invite code", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.inviteCode,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                supportingText = { Text("This code is permanent and can be used by people who want to connect with you.") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Connect with me on Memento using code ${state.inviteCode}.")
                    }
                    context.startActivity(Intent.createChooser(share, "Share invite code"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share invite code") }
        }
        OutlinedTextField(
            value = state.displayName,
            onValueChange = viewModel::onDisplayNameChanged,
            label = { Text("Display name") },
            supportingText = { state.displayNameError?.let { Text(it) } },
            isError = state.displayNameError != null,
            enabled = !state.isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::onUsernameChanged,
            label = { Text("Username") },
            supportingText = { Text(state.usernameError ?: "2–30 characters: letters, numbers, . or _") },
            isError = state.usernameError != null,
            enabled = !state.isSaving,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.bio,
            onValueChange = viewModel::onBioChanged,
            label = { Text("Bio") },
            supportingText = { Text(state.bioError ?: "${state.bio.length}/500") },
            isError = state.bioError != null,
            enabled = !state.isSaving,
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = viewModel::saveProfile,
            enabled = state.hasChanges && !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) CircularProgressIndicator() else Text("Save profile")
        }
        if (state.user == null) {
            OutlinedButton(
                onClick = viewModel::loadProfile,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Try again") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }
    }
}
