package com.tangai.memento.data.model

data class Post(
    val id: String,
    val authorId: String,
    val audienceType: AudienceType,
    val audienceId: String,
    val createdAt: Long,
    val mediaType: MediaType
)
