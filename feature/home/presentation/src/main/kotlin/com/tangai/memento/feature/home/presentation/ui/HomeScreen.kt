package com.tangai.memento.feature.home.presentation.ui

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.Post
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.presentation.viewmodel.HomeViewModel
import java.io.File

@Composable
fun HomeScreen(
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
            Text(
                text = "Memento",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

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

                items(uiState.connections, key = { it.id }) { connection ->
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        label = uiState.labelFor(connection),
                        selected = (uiState.selectedFilter is FeedFilter.Connection &&
                                (uiState.selectedFilter as FeedFilter.Connection).connectionId == connection.id),
                        onClick = { viewModel.onFilterSelected(FeedFilter.Connection(connection.id)) }
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
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
                            authorLabel = viewModel.getPostLabel(post),
                            mediaCacheRevision = uiState.mediaCacheRevision
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
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
    authorLabel: String,
    mediaCacheRevision: Long
) {
    val context = LocalContext.current
    val media = post.mediaItems.firstOrNull()
    val localFile = media?.let {
        File(
            context.filesDir,
            "pending_media/${post.connectionId}/${post.id}/${it.mediaId}.jpg",
        )
    }
    val localBitmap: Bitmap? = remember(
        post.id,
        post.connectionId,
        media?.mediaId,
        mediaCacheRevision,
        localFile?.lastModified()
    ) {
        localFile?.takeIf(File::exists)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = authorLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (localBitmap != null) {
                Image(
                    bitmap = localBitmap.asImageBitmap(),
                    contentDescription = post.caption ?: "Shared photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            } else {
                Text("Photo is not available in the local cache yet.")
            }
            post.caption?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
