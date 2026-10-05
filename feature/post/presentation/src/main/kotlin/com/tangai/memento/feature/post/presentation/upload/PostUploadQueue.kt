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
    SCHEDULED,
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
    private val postRepository: PostRepository,
    private val uploadScheduler: PostUploadScheduler
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
    private var restoreJob: Job? = null

    val items: StateFlow<List<PostUploadItem>> = _items.asStateFlow()

    init {
        scope.launch {
            uploadScheduler.updates.collect { updates ->
                updates.forEach { (postId, scheduledUpdate) ->
                    when (scheduledUpdate.status) {
                        ScheduledUploadStatus.ENQUEUED -> update(postId) { current ->
                            current.copy(item = current.item.copy(
                                status = PostUploadStatus.SCHEDULED,
                                progress = scheduledUpdate.progress,
                                errorMessage = null
                            ))
                        }
                        ScheduledUploadStatus.RUNNING -> update(postId) { current ->
                            current.copy(item = current.item.copy(
                                status = PostUploadStatus.UPLOADING,
                                progress = scheduledUpdate.progress,
                                errorMessage = null
                            ))
                        }
                        ScheduledUploadStatus.SUCCEEDED -> {
                            entries.removeAll { it.item.id == postId }
                            publish()
                        }
                        ScheduledUploadStatus.FAILED -> update(postId) { current ->
                            current.copy(item = current.item.copy(
                                status = PostUploadStatus.FAILED,
                                progress = scheduledUpdate.progress,
                                errorMessage = scheduledUpdate.errorMessage
                                    ?: "Couldn’t post. Tap Retry to try again."
                            ))
                        }
                    }
                }
            }
        }
    }

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
                                    status = PostUploadStatus.QUEUED
                                ),
                                connectionId = pending.post.connectionId,
                                pending = pending
                            )
                        }
                    }
                    entries.addAll(0, restored)
                    publish()
                    scheduleReady()
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
            scheduleReady()
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
                scheduleReady()
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
                scheduleReady()
            }.onFailure { error ->
                update(id) { current ->
                    current.copy(item = current.item.copy(
                        status = PostUploadStatus.FAILED,
                        progress = 0f,
                        errorMessage = error.message ?: "We couldn’t prepare this moment. Tap Retry."
                    ))
                }
                scheduleReady()
            }
        }
    }

    private fun scheduleReady() {
        entries.forEach { entry ->
            when (entry.item.status) {
                PostUploadStatus.PREPARING -> return
                PostUploadStatus.QUEUED -> {
                    val pending = entry.pending ?: return
                    uploadScheduler.enqueue(pending.post.connectionId, pending.post.id)
                    update(entry.item.id) { current ->
                        current.copy(item = current.item.copy(
                            status = PostUploadStatus.SCHEDULED,
                            progress = 0f,
                            errorMessage = null
                        ))
                    }
                }
                PostUploadStatus.SCHEDULED,
                PostUploadStatus.UPLOADING,
                PostUploadStatus.FAILED -> Unit
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
