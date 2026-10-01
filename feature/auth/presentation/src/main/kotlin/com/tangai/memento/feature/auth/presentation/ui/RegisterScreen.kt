package com.tangai.memento.feature.auth.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.auth.presentation.viewmodel.RegisterEffect
import com.tangai.memento.feature.auth.presentation.viewmodel.RegisterViewModel

@Composable
fun RegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val submit = {
        focusManager.clearFocus()
        if (!uiState.isLoading) viewModel.onRegisterClick()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                RegisterEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    AuthScreenLayout(
        topBar = { AuthBackTopBar(title = "Create account", onNavigateBack = onNavigateToLogin) }
    ) {
        Text(
            text = "Set up your account to start sharing private moments.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChanged,
            label = { Text("Email") },
            isError = uiState.emailError != null,
            supportingText = { uiState.emailError?.let { Text(it) } },
            enabled = !uiState.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress },
            shape = MaterialTheme.shapes.medium,
            colors = authTextFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChanged,
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = uiState.passwordError != null,
            supportingText = { uiState.passwordError?.let { Text(it) } },
            enabled = !uiState.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                PasswordVisibilityButton(
                    visible = passwordVisible,
                    onClick = { passwordVisible = !passwordVisible }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.NewPassword },
            shape = MaterialTheme.shapes.medium,
            colors = authTextFieldColors()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChanged,
            label = { Text("Confirm password") },
            visualTransformation = if (confirmPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = uiState.confirmPasswordError != null,
            supportingText = { uiState.confirmPasswordError?.let { Text(it) } },
            enabled = !uiState.isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                PasswordVisibilityButton(
                    visible = confirmPasswordVisible,
                    onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.NewPassword },
            shape = MaterialTheme.shapes.medium,
            colors = authTextFieldColors()
        )

        uiState.generalError?.let { message ->
            AuthStatusMessage(
                message = message,
                isError = true,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Button(
            onClick = submit,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .heightIn(min = 48.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.size(8.dp))
            }
            Text(if (uiState.isLoading) "Creating account…" else "Create account")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onNavigateToLogin,
                enabled = !uiState.isLoading
            ) {
                Text("Sign in")
            }
        }
    }
}
