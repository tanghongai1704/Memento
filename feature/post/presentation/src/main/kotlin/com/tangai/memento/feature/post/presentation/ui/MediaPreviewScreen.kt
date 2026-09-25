package com.tangai.memento.feature.post.presentation.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel
import java.util.Locale
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.ui.PhotoLayout
import com.tangai.memento.ui.MementoSectionTitle

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MediaPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: CreatePostViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    val previewMedia = uiState.selectedMedia.firstOrNull()

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard pending photo?") },
            text = { Text("The local draft will be removed. An unfinished server upload is cleaned up after the safety window.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        viewModel.discardPendingPhoto()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            PhotoLayout(
                media = uiState.selectedMedia.map { it.thumbnailUri ?: it.uri },
                layoutType = uiState.selectedLayout,
                contentDescription = "Media preview",
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                height = 320.dp
            )

            if (uiState.selectedMedia.size > 1 && uiState.pendingPhoto == null) {
                MementoSectionTitle(text = "Layout")
                Row {
                    val layoutChoices = if (uiState.selectedMedia.size >= 5) {
                        listOf(LayoutType.GRID, LayoutType.CAROUSEL)
                    } else {
                        listOf(LayoutType.GRID, LayoutType.COLLAGE, LayoutType.CAROUSEL)
                    }
                    layoutChoices.forEach { layout ->
                        FilterChip(
                            onClick = { viewModel.onLayoutSelected(layout) },
                            selected = uiState.selectedLayout == layout,
                            label = { Text(layout.name.lowercase().replaceFirstChar(Char::uppercase)) },
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }

            if (previewMedia != null) {
                Text(
                    text = "${uiState.selectedMedia.size} photo(s) · ready to post",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
                val originalSize = uiState.selectedMedia.sumOf { it.originalSizeBytes }
                val processedSize = uiState.selectedMedia.sumOf { it.processedSizeBytes }
                if (processedSize > 0L) {
                    Text(
                        text = if (originalSize > 0L) {
                            "${formatBytes(originalSize)} → ${formatBytes(processedSize)} " +
                                "(${((1f - processedSize.toFloat() / originalSize.toFloat())
                                    .coerceIn(0f, 1f) * 100).toInt()}% smaller)"
                        } else {
                            "Compressed size: ${formatBytes(processedSize)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (originalSize > 0L) {
                    Text(
                        text = "Original size: ${formatBytes(originalSize)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ListItem(
                overlineContent = {
                    Text(
                        "Sharing with",
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                headlineContent = {
                    Text(uiState.selectedRecipient?.let(uiState::labelFor) ?: "No connection")
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            )

            OutlinedTextField(
                value = uiState.caption,
                onValueChange = viewModel::onCaptionChanged,
                label = { Text("Caption (optional)") },
                supportingText = { Text("${uiState.caption.length}/1000") },
                enabled = !uiState.isProcessing && !uiState.isUploading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isProcessing) {
                ProgressStatus(
                    progress = uiState.processingProgress,
                    label = uiState.processingMessage
                )
            } else if (uiState.isUploading) {
                ProgressStatus(
                    progress = uiState.uploadProgress,
                    label = "Uploading media…"
                )
            } else if (uiState.selectedMedia.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            if (uiState.pendingPhoto == null) {
                                viewModel.clearSelectedMedia()
                                onNavigateBack()
                            } else {
                                confirmDiscard = true
                            }
                        },
                        enabled = previewMedia != null,
                        modifier = Modifier.weight(1f).padding(4.dp)
                    ) {
                        Text(if (uiState.pendingPhoto == null) "Change photos" else "Discard draft")
                    }

                    Button(
                        onClick = {
                            viewModel.confirmAndUploadSelectedMedia { newPost ->
                                onNavigateToHome()
                            }
                        },
                        modifier = Modifier.weight(1f).padding(4.dp)
                    ) {
                        Text(if (uiState.pendingPhoto == null) "Post photos" else "Retry upload")
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressStatus(progress: Float, label: String) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(safeProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { safeProgress },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}
