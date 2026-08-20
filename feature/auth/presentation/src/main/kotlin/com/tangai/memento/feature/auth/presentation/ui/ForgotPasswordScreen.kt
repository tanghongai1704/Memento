package com.tangai.memento.feature.auth.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Forgot Password",
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Coming soon...",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = onNavigateBack) {
                Text("Back")
            }
        }
    }
}
