package com.tangai.memento.feature.post.domain

import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.Post

interface PostRepository {
    suspend fun preparePhotoPost(
        connectionId: String,
        media: LocalMediaItem,
        caption: String?
    ): Result<PendingPhotoPost>

    suspend fun uploadPendingPhoto(
        pending: PendingPhotoPost,
        onProgress: (Float) -> Unit
    ): Result<Post>

    suspend fun getLatestPendingPhoto(): Result<PendingPhotoPost?>
}
