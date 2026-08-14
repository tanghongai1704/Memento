package com.tangai.memento.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tangai.memento.feature.auth.signup.SignupViewModel

@Composable
fun SignupScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    signupViewModel: SignupViewModel = viewModel()
) {
    val uiState by signupViewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Memento",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = "Sign Up",
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = uiState.username,
                onValueChange = { signupViewModel.onUsernameChanged(it) },
                label = { Text("Username") },
                modifier = Modifier.padding(8.dp),
                enabled = !uiState.isLoading
            )

            OutlinedTextField(
                value = uiState.account,
                onValueChange = { signupViewModel.onAccountChanged(it) },
                label = { Text("Email / Phone") },
                modifier = Modifier.padding(8.dp),
                enabled = !uiState.isLoading
            )

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { signupViewModel.onPasswordChanged(it) },
                label = { Text("Password") },
                modifier = Modifier.padding(8.dp),
                visualTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                enabled = !uiState.isLoading
            )

            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = { signupViewModel.onConfirmPasswordChanged(it) },
                label = { Text("Confirm Password") },
                modifier = Modifier.padding(8.dp),
                visualTransformation = if (uiState.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                enabled = !uiState.isLoading
            )

            uiState.errorMessage?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                        onClick = { signupViewModel.onSignupClick(onNavigateToHome) },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToLogin,
                enabled = !uiState.isLoading
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}
