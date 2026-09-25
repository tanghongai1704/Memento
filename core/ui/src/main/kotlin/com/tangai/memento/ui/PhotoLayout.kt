package com.tangai.memento.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tangai.memento.domain.model.LayoutType

@Composable
fun PhotoLayout(
    media: List<Any?>,
    layoutType: LayoutType,
    contentDescription: String,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp
) {
    if (media.isEmpty()) {
        PhotoCell(null, contentDescription, modifier.fillMaxWidth().height(height))
        return
    }
    when (if (media.size == 1) LayoutType.SINGLE else layoutType) {
        LayoutType.SINGLE -> PhotoCell(media.first(), contentDescription, modifier.fillMaxWidth().height(height))
        LayoutType.GRID -> GridPhotos(media, contentDescription, modifier, height)
        LayoutType.COLLAGE -> CollagePhotos(media, contentDescription, modifier, height)
        LayoutType.CAROUSEL -> CarouselPhotos(media, contentDescription, modifier, height)
    }
}

@Composable
private fun GridPhotos(media: List<Any?>, description: String, modifier: Modifier, height: Dp) {
    val rows = media.chunked(2)
    Column(modifier = modifier.fillMaxWidth().height(height), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (row.size == 1 && media.size > 1) {
                    PhotoCell(
                        row.first(),
                        "$description ${rowIndex * 2 + 1}",
                        Modifier.fillMaxSize()
                    )
                } else {
                    row.forEachIndexed { index, item ->
                        PhotoCell(
                            item,
                            "$description ${rowIndex * 2 + index + 1}",
                            Modifier.weight(1f).fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollagePhotos(media: List<Any?>, description: String, modifier: Modifier, height: Dp) {
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PhotoCell(media.first(), "$description 1", Modifier.weight(1.35f).fillMaxSize())
        Column(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            media.drop(1).forEachIndexed { index, item ->
                PhotoCell(item, "$description ${index + 2}", Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CarouselPhotos(media: List<Any?>, description: String, modifier: Modifier, height: Dp) {
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
                PhotoCell(item, "$description ${index + 1}", Modifier.width(itemWidth).fillMaxHeight())
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
private fun PhotoCell(model: Any?, description: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model == null) {
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
                Text(
                    "Connect to the internet to download it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            AsyncImage(
                model = model,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
