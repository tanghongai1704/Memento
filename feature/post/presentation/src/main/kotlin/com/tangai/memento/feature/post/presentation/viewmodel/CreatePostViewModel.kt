package com.tangai.memento.feature.post.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.LayoutType
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
                selectedMedia = pending?.localUris?.mapIndexed { index, uri ->
                        LocalMediaItem(
                            uri = uri,
                            type = MediaType.IMAGE,
                            displayName = "Pending photo ${index + 1}",
                            processedSizeBytes = pending.post.mediaItems.getOrNull(index)?.sizeBytes ?: 0L
                        )
                } ?: emptyList(),
                selectedLayout = pending?.post?.layoutType ?: LayoutType.SINGLE,
                caption = pending?.post?.caption.orEmpty(),
                pendingPhoto = pending,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
        viewModelScope.launch {
            connectionRepository.observeConnections().collect { result ->
                result.onSuccess { users ->
                    val usersById = users.associateBy { it.id }
                    val current = _uiState.value
                    _uiState.value = current.copy(
                        recipientLabels = current.recipients.associate { connection ->
                            connection.id to connection.displayLabel(usersById)
                        }
                    )
                }
            }
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
        val selected = (_uiState.value.selectedMedia + mediaWithName.copy(type = MediaType.IMAGE))
            .distinctBy(LocalMediaItem::uri)
            .take(MAX_PHOTOS_PER_POST)
        _uiState.value = _uiState.value.copy(
            selectedMedia = selected,
            selectedLayout = if (selected.size == 1) LayoutType.SINGLE else LayoutType.GRID,
            pendingPhoto = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun setSelectedMedia(media: List<LocalMediaItem>) {
        val selected = media.distinctBy(LocalMediaItem::uri).take(MAX_PHOTOS_PER_POST)
        _uiState.value = _uiState.value.copy(
            selectedMedia = selected,
            selectedLayout = if (selected.size <= 1) LayoutType.SINGLE else LayoutType.GRID,
            pendingPhoto = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onLayoutSelected(layoutType: LayoutType) {
        if (_uiState.value.selectedMedia.size > 1 && layoutType != LayoutType.SINGLE) {
            _uiState.value = _uiState.value.copy(selectedLayout = layoutType, errorMessage = null)
        }
    }

    fun removeSelectedMedia(uri: String) {
        val selected = _uiState.value.selectedMedia.filterNot { it.uri == uri }
        _uiState.value = _uiState.value.copy(
            selectedMedia = selected,
            selectedLayout = if (selected.size <= 1) LayoutType.SINGLE else _uiState.value.selectedLayout,
            pendingPhoto = null
        )
    }

    fun clearSelectedMedia() {
        _uiState.value = _uiState.value.copy(
            selectedMedia = emptyList(), selectedLayout = LayoutType.SINGLE, pendingPhoto = null
        )
    }

    fun discardPendingPhoto() {
        val pending = _uiState.value.pendingPhoto ?: return
        viewModelScope.launch {
            postRepository.discardPendingPhoto(pending)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        selectedMedia = emptyList(),
                        pendingPhoto = null,
                        uploadProgress = 0f,
                        errorMessage = null,
                        successMessage = "Pending photo discarded."
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Could not discard the pending photo."
                    )
                }
        }
    }

    fun onCaptionChanged(value: String) {
        if (value.length <= 1000) _uiState.value = _uiState.value.copy(caption = value, errorMessage = null)
    }

    fun confirmAndUploadSelectedMedia(onSuccess: (Post) -> Unit) {
        val recipient = _uiState.value.selectedRecipient
        val media = _uiState.value.selectedMedia
        if (recipient == null || media.isEmpty() || media.size > MAX_PHOTOS_PER_POST) {
            _uiState.value = _uiState.value.copy(errorMessage = "Choose a connection and 1–5 photos.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = _uiState.value.pendingPhoto == null,
                isUploading = false,
                processingProgress = 0f,
                processingMessage = "Resizing and compressing photos…",
                errorMessage = null,
                successMessage = null
            )
            val pendingResult = _uiState.value.pendingPhoto?.let { Result.success(it) }
                ?: postRepository.preparePhotoPost(
                    recipient.id,
                    media,
                    _uiState.value.selectedLayout,
                    _uiState.value.caption
                ) { progress ->
                    _uiState.value = _uiState.value.copy(processingProgress = progress)
                }
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
                selectedMedia = pending.localUris.mapIndexed { index, uri ->
                    val original = media[index]
                    val processedSize = pending.post.mediaItems[index].sizeBytes
                    original.copy(
                        uri = uri,
                        processedUri = uri,
                        processedSizeBytes = processedSize,
                        compressionRatio = if (original.originalSizeBytes > 0L) {
                            processedSize.toFloat() / original.originalSizeBytes.toFloat()
                        } else 1f
                    )
                },
                isProcessing = false,
                processingProgress = 1f,
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

    private companion object {
        const val MAX_PHOTOS_PER_POST = 5
    }

}
