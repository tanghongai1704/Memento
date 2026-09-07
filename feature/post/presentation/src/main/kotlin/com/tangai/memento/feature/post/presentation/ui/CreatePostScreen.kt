package com.tangai.memento.feature.post.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
fun CreatePostScreen(
    onNavigateToMediaPicker: () -> Unit,
    onNavigateBack: () -> Unit,
    homeUiState: Any? = null,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val createPostUiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Create Moment",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            Text(
                text = "Share with:",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(createPostUiState.recipients) { user ->
                    val isSelected = createPostUiState.selectedRecipient?.id == user.id
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { viewModel.onRecipientSelected(user) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = user.username,
                                fontSize = 18.sp
                            )
                            if (isSelected) {
                                Text(
                                    text = "Selected",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            createPostUiState.errorMessage?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Button(
                onClick = { onNavigateToMediaPicker() },
                enabled = createPostUiState.selectedRecipient != null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            ) {
                Text("Continue")
            }

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp)
            ) {
                Text("Back")
            }
        }
    }
}
