package com.tangai.memento.feature.home.presentation.ui

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.feature.home.domain.FeedFilter
import com.tangai.memento.feature.home.presentation.viewmodel.HomeViewModel
import java.text.DateFormat
import java.util.Date
import com.tangai.memento.ui.PhotoLayout
import com.tangai.memento.ui.MissingPhotoState
import com.tangai.memento.ui.MementoScreenHeader
import coil3.compose.AsyncImage

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    onCreateMoment: () -> Unit,
    onOpenConnections: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Post?>(null) }
    var showConnectionPicker by remember { mutableStateOf(false) }
    val connectionFilterListState = rememberLazyListState()
    val postListState = rememberLazyListState()
    val selectedConnectionId = (uiState.selectedFilter as? FeedFilter.Connection)?.connectionId
    val filteredPosts = viewModel.getFilteredPosts()
    val newestPost = filteredPosts.firstOrNull()
    var previousNewestPostKey by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(newestPost?.let(uiState::postKey)) {
        val newestPostKey = newestPost?.let(uiState::postKey)
        if (
            newestPostKey != null &&
            previousNewestPostKey != null &&
            newestPostKey != previousNewestPostKey &&
            newestPost.authorId == uiState.currentUserId
        ) {
            postListState.animateScrollToItem(0)
        }
        previousNewestPostKey = newestPostKey
    }

    LaunchedEffect(selectedConnectionId, uiState.suggestedConnections) {
        val selectedIndex = uiState.suggestedConnections
            .indexOfFirst { it.id == selectedConnectionId }
        if (selectedIndex >= 0) {
            connectionFilterListState.animateScrollToItem(selectedIndex + 1)
        }
    }

    if (showConnectionPicker) {
        ConnectionPickerSheet(
            state = uiState,
            onSelect = { connectionId ->
                viewModel.onFilterSelected(FeedFilter.Connection(connectionId))
                showConnectionPicker = false
            },
            onDismiss = { showConnectionPicker = false }
        )
    }

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
                state = connectionFilterListState,
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

                items(uiState.suggestedConnections, key = { it.id }) { connection ->
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

                if (uiState.hasMoreConnections) {
                    item(key = "more-connections") {
                        MaterialFilterChip(
                            selected = false,
                            onClick = { showConnectionPicker = true },
                            label = { Text("More") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.MoreHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
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
            } else if (uiState.errorMessage != null && filteredPosts.isEmpty()) {
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
            } else if (filteredPosts.isEmpty()) {
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
                    state = postListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(
                        items = filteredPosts,
                        key = { post -> "${post.connectionId}:${post.id}" }
                    ) { post ->
                        val author = viewModel.getPostAuthor(post)
                        val postKey = uiState.postKey(post)
                        PostCard(
                            post = post,
                            author = author,
                            authorLabel = uiState.authorLabelFor(post),
                            sharingLabel = uiState.sharingLabelFor(post),
                            cachedMedia = uiState.cachedMediaByPostKey[postKey]
                                ?: List(post.mediaItems.size) { null },
                            isMediaLoading = postKey in uiState.loadingMediaPostKeys,
                            isMediaFailed = postKey in uiState.failedMediaPostKeys,
                            canDelete = viewModel.canDelete(post),
                            isDeleting = postKey in uiState.deletingPostKeys,
                            onRetryMedia = { viewModel.retryPostMedia(post) },
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
@OptIn(ExperimentalMaterial3Api::class)
private fun ConnectionPickerSheet(
    state: com.tangai.memento.feature.home.presentation.viewmodel.HomeUiState,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val connections = remember(state.orderedConnections, state.connectionLabels, query) {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) {
            state.orderedConnections
        } else {
            state.orderedConnections.filter { connection ->
                val user = state.connectionUsers[connection.id]
                state.labelFor(connection).contains(normalizedQuery, ignoreCase = true) ||
                    user?.username?.contains(normalizedQuery, ignoreCase = true) == true
            }
        }
    }
    val selectedId = (state.selectedFilter as? FeedFilter.Connection)?.connectionId

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Choose a connection",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (state.connections.size > 8) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Search connections") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (connections.isEmpty()) {
                item(key = "no-connection-results") {
                    Text(
                        text = "No connections found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
            items(connections, key = { it.id }) { connection ->
                val user = state.connectionUsers[connection.id]
                val displayName = state.labelFor(connection)
                val selected = connection.id == selectedId
                ListItem(
                    headlineContent = {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = user?.username
                        ?.takeIf(String::isNotBlank)
                        ?.let { username ->
                            {
                                Text(
                                    text = "@$username",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                    leadingContent = {
                        UserAvatar(
                            imageModel = user?.avatarPath,
                            fallbackLabel = displayName.firstOrNull()?.uppercase() ?: "?",
                            size = 44.dp,
                            selected = selected
                        )
                    },
                    trailingContent = {
                        RadioButton(selected = selected, onClick = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onSelect(connection.id) }
                )
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
    authorLabel: String,
    sharingLabel: String,
    cachedMedia: List<String?>,
    isMediaLoading: Boolean,
    isMediaFailed: Boolean,
    canDelete: Boolean,
    isDeleting: Boolean,
    onRetryMedia: () -> Unit,
    onDelete: () -> Unit
) {
    val sharedAt = post.createdAt ?: post.clientCreatedAt
    val displayTime = remember(sharedAt) {
        formatPostTime(sharedAt, System.currentTimeMillis())
    }
    var menuExpanded by remember { mutableStateOf(false) }
    var captionExpanded by remember(post.id) { mutableStateOf(false) }
    var previewStartIndex by remember(post.id) { mutableStateOf<Int?>(null) }

    previewStartIndex?.let { initialPage ->
        PhotoPreviewDialog(
            media = cachedMedia,
            initialPage = initialPage,
            contentDescription = post.caption ?: "Shared photo",
            onDismiss = { previewStartIndex = null }
        )
    }

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
                    fallbackLabel = authorLabel.firstOrNull()?.uppercase() ?: "?",
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = authorLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sharingLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "· $displayTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
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
            Box(modifier = Modifier.fillMaxWidth()) {
                PhotoLayout(
                    media = cachedMedia,
                    layoutType = post.layoutType,
                    contentDescription = post.caption ?: "Shared photo",
                    modifier = Modifier.fillMaxWidth(),
                    onPhotoClick = { index -> previewStartIndex = index },
                    missingPhotoState = if (isMediaLoading || !isMediaFailed) {
                        MissingPhotoState.LOADING
                    } else {
                        MissingPhotoState.UNAVAILABLE
                    },
                    onRetry = onRetryMedia
                )
                if (post.mediaItems.size > 1 && post.layoutType != LayoutType.CAROUSEL) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.84f),
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    ) {
                        Text(
                            text = "${post.mediaItems.size} photos",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatPostTime(timestamp: Long, now: Long): String {
    val age = (now - timestamp).coerceAtLeast(0L)
    return if (age < DateUtils.WEEK_IN_MILLIS) {
        DateUtils.getRelativeTimeSpanString(
            timestamp,
            now,
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
    }
}

@Composable
private fun PhotoPreviewDialog(
    media: List<Any?>,
    initialPage: Int,
    contentDescription: String,
    onDismiss: () -> Unit
) {
    if (media.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(media.indices),
        pageCount = media::size
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = 72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val item = media[page]
                        if (item != null) {
                            AsyncImage(
                                model = item,
                                contentDescription = "$contentDescription ${page + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "Photo unavailable offline",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close photo preview")
                }

                if (media.size > 1) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${media.size}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
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
