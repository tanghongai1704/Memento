package com.tangai.memento.feature.home.presentation.ui

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineLarge)
        if (state.isLoading) {
            CircularProgressIndicator()
            return@Column
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (state.email.isNotBlank()) {
            Text(state.email, style = MaterialTheme.typography.bodyMedium)
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
