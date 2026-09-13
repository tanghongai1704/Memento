package com.tangai.memento.feature.post.data

import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.feature.post.domain.PostRepository
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor() : PostRepository {
    override suspend fun uploadMedia(mediaItems: List<LocalMediaItem>, onProgress: (Float) -> Unit): Result<Unit> =
        Result.failure(UnsupportedOperationException("Posting is not available yet."))
}
