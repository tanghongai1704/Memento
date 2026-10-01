package com.tangai.memento.feature.post.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.Connection
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.User
import com.tangai.memento.domain.model.displayLabel
import com.tangai.memento.feature.post.domain.PendingPhotoPost
import com.tangai.memento.feature.post.domain.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val connectionRepository: com.tangai.memento.feature.connection.domain.ConnectionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    private var loadRecipientsJob: Job? = null
    private val initialRecipientId: String? = savedStateHandle[INITIAL_RECIPIENT_ID]

    init {
        loadCreatePostData()
        viewModelScope.launch {
            connectionRepository.observeConnections().collect { result ->
                result.onSuccess {
                    val recipients = connectionRepository.loadCachedConnections()
                        .getOrDefault(_uiState.value.recipients)
                    val users = connectionRepository.loadCachedConnectionUsers()
                        .getOrNull()?.users.orEmpty()
                    applyRecipients(
                        recipients = recipients,
                        connectedUsers = users,
                        pending = _uiState.value.pendingPhoto,
                        restoreDraft = false,
                        isLoading = false
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = if (_uiState.value.recipients.isEmpty()) {
                            "We couldn’t load your connections. Check your connection and try again."
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun retryLoadingConnections() {
        loadCreatePostData()
        viewModelScope.launch {
            connectionRepository.loadConnections()
                .onFailure {
                    if (_uiState.value.recipients.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "We couldn’t load your connections. Check your connection and try again."
                        )
                    }
                }
        }
    }

    private fun loadCreatePostData() {
        loadRecipientsJob?.cancel()
        loadRecipientsJob = viewModelScope.launch {
            val hasExistingContent = _uiState.value.recipients.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasExistingContent,
                errorMessage = null
            )
            val pending = postRepository.getLatestPendingPhoto().getOrNull()
            val cachedRecipients = connectionRepository.loadCachedConnections().getOrDefault(emptyList())
            val cachedUsers = connectionRepository.loadCachedConnectionUsers()
                .getOrNull()?.users.orEmpty()
            applyRecipients(
                recipients = cachedRecipients,
                connectedUsers = cachedUsers,
                pending = pending,
                restoreDraft = true,
                isLoading = false
            )
        }
    }

    private fun applyRecipients(
        recipients: List<Connection>,
        connectedUsers: List<User>,
        pending: PendingPhotoPost?,
        restoreDraft: Boolean,
        isLoading: Boolean
    ) {
        val current = _uiState.value
        val connectedUsersById = connectedUsers.associateBy(User::id)
        val selectedRecipient = pending?.post?.connectionId
            ?.let { id -> recipients.find { it.id == id } }
            ?: current.selectedRecipient?.id
                ?.let { id -> recipients.find { it.id == id } }
            ?: initialRecipientId?.let { id -> recipients.find { it.id == id } }
            ?: recipients.firstOrNull()
        _uiState.value = current.copy(
            recipients = recipients,
            recipientLabels = recipients.associate { connection ->
                connection.id to connection.displayLabel(connectedUsersById)
            },
            recipientUsers = connectionUsers(recipients, connectedUsersById),
            selectedRecipient = selectedRecipient,
            selectedMedia = if (restoreDraft) {
                pending?.localUris?.mapIndexed { index, uri ->
                    LocalMediaItem(
                        uri = uri,
                        type = MediaType.IMAGE,
                        displayName = "Pending photo ${index + 1}",
                        processedSizeBytes = pending.post.mediaItems.getOrNull(index)?.sizeBytes ?: 0L
                    )
                }.orEmpty()
            } else {
                current.selectedMedia
            },
            selectedLayout = if (restoreDraft) {
                pending?.post?.layoutType ?: LayoutType.SINGLE
            } else {
                current.selectedLayout
            },
            caption = if (restoreDraft) pending?.post?.caption.orEmpty() else current.caption,
            pendingPhoto = if (restoreDraft) pending else current.pendingPhoto,
            isLoading = isLoading,
            errorMessage = null
        )
    }

    fun onRecipientSelected(user: Connection) {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = user,
            errorMessage = null
        )
    }

    fun setSelectedMedia(media: List<LocalMediaItem>) {
        val selected = media.distinctBy(LocalMediaItem::uri).take(MAX_PHOTOS_PER_POST)
        val selectedLayout = when {
            selected.size <= 1 -> LayoutType.SINGLE
            _uiState.value.selectedLayout == LayoutType.SINGLE -> LayoutType.GRID
            _uiState.value.selectedLayout == LayoutType.COLLAGE && selected.size >= MAX_PHOTOS_PER_POST -> {
                LayoutType.GRID
            }
            else -> _uiState.value.selectedLayout
        }
        _uiState.value = _uiState.value.copy(
            selectedMedia = selected,
            selectedLayout = selectedLayout,
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
                        selectedLayout = LayoutType.SINGLE,
                        caption = "",
                        pendingPhoto = null,
                        uploadProgress = 0f,
                        errorMessage = null,
                        successMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "We couldn’t start over. Please try again."
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
                processingMessage = "Getting your moment ready…",
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
                    errorMessage = "We couldn’t prepare your photos. Please try again."
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
                successMessage = null,
                errorMessage = if (result.isFailure) {
                    "Couldn’t post. Check your connection and try again."
                } else {
                    null
                }
            )
            result.getOrNull()?.let(onSuccess)
        }
    }

    private companion object {
        const val MAX_PHOTOS_PER_POST = 5
        const val INITIAL_RECIPIENT_ID = "recipientId"
    }

    private fun connectionUsers(
        connections: List<Connection>,
        usersById: Map<String, User>
    ): Map<String, User> = connections.mapNotNull { connection ->
        connection.members
            .asSequence()
            .mapNotNull { member -> usersById[member.userId] }
            .firstOrNull()
            ?.let { user -> connection.id to user }
    }.toMap()

}
