package com.tangai.memento.feature.post.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.displayLabel
import com.tangai.memento.feature.post.domain.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val connectionRepository: com.tangai.memento.feature.connection.domain.ConnectionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    init {
        viewModelScope.launch {
            val result = connectionRepository.getCurrentUserConnections()
            val recipients = result.getOrDefault(emptyList())
            val connectedUsers = if (recipients.isEmpty()) {
                emptyList()
            } else {
                connectionRepository.loadConnections().getOrDefault(emptyList())
            }
            val connectedUsersById = connectedUsers.associateBy { it.id }
            val pending = postRepository.getLatestPendingPhoto().getOrNull()
            _uiState.value = _uiState.value.copy(
                recipients = recipients,
                recipientLabels = recipients.associate { connection ->
                    connection.id to connection.displayLabel(connectedUsersById)
                },
                selectedRecipient = pending?.post?.connectionId?.let { id -> recipients.find { it.id == id } },
                selectedMedia = pending?.let {
                    listOf(LocalMediaItem(uri = it.localUri, type = MediaType.IMAGE, displayName = "Pending photo"))
                } ?: emptyList(),
                caption = pending?.post?.caption.orEmpty(),
                pendingPhoto = pending,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }

    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onRecipientSelected(user: Connection) {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = user,
            errorMessage = null
        )
    }

    fun setDefaultRecipient(user: Connection) {
        _uiState.value = _uiState.value.copy(selectedRecipient = user)
    }

    fun addSelectedMedia(media: LocalMediaItem) {
        val mediaWithName = media.copy(displayName = media.displayName.ifEmpty { "media_${System.currentTimeMillis()}" })
        _uiState.value = _uiState.value.copy(
            selectedMedia = listOf(mediaWithName.copy(type = MediaType.IMAGE)),
            pendingPhoto = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun removeSelectedMedia(uri: String) {
        _uiState.value = _uiState.value.copy(
            selectedMedia = _uiState.value.selectedMedia.filterNot { it.uri == uri },
            pendingPhoto = null
        )
    }

    fun clearSelectedMedia() {
        _uiState.value = _uiState.value.copy(selectedMedia = emptyList(), pendingPhoto = null)
    }

    fun onCaptionChanged(value: String) {
        if (value.length <= 1000) _uiState.value = _uiState.value.copy(caption = value, errorMessage = null)
    }

    fun confirmAndUploadSelectedMedia(onSuccess: (Post) -> Unit) {
        val recipient = _uiState.value.selectedRecipient
        val media = _uiState.value.selectedMedia.singleOrNull()
        if (recipient == null || media == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Choose a connection and one photo.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = _uiState.value.pendingPhoto == null,
                isUploading = false,
                processingMessage = "Resizing and compressing photo…",
                errorMessage = null,
                successMessage = null
            )
            val pendingResult = _uiState.value.pendingPhoto?.let { Result.success(it) }
                ?: postRepository.preparePhotoPost(recipient.id, media, _uiState.value.caption)
            val pending = pendingResult.getOrNull()
            if (pending == null) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = pendingResult.exceptionOrNull()?.message ?: "Could not prepare the photo."
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                pendingPhoto = pending,
                selectedMedia = listOf(media.copy(uri = pending.localUri)),
                isProcessing = false,
                isUploading = true,
                uploadProgress = 0f
            )
            val result = postRepository.uploadPendingPhoto(pending) { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }
            _uiState.value = _uiState.value.copy(
                isUploading = false,
                uploadProgress = if (result.isSuccess) 1f else _uiState.value.uploadProgress,
                pendingPhoto = if (result.isSuccess) null else pending,
                successMessage = if (result.isSuccess) "Photo posted." else null,
                errorMessage = result.exceptionOrNull()?.message
            )
            result.getOrNull()?.let(onSuccess)
        }
    }

}
