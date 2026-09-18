package com.tangai.memento.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    height: Dp = 280.dp
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
        rows.forEach { row ->
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { index, item ->
                    PhotoCell(item, "$description ${index + 1}", Modifier.weight(1f).fillMaxSize())
                }
                if (row.size == 1 && media.size > 1) Box(Modifier.weight(1f))
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
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val itemWidth = maxWidth - 28.dp
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(media) { index, item ->
                PhotoCell(item, "$description ${index + 1}", Modifier.width(itemWidth).height(height))
            }
        }
    }
}

@Composable
private fun PhotoCell(model: Any?, description: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (model == null) {
            Text("Photo unavailable offline", modifier = Modifier.padding(12.dp))
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
