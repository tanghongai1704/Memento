package com.tangai.memento.feature.post.domain

import com.tangai.memento.domain.model.Post

interface PostRepository {
    suspend fun createPost(post: Post): Result<Post>
    suspend fun simulateUpload(onProgress: (Float) -> Unit): Result<Unit>
}
