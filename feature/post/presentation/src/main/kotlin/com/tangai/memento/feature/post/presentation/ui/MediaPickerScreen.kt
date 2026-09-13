package com.tangai.memento.feature.post.presentation.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangai.memento.domain.model.MediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.feature.post.presentation.viewmodel.CreatePostViewModel

@Composable
fun MediaPickerScreen(
    onNavigateToPreview: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CreatePostViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val singlePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mediaType = MediaType.IMAGE
            viewModel.addSelectedMedia(MediaItem(uri = uri.toString(), type = mediaType, displayName = "photo_${System.currentTimeMillis()}"))
        }
    }
    val multiplePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        val items = uris.map { uri ->
            MediaItem(uri = uri.toString(), type = MediaType.IMAGE, displayName = "photo_${System.currentTimeMillis()}")
        }
        viewModel.addSelectedMedia(items)
    }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.addSelectedMedia(MediaItem(uri = uri.toString(), type = MediaType.VIDEO, displayName = "video_${System.currentTimeMillis()}"))
        }
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
                .padding(16.dp)
        ) {
            Text(
                text = "Select Media",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            Button(
                onClick = { singlePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Choose Photo")
            }

            Button(
                onClick = { multiplePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Choose Photos")
            }

            Button(
                onClick = { videoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Choose Video")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.selectedMedia.isNotEmpty()) {
                Text(
                    text = "Selected (${uiState.selectedMedia.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(uiState.selectedMedia) { media ->
                        val previewBitmap = remember(media.uri) {
                            loadBitmapFromUri(context, media.thumbnailUri ?: media.uri)
                        }
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateToPreview() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap.asImageBitmap(),
                                    contentDescription = media.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = if (media.type == MediaType.VIDEO) "Video" else "Photo",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { viewModel.removeSelectedMedia(media.uri) },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("x")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onNavigateToPreview,
                enabled = uiState.selectedMedia.isNotEmpty(),
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Preview")
            }

            Button(
                onClick = onNavigateBack,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Back")
            }
        }
    }
}

private fun loadBitmapFromUri(context: Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (_: Exception) {
        null
    }
}
