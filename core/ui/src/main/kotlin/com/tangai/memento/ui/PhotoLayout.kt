package com.tangai.memento.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tangai.memento.domain.model.LayoutType

enum class MissingPhotoState { LOADING, UNAVAILABLE }

@Composable
fun PhotoLayout(
    media: List<Any?>,
    layoutType: LayoutType,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    onRemove: ((Int) -> Unit)? = null,
    onPhotoClick: ((Int) -> Unit)? = null,
    missingPhotoState: MissingPhotoState = MissingPhotoState.UNAVAILABLE,
    onRetry: (() -> Unit)? = null
) {
    if (media.isEmpty()) {
        PhotoCell(
            model = null,
            description = contentDescription,
            index = 0,
            onRemove = onRemove,
            onPhotoClick = onPhotoClick,
            missingPhotoState = missingPhotoState,
            onRetry = onRetry,
            modifier = modifier.fillMaxWidth().height(height)
        )
        return
    }
    when (if (media.size == 1) LayoutType.SINGLE else layoutType) {
        LayoutType.SINGLE -> PhotoCell(
            model = media.first(),
            description = contentDescription,
            index = 0,
            onRemove = onRemove,
            onPhotoClick = onPhotoClick,
            missingPhotoState = missingPhotoState,
            onRetry = onRetry,
            modifier = modifier.fillMaxWidth().height(height)
        )
        LayoutType.GRID -> GridPhotos(media, contentDescription, modifier, height, onRemove, onPhotoClick, missingPhotoState, onRetry)
        LayoutType.COLLAGE -> CollagePhotos(media, contentDescription, modifier, height, onRemove, onPhotoClick, missingPhotoState, onRetry)
        LayoutType.CAROUSEL -> CarouselPhotos(media, contentDescription, modifier, height, onRemove, onPhotoClick, missingPhotoState, onRetry)
    }
}

@Composable
private fun GridPhotos(
    media: List<Any?>,
    description: String,
    modifier: Modifier,
    height: Dp,
    onRemove: ((Int) -> Unit)?,
    onPhotoClick: ((Int) -> Unit)?,
    missingPhotoState: MissingPhotoState,
    onRetry: (() -> Unit)?
) {
    val rows = media.chunked(2)
    Column(
        modifier = modifier.fillMaxWidth().height(height),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (row.size == 1 && media.size > 1) {
                    PhotoCell(
                        model = row.first(),
                        description = "$description ${rowIndex * 2 + 1}",
                        index = rowIndex * 2,
                        onRemove = onRemove,
                        onPhotoClick = onPhotoClick,
                        missingPhotoState = missingPhotoState,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    row.forEachIndexed { index, item ->
                        val mediaIndex = rowIndex * 2 + index
                        PhotoCell(
                            model = item,
                            description = "$description ${mediaIndex + 1}",
                            index = mediaIndex,
                            onRemove = onRemove,
                            onPhotoClick = onPhotoClick,
                            missingPhotoState = missingPhotoState,
                            onRetry = onRetry,
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollagePhotos(
    media: List<Any?>,
    description: String,
    modifier: Modifier,
    height: Dp,
    onRemove: ((Int) -> Unit)?,
    onPhotoClick: ((Int) -> Unit)?,
    missingPhotoState: MissingPhotoState,
    onRetry: (() -> Unit)?
) {
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PhotoCell(
            model = media.first(),
            description = "$description 1",
            index = 0,
            onRemove = onRemove,
            onPhotoClick = onPhotoClick,
            missingPhotoState = missingPhotoState,
            onRetry = onRetry,
            modifier = Modifier.weight(1.35f).fillMaxSize()
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            media.drop(1).forEachIndexed { index, item ->
                PhotoCell(
                    model = item,
                    description = "$description ${index + 2}",
                    index = index + 1,
                    onRemove = onRemove,
                    onPhotoClick = onPhotoClick,
                    missingPhotoState = missingPhotoState,
                    onRetry = onRetry,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CarouselPhotos(
    media: List<Any?>,
    description: String,
    modifier: Modifier,
    height: Dp,
    onRemove: ((Int) -> Unit)?,
    onPhotoClick: ((Int) -> Unit)?,
    missingPhotoState: MissingPhotoState,
    onRetry: (() -> Unit)?
) {
    val listState = rememberLazyListState()
    val visiblePhoto by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtMost(media.size) }
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val itemWidth = maxWidth - 28.dp
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(media) { index, item ->
                PhotoCell(
                    model = item,
                    description = "$description ${index + 1}",
                    index = index,
                    onRemove = onRemove,
                    onPhotoClick = onPhotoClick,
                    missingPhotoState = missingPhotoState,
                    onRetry = onRetry,
                    modifier = Modifier.width(itemWidth).fillMaxHeight()
                )
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.84f),
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {
            Text(
                text = "$visiblePhoto/${media.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PhotoCell(
    model: Any?,
    description: String,
    index: Int,
    onRemove: ((Int) -> Unit)?,
    onPhotoClick: ((Int) -> Unit)?,
    missingPhotoState: MissingPhotoState,
    onRetry: (() -> Unit)?,
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (model != null && onPhotoClick != null) {
                    Modifier.clickable(role = Role.Button) { onPhotoClick(index) }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (model == null) {
            if (missingPhotoState == MissingPhotoState.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Photo unavailable",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (onRetry != null) {
                        TextButton(onClick = onRetry) { Text("Try again") }
                    }
                }
            }
        } else {
            AsyncImage(
                model = model,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (onRemove != null && model != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clickable(role = Role.Button) { onRemove(index) }
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
                        contentDescription = "Remove photo ${index + 1}",
                        modifier = Modifier.padding(7.dp)
                    )
                }
            }
        }
    }
}
