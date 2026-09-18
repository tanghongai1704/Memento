package com.tangai.memento.feature.post.domain

import com.tangai.memento.domain.model.Post

data class PendingPhotoPost(
    val post: Post,
    val localUris: List<String>
)
