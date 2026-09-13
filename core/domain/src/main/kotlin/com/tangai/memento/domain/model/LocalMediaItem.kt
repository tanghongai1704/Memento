package com.tangai.memento.domain.model

data class LocalMediaItem(
    val uri: String,
    val type: MediaType,
    val displayName: String = "",
    val originalSizeBytes: Long = 0L,
    val processedSizeBytes: Long = 0L,
    val compressionRatio: Float = 1f,
    val processedUri: String? = null,
    val thumbnailUri: String? = null
)
