package com.tangai.memento.feature.post.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangai.memento.feature.post.presentation.upload.PostUploadItem
import com.tangai.memento.feature.post.presentation.upload.PostUploadStatus

@Composable
fun PostUploadStatusBar(
    items: List<PostUploadItem>,
    onRetry: (String) -> Unit,
    onRetryAll: () -> Unit,
    onDiscard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val failedCount = items.count { it.status == PostUploadStatus.FAILED }
    val activeItem = items.firstOrNull { it.status != PostUploadStatus.FAILED }
    val headline = when {
        failedCount == items.size -> "$failedCount moment${if (failedCount == 1) "" else "s"} need attention"
        activeItem?.status == PostUploadStatus.PREPARING -> "Preparing ${items.size} moment${if (items.size == 1) "" else "s"}"
        else -> "Posting ${items.size} moment${if (items.size == 1) "" else "s"}"
    }

    Surface(
        color = if (failedCount > 0) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (failedCount > 0) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.CloudUpload, contentDescription = null)
                Column(modifier = Modifier.weight(1f)) {
                    Text(headline, style = MaterialTheme.typography.labelLarge)
                    activeItem?.let { item ->
                        Text(
                            text = statusLabel(item),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (failedCount > 1) {
                    TextButton(onClick = onRetryAll) { Text("Retry all") }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse uploads" else "Show uploads"
                    )
                }
            }
            activeItem?.takeIf { it.status != PostUploadStatus.QUEUED }?.let { item ->
                LinearProgressIndicator(
                    progress = { item.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    items.forEachIndexed { index, item ->
                        UploadQueueRow(
                            item = item,
                            position = index + 1,
                            onRetry = { onRetry(item.id) },
                            onDiscard = { onDiscard(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadQueueRow(
    item: PostUploadItem,
    position: Int,
    onRetry: () -> Unit,
    onDiscard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.caption?.takeIf(String::isNotBlank) ?: "Moment $position",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusLabel(item),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.status == PostUploadStatus.FAILED) {
            TextButton(onClick = onDiscard) { Text("Remove") }
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun statusLabel(item: PostUploadItem): String = when (item.status) {
    PostUploadStatus.PREPARING -> "Preparing… ${(item.progress * 100).toInt()}%"
    PostUploadStatus.QUEUED -> "Waiting in queue"
    PostUploadStatus.SCHEDULED -> "Waiting for a connection"
    PostUploadStatus.UPLOADING -> "Uploading… ${(item.progress * 100).toInt()}%"
    PostUploadStatus.FAILED -> item.errorMessage ?: "Upload failed"
}
