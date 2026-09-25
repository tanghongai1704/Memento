package com.tangai.memento.feature.history.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.feature.history.presentation.viewmodel.HistoryViewModel
import com.tangai.memento.feature.history.domain.HistoryEntry
import com.tangai.memento.ui.UiState
import java.io.File
import java.text.DateFormat
import java.util.Date
import com.tangai.memento.ui.PhotoLayout
import com.tangai.memento.ui.MementoScreenHeader
import com.tangai.memento.ui.MementoSectionTitle
import coil3.compose.AsyncImage

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            MementoScreenHeader(
                title = "History",
                subtitle = "Your recent private moments",
                modifier = Modifier.padding(bottom = 20.dp)
            )

            when (uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.PhotoAlbum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("No moments yet", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Moments you share and receive will be collected here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    val message = (uiState as UiState.Error).message
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = viewModel::loadHistory) { Text("Retry") }
                        }
                    }
                }

                is UiState.Success -> {
                    val moments = (uiState as UiState.Success<List<HistoryEntry>>).data
                    val momentsByDay = remember(moments) {
                        moments.groupBy { moment ->
                            DateFormat.getDateInstance(DateFormat.LONG)
                                .format(Date(moment.post.createdAt ?: moment.post.clientCreatedAt))
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        momentsByDay.forEach { (day, dayMoments) ->
                            item(key = "day-$day") {
                                MementoSectionTitle(
                                    text = day,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            items(
                                items = dayMoments,
                                key = { "${it.post.connectionId}:${it.post.id}" }
                            ) { moment ->
                                HistoryMomentCard(moment)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMomentCard(entry: HistoryEntry) {
    val context = LocalContext.current
    val post = entry.post
    val cachedMedia = post.mediaItems.sortedBy { it.position }.map { media ->
        File(context.filesDir, "pending_media/${post.connectionId}/${post.id}/${media.mediaId}.jpg")
            .takeIf(File::exists)
    }
    val sharedAt = post.createdAt ?: post.clientCreatedAt
    val relatedUser = if (entry.isOutgoing) entry.connectionUser else entry.author
    val relatedName = relatedUser?.displayName?.takeIf(String::isNotBlank) ?: "Unknown user"
    val directionLabel = if (entry.isOutgoing) {
        "You shared with $relatedName"
    } else {
        "$relatedName shared with you"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = relatedName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    relatedUser?.avatarPath?.takeIf(String::isNotBlank)?.let { avatarPath ->
                        AsyncImage(
                            model = avatarPath,
                            contentDescription = "$relatedName avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = directionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            relatedUser?.username?.takeIf(String::isNotBlank)?.let { append("@$it · ") }
                            append(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(sharedAt)))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (entry.isOutgoing) {
                        Icons.Outlined.ArrowUpward
                    } else {
                        Icons.Outlined.ArrowDownward
                    },
                    contentDescription = if (entry.isOutgoing) "Sent" else "Received",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            post.caption?.takeIf(String::isNotBlank)?.let { caption ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            PhotoLayout(
                media = cachedMedia,
                layoutType = post.layoutType,
                contentDescription = post.caption ?: "Shared photo",
                height = 148.dp
            )
        }
    }
}
