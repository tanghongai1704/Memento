package com.tangai.memento.feature.post.presentation.viewmodel

import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.User

data class CreatePostUiState(
    val recipients: List<Connection> = emptyList(),
    val recipientLabels: Map<String, String> = emptyMap(),
    val recipientUsers: Map<String, User> = emptyMap(),
    val selectedRecipient: Connection? = null,
    val selectedMedia: List<LocalMediaItem> = emptyList(),
    val selectedLayout: LayoutType = LayoutType.SINGLE,
    val caption: String = "",
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val isUploading: Boolean = false,
    val processingProgress: Float = 0f,
    val uploadProgress: Float = 0f,
    val processingMessage: String = "Preparing media...",
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    fun labelFor(connection: Connection): String = recipientLabels[connection.id]
        ?: connection.name?.takeIf { it.isNotBlank() }
        ?: "Direct connection"

    fun userFor(connection: Connection): User? = recipientUsers[connection.id]
}
