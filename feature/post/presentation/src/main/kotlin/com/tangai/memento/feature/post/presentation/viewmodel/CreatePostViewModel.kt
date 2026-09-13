package com.tangai.memento.feature.post.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.AudienceType
import com.tangai.memento.domain.model.MediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.post.domain.PostRepository
import com.tangai.memento.feature.post.presentation.util.MediaProcessingPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CreatePostUiState(
            recipients = listOf(
                User("user_alice", "Alice"),
                User("user_bob", "Bob"),
                User("user_charlie", "Charlie")
            )
        )
    )
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onRecipientSelected(user: User) {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = user,
            errorMessage = null
        )
    }

    fun setDefaultRecipient(user: User) {
        _uiState.value = _uiState.value.copy(selectedRecipient = user)
    }

    fun addSelectedMedia(media: MediaItem) {
        val mediaWithName = media.copy(displayName = media.displayName.ifEmpty { "media_${System.currentTimeMillis()}" })
        val existing = _uiState.value.selectedMedia
        if (existing.any { it.uri == mediaWithName.uri }) {
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedMedia = existing + mediaWithName,
            errorMessage = null
        )
    }

    fun addSelectedMedia(mediaList: List<MediaItem>) {
        if (mediaList.isEmpty()) return
        val merged = _uiState.value.selectedMedia + mediaList.filter { item ->
            _uiState.value.selectedMedia.none { it.uri == item.uri }
        }
        _uiState.value = _uiState.value.copy(
            selectedMedia = merged,
            errorMessage = null
        )
    }

    fun removeSelectedMedia(uri: String) {
        _uiState.value = _uiState.value.copy(
            selectedMedia = _uiState.value.selectedMedia.filterNot { it.uri == uri }
        )
    }

    fun clearSelectedMedia() {
        _uiState.value = _uiState.value.copy(selectedMedia = emptyList())
    }

    fun confirmSelectedMedia(onReady: () -> Unit) {
        val selectedMedia = _uiState.value.selectedMedia
        if (selectedMedia.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select at least one media item"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingProgress = 0f,
                processingMessage = "Detecting media type...",
                errorMessage = null
            )

            val processedMedia = selectedMedia.mapIndexed { index, media ->
                _uiState.value = _uiState.value.copy(
                    processingProgress = (index.toFloat() / selectedMedia.size.toFloat()),
                    processingMessage = if (media.type == MediaType.VIDEO) {
                        "Processing video..."
                    } else {
                        "Resizing and compressing image..."
                    }
                )
                MediaProcessingPipeline.processMedia(context, media)
            }

            _uiState.value = _uiState.value.copy(
                selectedMedia = processedMedia,
                isProcessing = false,
                processingProgress = 1f,
                processingMessage = "Ready for upload",
                errorMessage = null
            )
            onReady()
        }
    }

    fun confirmAndUploadSelectedMedia(onSuccess: (Post) -> Unit) {
        val recipient = _uiState.value.selectedRecipient
        if (recipient == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select a recipient"
            )
            return
        }

        if (_uiState.value.selectedMedia.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select media before posting"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                processingProgress = 0f,
                processingMessage = "Preparing media...",
                errorMessage = null
            )

            val processedMedia = _uiState.value.selectedMedia.mapIndexed { index, media ->
                _uiState.value = _uiState.value.copy(
                    processingProgress = (index.toFloat() / _uiState.value.selectedMedia.size.toFloat()),
                    processingMessage = if (media.type == MediaType.VIDEO) {
                        "Processing video..."
                    } else {
                        "Resizing and compressing image..."
                    }
                )
                MediaProcessingPipeline.processMedia(context, media)
            }

            _uiState.value = _uiState.value.copy(
                selectedMedia = processedMedia,
                isProcessing = false,
                processingProgress = 1f,
                processingMessage = "Uploading media...",
                isUploading = true,
                uploadProgress = 0f
            )

            postRepository.uploadMedia(processedMedia) { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }

            val mediaType = if (processedMedia.any { it.type == MediaType.VIDEO }) MediaType.VIDEO else MediaType.IMAGE
            val newPost = Post(
                id = "post_" + System.currentTimeMillis().toString(),
                authorId = "user_current",
                audienceType = AudienceType.USER,
                audienceId = recipient.id,
                createdAt = System.currentTimeMillis(),
                mediaType = mediaType
            )

            _uiState.value = _uiState.value.copy(
                isUploading = false,
                uploadProgress = 0f,
                processingMessage = "Upload complete"
            )
            onSuccess(newPost)
        }
    }

    fun simulatePostCreation(onSuccess: (Post) -> Unit) {
        val recipient = _uiState.value.selectedRecipient
        if (recipient == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select a recipient"
            )
            return
        }

        if (_uiState.value.selectedMedia.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select media before posting"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)

            postRepository.uploadMedia(_uiState.value.selectedMedia) { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }

            val mediaType = if (_uiState.value.selectedMedia.any { it.type == MediaType.VIDEO }) {
                MediaType.VIDEO
            } else {
                MediaType.IMAGE
            }

            val newPost = Post(
                id = "post_" + System.currentTimeMillis().toString(),
                authorId = "user_current",
                audienceType = AudienceType.USER,
                audienceId = recipient.id,
                createdAt = System.currentTimeMillis(),
                mediaType = mediaType
            )

            _uiState.value = _uiState.value.copy(isUploading = false, uploadProgress = 0f)
            onSuccess(newPost)
        }
    }
}
