package com.tangai.memento.feature.post.data

import com.tangai.memento.domain.model.MediaItem
import com.tangai.memento.domain.model.Post
import com.tangai.memento.feature.post.domain.PostRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import javax.inject.Inject

class FakePostRepository @Inject constructor() : PostRepository {
    override suspend fun createPost(post: Post): Result<Post> {
        delay(100.milliseconds)
        return Result.success(post)
    }

    override suspend fun simulateUpload(onProgress: (Float) -> Unit): Result<Unit> {
        for (i in 0..100 step 20) {
            onProgress(i / 100f)
            delay(100.milliseconds)
        }
        delay(500.milliseconds)
        return Result.success(Unit)
    }

    override suspend fun uploadMedia(mediaItems: List<MediaItem>, onProgress: (Float) -> Unit): Result<Unit> {
        val totalSteps = maxOf(mediaItems.size, 1)
        mediaItems.forEachIndexed { index, media ->
            val baseProgress = index.toFloat() / totalSteps.toFloat()
            for (progress in 0..100 step 20) {
                onProgress((baseProgress + (progress / 100f) / totalSteps.toFloat()).coerceAtMost(1f))
                delay(50.milliseconds)
            }
            delay(150.milliseconds)
        }
        onProgress(1f)
        return Result.success(Unit)
    }
}
