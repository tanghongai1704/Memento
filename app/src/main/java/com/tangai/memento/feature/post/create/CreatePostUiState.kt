package com.tangai.memento.feature.post.create

import com.tangai.memento.data.model.MediaItem
import com.tangai.memento.data.model.User

data class CreatePostUiState(
    val selectedRecipient: User? = null,
    val selectedMedia: List<MediaItem> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null
)
