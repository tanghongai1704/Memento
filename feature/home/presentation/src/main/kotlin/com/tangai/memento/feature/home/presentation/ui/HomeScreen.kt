package com.tangai.memento.feature.home.presentation.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.Post
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToConnection: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCreatePost: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Memento",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onNavigateToConnection,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("Connections")
                }

                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text("History")
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                item {
                    FilterChip(
                        label = "All",
                        selected = uiState.selectedFilter is FeedFilter.All,
                        onClick = { viewModel.onFilterSelected(FeedFilter.All) }
                    )
                }

                items(uiState.connections) { connection ->
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        label = connection.username,
                        selected = (uiState.selectedFilter is FeedFilter.User &&
                                (uiState.selectedFilter as FeedFilter.User).userId == connection.id),
                        onClick = { viewModel.onFilterSelected(FeedFilter.User(connection.id)) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.getFilteredPosts().isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No moments yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(viewModel.getFilteredPosts()) { post ->
                        PostCard(
                            post = post,
                            postLabel = viewModel.getPostLabel(post)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateToCreatePost,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create Post")
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(4.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    ) {
        Text(label)
    }
}

@Composable
fun PostCard(
    post: Post,
    postLabel: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = postLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "[${post.mediaType.name} placeholder]",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
