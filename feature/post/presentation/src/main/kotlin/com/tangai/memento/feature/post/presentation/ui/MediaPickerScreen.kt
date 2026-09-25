package com.tangai.memento.feature.post.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel

private const val MaxPhotosPerMoment = 5

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MediaPickerScreen(
    onNavigateToPreview: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CreatePostViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCount = uiState.selectedMedia.size
    val selectedMediaState = rememberUpdatedState(uiState.selectedMedia)
    val supportsPickerPreselection = supportsPickerPreselection()
    val pickerContract = remember {
        PreselectedPhotoPickerContract {
            selectedMediaState.value.map { media -> Uri.parse(media.uri) }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = pickerContract
    ) { uris ->
        if (uris.isNotEmpty()) {
            val pickedMedia = uris.mapIndexed { index, uri ->
                val originalSize = context.contentResolver.openAssetFileDescriptor(uri, "r")
                    ?.use { it.length.coerceAtLeast(0L) }
                    ?: 0L
                LocalMediaItem(
                    uri = uri.toString(),
                    type = MediaType.IMAGE,
                    displayName = "photo_${System.currentTimeMillis()}_$index",
                    originalSizeBytes = originalSize
                )
            }
            viewModel.setSelectedMedia(
                if (supportsPickerPreselection) pickedMedia
                else selectedMediaState.value + pickedMedia
            )
        }
    }
    val launchPicker = {
        photoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select photos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = "$selectedCount/$MaxPhotosPerMoment",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onNavigateToPreview,
                    enabled = selectedCount > 0,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(
                        if (selectedCount == 0) "Select photos to continue"
                        else "Continue with $selectedCount ${if (selectedCount == 1) "photo" else "photos"}"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedCount == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(300.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Outlined.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.padding(18.dp).size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text("Build your moment", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Choose up to five photos. You can review their order and layout before posting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                        )
                        Button(onClick = launchPicker) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Choose photos")
                        }
                    }
                }
            } else {
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
                                Text(
                                    "Selected photos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Tap × to remove a photo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = viewModel::clearSelectedMedia) {
                                Text("Clear all")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.selectedMedia.chunked(2).forEachIndexed { rowIndex, rowMedia ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowMedia.forEachIndexed { columnIndex, media ->
                                        val position = rowIndex * 2 + columnIndex + 1
                                        SelectedPhotoThumbnail(
                                            media = media,
                                            position = position,
                                            onRemove = { viewModel.removeSelectedMedia(media.uri) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowMedia.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedCount < MaxPhotosPerMoment) {
                    OutlinedButton(onClick = launchPicker, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Add more photos · ${MaxPhotosPerMoment - selectedCount} left")
                    }
                } else {
                    Text(
                        "Maximum of five photos selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private class PreselectedPhotoPickerContract(
    private val selectedUris: () -> List<Uri>
) : ActivityResultContracts.PickMultipleVisualMedia(MaxPhotosPerMoment) {
    override fun createIntent(context: Context, input: PickVisualMediaRequest): Intent =
        super.createIntent(context, input).apply {
            if (
                supportsPickerPreselection() &&
                action == MediaStore.ACTION_PICK_IMAGES
            ) {
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

@Composable
private fun SelectedPhotoThumbnail(
    media: LocalMediaItem,
    position: Int,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = media.thumbnailUri ?: media.uri,
            contentDescription = "Selected photo $position",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                Text(
                    text = position.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onRemove)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                shadowElevation = 2.dp,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove photo $position",
                    modifier = Modifier.padding(7.dp)
                )
            }
        }
    }
}
