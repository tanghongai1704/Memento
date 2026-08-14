package com.tangai.memento.feature.post.preview

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tangai.memento.feature.post.create.CreatePostViewModel
import com.tangai.memento.feature.home.HomeViewModel

@Composable
fun MediaPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    createPostViewModel: CreatePostViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val uiState by createPostViewModel.uiState.collectAsStateWithLifecycle()

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
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 24.dp)
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
                    createPostViewModel.simulatePostCreation { newPost ->
                        // Add the post to homeViewModel and navigate home
                        homeViewModel.addPost(newPost)
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
