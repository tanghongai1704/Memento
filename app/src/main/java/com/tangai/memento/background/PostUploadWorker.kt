package com.tangai.memento.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tangai.memento.feature.post.domain.PostRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PostUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val postRepository: PostRepository
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val connectionId = inputData.getString(PostUploadWorkKeys.CONNECTION_ID)
            ?: return terminalFailure("Missing connection ID.")
        val postId = inputData.getString(PostUploadWorkKeys.POST_ID)
            ?: return terminalFailure("Missing post ID.")

        val pendingResult = postRepository.getPendingPhoto(connectionId, postId)
        val pending = pendingResult.getOrElse { error ->
            return retryOrFinish(error.message ?: "Couldn’t restore this moment.")
        } ?: return Result.success(workDataOf(
            PostUploadWorkKeys.OUTCOME to PostUploadWorkKeys.OUTCOME_SUCCEEDED
        ))

        val uploadResult = postRepository.uploadPendingPhoto(pending) { progress ->
            setProgressAsync(workDataOf(
                PostUploadWorkKeys.PROGRESS to progress.coerceIn(0f, 1f)
            ))
        }
        return uploadResult.fold(
            onSuccess = {
                Result.success(workDataOf(
                    PostUploadWorkKeys.OUTCOME to PostUploadWorkKeys.OUTCOME_SUCCEEDED
                ))
            },
            onFailure = { error ->
                retryOrFinish(error.message ?: "Couldn’t upload this moment.")
            }
        )
    }

    private fun retryOrFinish(message: String): Result =
        if (runAttemptCount < MAX_AUTOMATIC_RETRIES) {
            Result.retry()
        } else {
            terminalFailure(message)
        }

    private fun terminalFailure(message: String): Result = Result.success(workDataOf(
        PostUploadWorkKeys.OUTCOME to PostUploadWorkKeys.OUTCOME_FAILED,
        PostUploadWorkKeys.ERROR to message.take(MAX_ERROR_LENGTH)
    ))

    private companion object {
        const val MAX_AUTOMATIC_RETRIES = 3
        const val MAX_ERROR_LENGTH = 500
    }
}
