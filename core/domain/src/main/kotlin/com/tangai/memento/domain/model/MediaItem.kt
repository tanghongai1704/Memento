package com.tangai.memento.domain.model

data class MediaItem(
    val mediaId: String,
    val mediaType: MediaType,
    val storagePath: String,
    val thumbnailPath: String?,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val durationMs: Long?,
    val sizeBytes: Long,
    val position: Int
)
