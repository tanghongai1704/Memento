package com.tangai.memento.feature.post.presentation.upload

import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.feature.post.domain.PendingPhotoPost
import com.tangai.memento.feature.post.domain.PostRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PostUploadStatus {
    PREPARING,
    QUEUED,
    UPLOADING,
    FAILED
}

data class PostUploadItem(
    val id: String,
    val caption: String?,
    val previewUri: String?,
    val status: PostUploadStatus,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

@Singleton
class PostUploadQueue @Inject constructor(
    private val postRepository: PostRepository
) {
    private data class QueueEntry(
        val item: PostUploadItem,
        val connectionId: String,
        val media: List<LocalMediaItem> = emptyList(),
        val layoutType: LayoutType = LayoutType.SINGLE,
        val pending: PendingPhotoPost? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val entries = mutableListOf<QueueEntry>()
    private val _items = MutableStateFlow<List<PostUploadItem>>(emptyList())
    private var started = false
    private var draining = false
    private var restoreJob: Job? = null

    val items: StateFlow<List<PostUploadItem>> = _items.asStateFlow()

    fun start() {
        if (started) return
        started = true
        restoreJob = scope.launch {
            postRepository.getPendingPhotos()
                .onSuccess { pendingPosts ->
                    val restored = pendingPosts.mapNotNull { pending ->
                        if (entries.any { it.item.id == pending.post.id }) {
                            null
                        } else {
                            QueueEntry(
                                item = PostUploadItem(
                                    id = pending.post.id,
                                    caption = pending.post.caption,
                                    previewUri = pending.localUris.firstOrNull(),
                                    status = PostUploadStatus.FAILED,
                                    errorMessage = "This moment is saved on this device. Tap Retry to post it."
                                ),
                                connectionId = pending.post.connectionId,
                                pending = pending
                            )
                        }
                    }
                    entries.addAll(0, restored)
                    publish()
                }
        }
    }

    fun enqueue(
        connectionId: String,
        media: List<LocalMediaItem>,
        layoutType: LayoutType,
        caption: String?
    ) {
        start()
        val queueId = UUID.randomUUID().toString()
        entries += QueueEntry(
            item = PostUploadItem(
                id = queueId,
                caption = caption?.trim()?.takeIf(String::isNotEmpty),
                previewUri = media.firstOrNull()?.uri,
                status = PostUploadStatus.PREPARING
            ),
            connectionId = connectionId,
            media = media,
            layoutType = layoutType
        )
        publish()
        prepare(queueId)
    }

    fun retry(id: String) {
        val entry = entries.firstOrNull { it.item.id == id } ?: return
        if (entry.item.status != PostUploadStatus.FAILED) return
        if (entry.pending == null) {
            update(id) { current ->
                current.copy(item = current.item.copy(
                    status = PostUploadStatus.PREPARING,
                    progress = 0f,
                    errorMessage = null
                ))
            }
            prepare(id)
        } else {
            update(id) { current ->
                current.copy(item = current.item.copy(
                    status = PostUploadStatus.QUEUED,
                    progress = 0f,
                    errorMessage = null
                ))
            }
            drain()
        }
    }

    fun retryAll() {
        entries.filter { it.item.status == PostUploadStatus.FAILED }
            .map { it.item.id }
            .forEach(::retry)
    }

    fun discard(id: String) {
        val entry = entries.firstOrNull { it.item.id == id } ?: return
        if (entry.item.status != PostUploadStatus.FAILED) return
        scope.launch {
            val removed = entry.pending?.let { postRepository.discardPendingPhoto(it).isSuccess } ?: true
            if (removed) {
                entries.removeAll { it.item.id == id }
                publish()
                drain()
            }
        }
    }

    private fun prepare(id: String) {
        scope.launch {
            restoreJob?.join()
            val entry = entries.firstOrNull { it.item.id == id } ?: return@launch
            val result = postRepository.preparePhotoPost(
                connectionId = entry.connectionId,
                media = entry.media,
                layoutType = entry.layoutType,
                caption = entry.item.caption
            ) { progress ->
                scope.launch {
                    update(id) { current -> current.copy(item = current.item.copy(progress = progress)) }
                }
            }
            result.onSuccess { pending ->
                update(id) { current ->
                    current.copy(
                        item = current.item.copy(
                            id = pending.post.id,
                            previewUri = pending.localUris.firstOrNull(),
                            status = PostUploadStatus.QUEUED,
                            progress = 0f,
                            errorMessage = null
                        ),
                        pending = pending
                    )
                }
                drain()
            }.onFailure { error ->
                update(id) { current ->
                    current.copy(item = current.item.copy(
                        status = PostUploadStatus.FAILED,
                        progress = 0f,
                        errorMessage = error.message ?: "We couldn’t prepare this moment. Tap Retry."
                    ))
                }
            }
        }
    }

    private fun drain() {
        if (draining) return
        draining = true
        scope.launch {
            try {
                while (true) {
                    val next = entries.firstOrNull {
                        it.item.status == PostUploadStatus.PREPARING ||
                            it.item.status == PostUploadStatus.QUEUED ||
                            it.item.status == PostUploadStatus.UPLOADING
                    }
                    if (next == null || next.item.status == PostUploadStatus.PREPARING) break
                    if (next.item.status != PostUploadStatus.QUEUED || next.pending == null) break

                    val id = next.item.id
                    update(id) { current ->
                        current.copy(item = current.item.copy(
                            status = PostUploadStatus.UPLOADING,
                            progress = 0f
                        ))
                    }
                    val result = postRepository.uploadPendingPhoto(next.pending) { progress ->
                        scope.launch {
                            update(id) { current -> current.copy(item = current.item.copy(progress = progress)) }
                        }
                    }
                    if (result.isSuccess) {
                        entries.removeAll { it.item.id == id }
                        publish()
                    } else {
                        update(id) { current ->
                            current.copy(item = current.item.copy(
                                status = PostUploadStatus.FAILED,
                                errorMessage = result.exceptionOrNull()?.message
                                    ?: "Couldn’t post. Check your connection and tap Retry."
                            ))
                        }
                        break
                    }
                }
            } finally {
                draining = false
            }
        }
    }

    private fun update(id: String, transform: (QueueEntry) -> QueueEntry) {
        val index = entries.indexOfFirst { it.item.id == id }
        if (index < 0) return
        entries[index] = transform(entries[index])
        publish()
    }

    private fun publish() {
        _items.value = entries.map(QueueEntry::item)
    }
}
