package com.tangai.memento.feature.post.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel
import com.tangai.memento.ui.MementoSectionTitle
import com.tangai.memento.ui.PhotoLayout

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun MediaPreviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: CreatePostViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var confirmDiscard by remember { mutableStateOf(false) }
    val isBusy = uiState.isProcessing || uiState.isUploading
    val hasPhotos = uiState.selectedMedia.isNotEmpty()
    val isKeyboardVisible = WindowInsets.isImeVisible
    val recipient = uiState.selectedRecipient?.let(uiState::userFor)
    val recipientName = recipient?.displayName?.takeIf(String::isNotBlank)
        ?: uiState.selectedRecipient?.let(uiState::labelFor)
        ?: "No connection selected"

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard upload draft?") },
            text = {
                Text("The prepared draft and its local files will be removed. Your original photos are not affected.")
            },
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
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep draft") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview moment") },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isBusy) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (hasPhotos && !isKeyboardVisible) {
                Surface(
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (uiState.pendingPhoto == null) onNavigateBack()
                                else confirmDiscard = true
                            },
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (uiState.pendingPhoto == null) "Edit photos" else "Discard")
                        }
                        Button(
                            onClick = {
                                viewModel.confirmAndUploadSelectedMedia { onNavigateToHome() }
                            },
                            enabled = !isBusy && uiState.selectedRecipient != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(
                                when {
                                    uiState.isProcessing -> "Preparing…"
                                    uiState.isUploading -> "Posting…"
                                    uiState.pendingPhoto != null -> "Retry post"
                                    else -> "Post moment"
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (!hasPhotos) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "No photos selected",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        "Go back and choose at least one photo for this moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                    )
                    Button(onClick = onNavigateBack) { Text("Choose photos") }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Column {
                            MementoSectionTitle(text = "Your moment")
                            Text(
                                "${uiState.selectedMedia.size} ${if (uiState.selectedMedia.size == 1) "photo" else "photos"} selected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        PhotoLayout(
                            media = uiState.selectedMedia.map { it.thumbnailUri ?: it.uri },
                            layoutType = uiState.selectedLayout,
                            contentDescription = "Moment preview",
                            height = 300.dp
                        )

                        if (uiState.selectedMedia.size > 1 && uiState.pendingPhoto == null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Choose a layout",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val layoutChoices = if (uiState.selectedMedia.size >= 5) {
                                listOf(LayoutType.GRID, LayoutType.CAROUSEL)
                            } else {
                                listOf(LayoutType.GRID, LayoutType.COLLAGE, LayoutType.CAROUSEL)
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                items(layoutChoices) { layout ->
                                    FilterChip(
                                        onClick = { viewModel.onLayoutSelected(layout) },
                                        selected = uiState.selectedLayout == layout,
                                        label = { Text(layout.displayLabel()) }
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            overlineContent = {
                                Text(
                                    "Sharing with",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = recipientName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = recipient?.username
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
                                RecipientAvatar(
                                    user = recipient,
                                    displayName = recipientName,
                                    size = 44.dp
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        OutlinedTextField(
                            value = uiState.caption,
                            onValueChange = viewModel::onCaptionChanged,
                            label = { Text("Add a caption") },
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Optional · ${uiState.caption.length}/1000",
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isKeyboardVisible) {
                                        TextButton(
                                            onClick = { focusManager.clearFocus() },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Done")
                                        }
                                    }
                                }
                            },
                            enabled = !isBusy,
                            minLines = 3,
                            maxLines = 5,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }

                when {
                    uiState.isProcessing -> ProgressStatus(
                        progress = uiState.processingProgress,
                        label = uiState.processingMessage
                    )
                    uiState.isUploading -> ProgressStatus(
                        progress = uiState.uploadProgress,
                        label = "Uploading photos…"
                    )
                }

                uiState.errorMessage?.let { message ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressStatus(progress: Float, label: String) {
    val safeProgress = progress.coerceIn(0f, 1f)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    style = MaterialTheme.typography.labelLarge
                )
            }
            LinearProgressIndicator(
                progress = { safeProgress },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

private fun LayoutType.displayLabel(): String = when (this) {
    LayoutType.SINGLE -> "Single"
    LayoutType.GRID -> "Grid"
    LayoutType.COLLAGE -> "Collage"
    LayoutType.CAROUSEL -> "Carousel"
}
