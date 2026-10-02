package com.tangai.memento.feature.post.presentation.upload

import kotlinx.coroutines.flow.StateFlow

enum class ScheduledUploadStatus {
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}

data class ScheduledUploadUpdate(
    val status: ScheduledUploadStatus,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

interface PostUploadScheduler {
    val updates: StateFlow<Map<String, ScheduledUploadUpdate>>

    fun enqueue(connectionId: String, postId: String)
}
