package com.tangai.memento.feature.post.domain

import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.Post

interface PostRepository {
    suspend fun uploadMedia(mediaItems: List<LocalMediaItem>, onProgress: (Float) -> Unit): Result<Unit>
}
