package com.tangai.memento.feature.post.create

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tangai.memento.data.model.User
import com.tangai.memento.data.model.FeedFilter
import com.tangai.memento.feature.home.HomeViewModel

@Composable
fun CreatePostScreen(
    onNavigateToMediaPicker: () -> Unit,
    onNavigateBack: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    createPostViewModel: CreatePostViewModel = viewModel()
) {
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val createPostUiState by createPostViewModel.uiState.collectAsStateWithLifecycle()

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
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 24.dp)
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
                items(homeUiState.connections) { user ->
                    UserSelectionCard(
                        user = user,
                        isSelected = createPostUiState.selectedRecipient?.id == user.id,
                        onClick = { createPostViewModel.onRecipientSelected(user) }
                    )
                }
            }

            createPostUiState.errorMessage?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            LaunchedEffect(homeUiState.selectedFilter) {
                // If filter is a specific user, set them as the default recipient
                if (homeUiState.selectedFilter is FeedFilter.User && createPostUiState.selectedRecipient == null) {
                    val userId = (homeUiState.selectedFilter as FeedFilter.User).userId
                    val user = homeUiState.connections.find { it.id == userId }
                    user?.let { createPostViewModel.setDefaultRecipient(it) }
                }
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

@Composable
fun UserSelectionCard(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
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
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
