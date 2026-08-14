package com.tangai.memento.feature.post.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangai.memento.data.model.User
import com.tangai.memento.data.model.Post
import com.tangai.memento.data.model.AudienceType
import com.tangai.memento.data.model.MediaType
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onRecipientSelected(user: User) {
        _uiState.value = _uiState.value.copy(selectedRecipient = user)
    }

    fun onRecipientChanged(user: User) {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = user,
            selectedMedia = emptyList()
        )
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

            for (i in 0..100 step 20) {
                _uiState.value = _uiState.value.copy(uploadProgress = i / 100f)
                delay(100.milliseconds)
            }

            delay(500.milliseconds)

            // Create a fake Post object
            val newPost = Post(
                id = "post_"+System.currentTimeMillis().toString(),
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
