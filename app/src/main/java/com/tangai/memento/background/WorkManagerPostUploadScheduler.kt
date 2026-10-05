package com.tangai.memento.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.tangai.memento.feature.post.presentation.upload.PostUploadScheduler
import com.tangai.memento.feature.post.presentation.upload.ScheduledUploadStatus
import com.tangai.memento.feature.post.presentation.upload.ScheduledUploadUpdate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class WorkManagerPostUploadScheduler @Inject constructor(
    @ApplicationContext context: Context
) : PostUploadScheduler {
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val latestWorkIds = mutableMapOf<String, UUID>()
    private val _updates = MutableStateFlow<Map<String, ScheduledUploadUpdate>>(emptyMap())
    private val requests = Channel<UploadRequest>(Channel.UNLIMITED)

    override val updates: StateFlow<Map<String, ScheduledUploadUpdate>> = _updates.asStateFlow()

    init {
        scope.launch {
            for (request in requests) schedule(request)
        }
    }

    override fun enqueue(connectionId: String, postId: String) {
        update(postId, ScheduledUploadUpdate(ScheduledUploadStatus.ENQUEUED))
        requests.trySend(UploadRequest(connectionId, postId))
    }

    private suspend fun schedule(request: UploadRequest) {
        val tag = PostUploadWorkKeys.TAG_PREFIX + request.postId
        val existing = workManager.getWorkInfosByTagFlow(tag).first()
            .filter { info ->
                info.state == WorkInfo.State.RUNNING ||
                    info.state == WorkInfo.State.ENQUEUED ||
                    info.state == WorkInfo.State.BLOCKED
            }
            .maxByOrNull { info ->
                when (info.state) {
                    WorkInfo.State.RUNNING -> 2
                    WorkInfo.State.ENQUEUED -> 1
                    else -> 0
                }
            }
        val workId = if (existing != null) {
            existing.id
        } else {
            val workRequest = OneTimeWorkRequestBuilder<PostUploadWorker>()
                .setInputData(workDataOf(
                    PostUploadWorkKeys.CONNECTION_ID to request.connectionId,
                    PostUploadWorkKeys.POST_ID to request.postId
                ))
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    MIN_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .addTag(tag)
                .build()
            workManager.enqueueUniqueWork(
                PostUploadWorkKeys.QUEUE_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
            workRequest.id
        }
        latestWorkIds[request.postId] = workId
        observe(request.postId, workId)
    }

    private fun observe(postId: String, workId: UUID) {
        scope.launch {
            workManager.getWorkInfoByIdFlow(workId)
                .filterNotNull()
                .collect { info ->
                    if (latestWorkIds[postId] != workId) return@collect
                    val progress = info.progress
                        .getFloat(PostUploadWorkKeys.PROGRESS, 0f)
                        .coerceIn(0f, 1f)
                    val next = when (info.state) {
                        WorkInfo.State.RUNNING -> ScheduledUploadUpdate(
                            ScheduledUploadStatus.RUNNING,
                            progress
                        )
                        WorkInfo.State.SUCCEEDED -> {
                            if (info.outputData.getString(PostUploadWorkKeys.OUTCOME) ==
                                PostUploadWorkKeys.OUTCOME_FAILED
                            ) {
                                ScheduledUploadUpdate(
                                    ScheduledUploadStatus.FAILED,
                                    progress,
                                    info.outputData.getString(PostUploadWorkKeys.ERROR)
                                )
                            } else {
                                ScheduledUploadUpdate(ScheduledUploadStatus.SUCCEEDED, 1f)
                            }
                        }
                        WorkInfo.State.FAILED,
                        WorkInfo.State.CANCELLED -> ScheduledUploadUpdate(
                            ScheduledUploadStatus.FAILED,
                            progress,
                            "The system upload stopped. Tap Retry to try again."
                        )
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.BLOCKED -> ScheduledUploadUpdate(
                            ScheduledUploadStatus.ENQUEUED,
                            progress
                        )
                    }
                    update(postId, next)
                }
        }
    }

    private fun update(postId: String, update: ScheduledUploadUpdate) {
        _updates.value = _updates.value + (postId to update)
    }

    private companion object {
        const val MIN_BACKOFF_SECONDS = 15L
    }

    private data class UploadRequest(val connectionId: String, val postId: String)
}
