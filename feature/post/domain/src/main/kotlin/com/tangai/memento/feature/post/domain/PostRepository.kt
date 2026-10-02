package com.tangai.memento.feature.post.domain

import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.LayoutType

interface PostRepository {
    suspend fun preparePhotoPost(
        connectionId: String,
        media: List<LocalMediaItem>,
        layoutType: LayoutType,
        caption: String?,
        onProgress: (Float) -> Unit
    ): Result<PendingPhotoPost>

    suspend fun uploadPendingPhoto(
        pending: PendingPhotoPost,
        onProgress: (Float) -> Unit
    ): Result<Post>

    suspend fun getPendingPhotos(): Result<List<PendingPhotoPost>>

    suspend fun discardPendingPhoto(pending: PendingPhotoPost): Result<Unit>
}
