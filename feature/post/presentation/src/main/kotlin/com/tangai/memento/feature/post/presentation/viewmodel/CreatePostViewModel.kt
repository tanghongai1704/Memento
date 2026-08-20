package com.tangai.memento.feature.post.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.AudienceType
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.User
import com.tangai.memento.feature.post.domain.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onRecipientSelected(user: User) {
        _uiState.value = _uiState.value.copy(selectedRecipient = user)
    }

    fun setDefaultRecipient(user: User) {
        _uiState.value = _uiState.value.copy(selectedRecipient = user)
    }

    fun simulatePostCreation(onSuccess: (Post) -> Unit) {
        val recipient = _uiState.value.selectedRecipient
        if (recipient == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please select a recipient"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, errorMessage = null)

            postRepository.simulateUpload { progress ->
                _uiState.value = _uiState.value.copy(uploadProgress = progress)
            }

            val newPost = Post(
                id = "post_" + System.currentTimeMillis().toString(),
                authorId = "user_current",
                audienceType = AudienceType.USER,
                audienceId = recipient.id,
                createdAt = System.currentTimeMillis(),
                mediaType = MediaType.IMAGE
            )

            _uiState.value = _uiState.value.copy(isUploading = false, uploadProgress = 0f)
            onSuccess(newPost)
        }
    }
}
