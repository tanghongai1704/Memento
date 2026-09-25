package com.tangai.memento.feature.home.presentation.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip as MaterialFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.presentation.viewmodel.HomeViewModel
import java.io.File
import com.tangai.memento.ui.PhotoLayout
import com.tangai.memento.ui.MementoScreenHeader
import coil3.compose.AsyncImage

@Composable
fun HomeScreen(
    onCreateMoment: () -> Unit,
    onOpenConnections: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Post?>(null) }

    pendingDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this moment?") },
            text = { Text("It will disappear for everyone in this connection.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        viewModel.deletePost(post)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            MementoScreenHeader(
                title = "Memento",
                subtitle = "Private shared moments",
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                item {
                    FeedFilterChip(
                        label = "All",
                        selected = uiState.selectedFilter is FeedFilter.All,
                        onClick = { viewModel.onFilterSelected(FeedFilter.All) }
                    )
                }

                items(uiState.connections, key = { it.id }) { connection ->
                    Spacer(modifier = Modifier.width(8.dp))
                    FeedFilterChip(
                        label = uiState.labelFor(connection),
                        avatarLabel = uiState.connectionUsers[connection.id]
                            ?.displayName
                            ?.firstOrNull()
                            ?.uppercase()
                            ?: uiState.labelFor(connection).firstOrNull()?.uppercase(),
                        avatarModel = uiState.connectionUsers[connection.id]?.avatarPath,
                        selected = (uiState.selectedFilter is FeedFilter.Connection &&
                                (uiState.selectedFilter as FeedFilter.Connection).connectionId == connection.id),
                        onClick = { viewModel.onFilterSelected(FeedFilter.Connection(connection.id)) }
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
            } else if (uiState.errorMessage != null && viewModel.getFilteredPosts().isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Couldn’t load your moments",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            uiState.errorMessage ?: "Please try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = viewModel::refresh) { Text("Try again") }
                    }
                }
            } else if (viewModel.getFilteredPosts().isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No moments yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            if (uiState.connections.isEmpty()) {
                                "Connect with someone first, then start sharing private moments."
                            } else {
                                "Your shared photos will appear here as soon as you post or receive them."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = if (uiState.connections.isEmpty()) onOpenConnections else onCreateMoment
                        ) {
                            Text(if (uiState.connections.isEmpty()) "Add a connection" else "Create a moment")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = viewModel.getFilteredPosts(),
                        key = { post -> "${post.connectionId}:${post.id}" }
                    ) { post ->
                        val author = viewModel.getPostAuthor(post)
                        PostCard(
                            post = post,
                            author = author,
                            mediaCacheRevision = uiState.mediaCacheRevision,
                            canDelete = viewModel.canDelete(post),
                            isDeleting = "${post.connectionId}:${post.id}" in uiState.deletingPostKeys,
                            onDelete = { pendingDelete = post }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (uiState.hasMorePosts) {
                        item(key = "load-more") {
                            Button(
                                onClick = viewModel::loadOlderPosts,
                                enabled = !uiState.isLoadingMore,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(20.dp).height(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (uiState.isLoadingMore) "Loading..." else "Load older moments")
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun FeedFilterChip(
    label: String,
    avatarLabel: String? = null,
    avatarModel: Any? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    MaterialFilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(end = 8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        ),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (avatarLabel != null) {
                    UserAvatar(
                        imageModel = avatarModel,
                        fallbackLabel = avatarLabel,
                        size = 24.dp,
                        selected = selected
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 112.dp)
                )
            }
        }
    )
}

@Composable
fun PostCard(
    post: Post,
    author: User?,
    mediaCacheRevision: Long,
    canDelete: Boolean,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val cachedMedia = remember(post, mediaCacheRevision) {
        post.mediaItems.sortedBy { it.position }.map { media ->
            File(
                context.filesDir,
                "pending_media/${post.connectionId}/${post.id}/${media.mediaId}.jpg",
            ).takeIf(File::exists)
        }
    }
    val displayName = author?.displayName?.takeIf(String::isNotBlank) ?: "Unknown author"
    val username = author?.username?.takeIf(String::isNotBlank)
    val sharedAt = post.createdAt ?: post.clientCreatedAt
    val relativeTime = remember(sharedAt) {
        DateUtils.getRelativeTimeSpanString(
            sharedAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var captionExpanded by remember(post.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    imageModel = author?.avatarPath,
                    fallbackLabel = displayName.firstOrNull()?.uppercase() ?: "?",
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${username?.let { "@$it" } ?: "@unknown"} · $relativeTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Post options"
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (canDelete) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isDeleting) "Deleting…" else "Delete",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                enabled = !isDeleting,
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    disabledTextColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                                )
                            )
                        } else {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "No actions",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = {},
                                enabled = false
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            post.caption?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (captionExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (it.length > 180) {
                    TextButton(
                        onClick = { captionExpanded = !captionExpanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (captionExpanded) "Show less" else "Show more")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            PhotoLayout(
                media = cachedMedia,
                layoutType = post.layoutType,
                contentDescription = post.caption ?: "Shared photo",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UserAvatar(
    imageModel: Any?,
    fallbackLabel: String,
    size: androidx.compose.ui.unit.Dp,
    selected: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fallbackLabel,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            fontWeight = FontWeight.SemiBold
        )
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
