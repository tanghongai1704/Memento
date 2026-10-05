package com.tangai.memento.feature.post.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostUiState
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel
import com.tangai.memento.ui.MementoSectionTitle
import com.tangai.memento.ui.PhotoLayout

private const val MaxPhotosPerMoment = 5

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun MediaPickerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToConnections: () -> Unit,
    viewModel: CreatePostViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMediaState = rememberUpdatedState(uiState.selectedMedia)
    val supportsPickerPreselection = supportsPickerPreselection()
    val isBusy = uiState.isProcessing || uiState.isUploading
    val canEdit = !isBusy
    val isKeyboardVisible = WindowInsets.isImeVisible
    var showRecipientPicker by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val hasUnsavedChanges = (
        uiState.selectedMedia.isNotEmpty() || uiState.caption.isNotBlank()
    )
    val requestBack = {
        if (hasUnsavedChanges) confirmExit = true else onNavigateBack()
    }

    BackHandler {
        if (!isBusy) requestBack()
    }

    val pickerContract = remember {
        PreselectedPhotoPickerContract {
            selectedMediaState.value.map { media -> media.uri.toUri() }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(contract = pickerContract) { uris ->
        if (uris.isNotEmpty()) {
            val pickedMedia = uris.mapIndexed { index, uri ->
                LocalMediaItem(
                    uri = uri.toString(),
                    type = MediaType.IMAGE,
                    displayName = "photo_${System.currentTimeMillis()}_$index"
                )
            }
            viewModel.setSelectedMedia(
                if (supportsPickerPreselection) pickedMedia
                else selectedMediaState.value + pickedMedia
            )
        }
    }
    val launchPicker = {
        if (canEdit) {
            photoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    if (showRecipientPicker) {
        RecipientPickerSheet(
            state = uiState,
            onSelect = { connection ->
                viewModel.onRecipientSelected(connection)
                showRecipientPicker = false
            },
            onDismiss = { showRecipientPicker = false }
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Discard this moment?") },
            text = { Text("Your selected photos and caption will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create moment") },
                navigationIcon = {
                    IconButton(onClick = requestBack, enabled = !isBusy) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isLoading && uiState.recipients.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = "${uiState.selectedMedia.size}/$MaxPhotosPerMoment",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (
                !uiState.isLoading &&
                uiState.recipients.isNotEmpty() &&
                !isKeyboardVisible
            ) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.confirmAndUploadSelectedMedia { onNavigateToHome() }
                            },
                            enabled = !isBusy &&
                                uiState.selectedRecipient != null &&
                                uiState.selectedMedia.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(
                                when {
                                    uiState.isProcessing -> "Preparing…"
                                    uiState.isUploading -> "Posting…"
                                    uiState.selectedMedia.isEmpty() -> "Add photos to post"
                                    else -> "Post moment"
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingConnections(modifier = Modifier.padding(innerPadding))

            uiState.errorMessage != null && uiState.recipients.isEmpty() -> ConnectionLoadError(
                message = uiState.errorMessage.orEmpty(),
                onRetry = viewModel::retryLoadingConnections,
                modifier = Modifier.padding(innerPadding)
            )

            uiState.recipients.isEmpty() -> NoConnections(
                onNavigateToConnections = onNavigateToConnections,
                modifier = Modifier.padding(innerPadding)
            )

            else -> CreateMomentContent(
                state = uiState,
                canEdit = canEdit,
                isKeyboardVisible = isKeyboardVisible,
                onChangeRecipient = { showRecipientPicker = true },
                onChoosePhotos = launchPicker,
                onRemovePhoto = { index ->
                    uiState.selectedMedia.getOrNull(index)?.let { media ->
                        viewModel.removeSelectedMedia(media.uri)
                    }
                },
                onClearPhotos = viewModel::clearSelectedMedia,
                onLayoutSelected = viewModel::onLayoutSelected,
                onCaptionChanged = viewModel::onCaptionChanged,
                onDoneCaption = { focusManager.clearFocus() },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun CreateMomentContent(
    state: CreatePostUiState,
    canEdit: Boolean,
    isKeyboardVisible: Boolean,
    onChangeRecipient: () -> Unit,
    onChoosePhotos: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onClearPhotos: () -> Unit,
    onLayoutSelected: (LayoutType) -> Unit,
    onCaptionChanged: (String) -> Unit,
    onDoneCaption: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SharingSection(
            state = state,
            enabled = canEdit,
            onChangeRecipient = onChangeRecipient
        )

        PhotosSection(
            state = state,
            canEdit = canEdit,
            onChoosePhotos = onChoosePhotos,
            onRemovePhoto = onRemovePhoto,
            onClearPhotos = onClearPhotos,
            onLayoutSelected = onLayoutSelected
        )

        CaptionSection(
            caption = state.caption,
            enabled = canEdit,
            isKeyboardVisible = isKeyboardVisible,
            onCaptionChanged = onCaptionChanged,
            onDone = onDoneCaption
        )

        when {
            state.isProcessing -> ProgressStatus(
                progress = state.processingProgress,
                label = "Getting your moment ready…"
            )
            state.isUploading -> ProgressStatus(
                progress = state.uploadProgress,
                label = "Posting your moment…"
            )
        }

        state.errorMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SharingSection(
    state: CreatePostUiState,
    enabled: Boolean,
    onChangeRecipient: () -> Unit
) {
    val connection = state.selectedRecipient
    val recipient = connection?.let(state::userFor)
    val displayName = recipient?.displayName?.takeIf(String::isNotBlank)
        ?: connection?.let(state::labelFor)
        ?: "Choose a connection"

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onChangeRecipient)
    ) {
        ListItem(
            overlineContent = {
                Text("Sharing with", color = MaterialTheme.colorScheme.primary)
            },
            headlineContent = {
                Text(
                    text = displayName,
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
                    displayName = displayName,
                    size = 44.dp
                )
            },
            trailingContent = {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Change recipient")
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        )
    }
}

@Composable
private fun PhotosSection(
    state: CreatePostUiState,
    canEdit: Boolean,
    onChoosePhotos: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onClearPhotos: () -> Unit,
    onLayoutSelected: (LayoutType) -> Unit
) {
    val selectedCount = state.selectedMedia.size
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MementoSectionTitle(text = "Photos")
                    Text(
                        text = if (selectedCount == 0) {
                            "Choose up to five photos"
                        } else {
                            "$selectedCount ${if (selectedCount == 1) "photo" else "photos"} selected"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (selectedCount > 0 && canEdit) {
                    TextButton(onClick = onClearPhotos) {
                        Text("Clear all")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedCount == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.padding(14.dp).size(28.dp)
                            )
                        }
                        Text(
                            "Add photos to your moment",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)
                        )
                        Button(onClick = onChoosePhotos, enabled = canEdit) {
                            Text("Choose photos")
                        }
                    }
                }
            } else {
                PhotoLayout(
                    media = state.selectedMedia.map { it.thumbnailUri ?: it.uri },
                    layoutType = state.selectedLayout,
                    contentDescription = "Moment photo",
                    height = 300.dp,
                    onRemove = if (canEdit) onRemovePhoto else null
                )

                if (canEdit && selectedCount < MaxPhotosPerMoment) {
                    OutlinedButton(
                        onClick = onChoosePhotos,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Add more · ${MaxPhotosPerMoment - selectedCount} left")
                    }
                }

                if (selectedCount > 1) {
                    Text(
                        "Layout",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    val layoutChoices = if (selectedCount >= MaxPhotosPerMoment) {
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
                                onClick = { onLayoutSelected(layout) },
                                selected = state.selectedLayout == layout,
                                enabled = canEdit,
                                label = { Text(layout.displayLabel()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionSection(
    caption: String,
    enabled: Boolean,
    isKeyboardVisible: Boolean,
    onCaptionChanged: (String) -> Unit,
    onDone: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MementoSectionTitle(text = "Caption")
            OutlinedTextField(
                value = caption,
                onValueChange = onCaptionChanged,
                label = { Text("Write something about this moment") },
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Optional · ${caption.length}/1000",
                            modifier = Modifier.weight(1f)
                        )
                        if (isKeyboardVisible) {
                            TextButton(
                                onClick = onDone,
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                },
                enabled = enabled,
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RecipientPickerSheet(
    state: CreatePostUiState,
    onSelect: (Connection) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Share this moment with",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Text(
            "Choose one private connection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(state.recipients, key = Connection::id) { connection ->
                val selected = state.selectedRecipient?.id == connection.id
                val user = state.userFor(connection)
                val displayName = user?.displayName?.takeIf(String::isNotBlank)
                    ?: state.labelFor(connection)
                ListItem(
                    headlineContent = {
                        Text(
                            displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = user?.username
                        ?.takeIf(String::isNotBlank)
                        ?.let { username ->
                            {
                                Text(
                                    "@$username",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                    leadingContent = {
                        RecipientAvatar(user = user, displayName = displayName, size = 44.dp)
                    },
                    trailingContent = {
                        RadioButton(selected = selected, onClick = { onSelect(connection) })
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
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onSelect(connection) }
                )
            }
        }
    }
}

@Composable
private fun LoadingConnections(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Loading connections…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConnectionLoadError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text("Couldn’t load connections", style = MaterialTheme.typography.titleLarge)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun NoConnections(
    onNavigateToConnections: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.PeopleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text("Connect before sharing", style = MaterialTheme.typography.titleLarge)
            Text(
                "Memento shares each moment privately with one connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onNavigateToConnections) {
                Text("Add a connection")
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
        Column(modifier = Modifier.padding(16.dp)) {
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

private class PreselectedPhotoPickerContract(
    private val selectedUris: () -> List<Uri>
) : ActivityResultContracts.PickMultipleVisualMedia(MaxPhotosPerMoment) {
    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent =
        super.createIntent(context, input).apply {
            if (supportsPickerPreselection() && action == MediaStore.ACTION_PICK_IMAGES) {
                putParcelableArrayListExtra(
                    PickerPreselectionExtra,
                    ArrayList(selectedUris())
                )
            }
        }
}

private const val PickerPreselectionExtra = "android.provider.extra.PICKER_PRE_SELECTION_URIS"

private fun supportsPickerPreselection(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA ||
        (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 15
            )

private fun LayoutType.displayLabel(): String = when (this) {
    LayoutType.SINGLE -> "Single"
    LayoutType.GRID -> "Grid"
    LayoutType.COLLAGE -> "Collage"
    LayoutType.CAROUSEL -> "Carousel"
}
