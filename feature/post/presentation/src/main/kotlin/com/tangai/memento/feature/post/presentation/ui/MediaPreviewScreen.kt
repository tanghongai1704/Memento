package com.tangai.memento.feature.post.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel

@Composable
fun MediaPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                text = "Preview",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("[Media Placeholder]")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recipient: ${uiState.selectedRecipient?.username ?: "Unknown"}",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isUploading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Uploading... ${(uiState.uploadProgress * 100).toInt()}%")
            } else {
                Button(
                    onClick = {
                        viewModel.simulatePostCreation { newPost ->
                            onNavigateToHome()
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Post")
                }

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Back")
                }
            }
        }
    }
}
