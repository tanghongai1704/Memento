package com.tangai.memento.feature.post.data

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
}
