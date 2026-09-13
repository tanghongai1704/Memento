package com.tangai.memento.domain.model

data class Post(
    val id: String,
    val connectionId: String,
    val authorId: String,
    val postType: PostType,
    val layoutType: LayoutType = LayoutType.SINGLE,
    val caption: String? = null,
    val mediaItems: List<MediaItem> = emptyList(),
    val clientCreatedAt: Long,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val status: PostStatus = PostStatus.ACTIVE,
    val deletedAt: Long? = null,
    val deletedBy: String? = null,
    val schemaVersion: Int = 1
)
enum class PostType { PHOTO, VIDEO }
enum class LayoutType { SINGLE, GRID, COLLAGE, CAROUSEL }
enum class PostStatus { ACTIVE, DELETED }
enum class LocalSyncStatus { PENDING, SYNCED, FAILED }
