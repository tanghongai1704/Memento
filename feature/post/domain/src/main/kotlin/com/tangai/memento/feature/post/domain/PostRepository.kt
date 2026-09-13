package com.tangai.memento.feature.post.domain

import com.tangai.memento.domain.model.MediaItem
import com.tangai.memento.domain.model.Post

interface PostRepository {
    suspend fun createPost(post: Post): Result<Post>
    suspend fun simulateUpload(onProgress: (Float) -> Unit): Result<Unit>
    suspend fun uploadMedia(mediaItems: List<MediaItem>, onProgress: (Float) -> Unit): Result<Unit>
}
