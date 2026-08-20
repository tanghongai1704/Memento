package com.tangai.memento.feature.post.presentation.viewmodel

import com.tangai.memento.domain.model.MediaItem
import com.tangai.memento.domain.model.User

data class CreatePostUiState(
    val selectedRecipient: User? = null,
    val selectedMedia: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null
)
