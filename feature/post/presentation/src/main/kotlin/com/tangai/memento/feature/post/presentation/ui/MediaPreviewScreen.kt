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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel
import java.util.Locale
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.ui.PhotoLayout

@Composable
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
                Button(onClick = {
                    confirmDiscard = false
                    viewModel.discardPendingPhoto()
                }) { Text("Discard") }
            },
            dismissButton = {
                Button(onClick = { confirmDiscard = false }) { Text("Keep") }
            }
        )
    }

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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            PhotoLayout(
                media = uiState.selectedMedia.map { it.thumbnailUri ?: it.uri },
                layoutType = uiState.selectedLayout,
                contentDescription = "Media preview",
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                height = 320.dp
            )

            if (uiState.selectedMedia.size > 1 && uiState.pendingPhoto == null) {
                Text("Layout", style = MaterialTheme.typography.titleSmall)
                Row {
                    listOf(LayoutType.GRID, LayoutType.COLLAGE, LayoutType.CAROUSEL).forEach { layout ->
                        Button(
                            onClick = { viewModel.onLayoutSelected(layout) },
                            enabled = uiState.selectedLayout != layout,
                            modifier = Modifier.padding(4.dp)
                        ) { Text(layout.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    }
                }
            }

            if (previewMedia != null) {
                Text(
                    text = "${uiState.selectedMedia.size} photo(s) · ready to post",
                    fontSize = 16.sp,
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

            Text(
                text = "Share with: ${uiState.selectedRecipient?.let(uiState::labelFor) ?: "No connection"}",
                fontSize = 16.sp
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
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.processingMessage)
                Text("${(uiState.processingProgress * 100).toInt()}%")
            } else if (uiState.isUploading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Uploading media...")
                Text("${(uiState.uploadProgress * 100).toInt()}%")
            } else if (uiState.selectedMedia.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (uiState.pendingPhoto == null) {
                                viewModel.clearSelectedMedia()
                                onNavigateBack()
                            } else {
                                confirmDiscard = true
                            }
                        },
                        enabled = previewMedia != null,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(if (uiState.pendingPhoto == null) "Change photos" else "Discard draft")
                    }

                    Button(
                        onClick = {
                            viewModel.confirmAndUploadSelectedMedia { newPost ->
                                onNavigateToHome()
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(if (uiState.pendingPhoto == null) "Post photos" else "Retry upload")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Back")
            }

            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}
