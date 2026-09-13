package com.tangai.memento.feature.post.presentation.viewmodel

import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.Connection

data class CreatePostUiState(
    val recipients: List<Connection> = emptyList(),
    val selectedRecipient: Connection? = null,
    val selectedMedia: List<LocalMediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isUploading: Boolean = false,
    val processingProgress: Float = 0f,
    val uploadProgress: Float = 0f,
    val processingMessage: String = "Preparing media...",
    val errorMessage: String? = null
)
