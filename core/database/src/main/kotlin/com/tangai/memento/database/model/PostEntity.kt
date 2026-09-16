package com.tangai.memento.database.model

import androidx.room.*
import com.tangai.memento.domain.model.*

@Entity(tableName = "posts", primaryKeys = ["connectionId", "id"], indices = [
    Index("connectionId"), Index("authorId"), Index("createdAt"),
    Index(value = ["status", "createdAt"]), Index(value = ["connectionId", "status", "createdAt"])
])
data class PostEntity(
    val id: String, val connectionId: String, val authorId: String,
    val postType: PostType, val layoutType: LayoutType, val caption: String?,
    val clientCreatedAt: Long, val createdAt: Long?, val updatedAt: Long?,
    val status: PostStatus, val deletedAt: Long?, val deletedBy: String?,
    val schemaVersion: Int = 1, val localSyncStatus: LocalSyncStatus = LocalSyncStatus.PENDING
)
fun PostEntity.toDomain(media: List<MediaItem> = emptyList()) = Post(
    id, connectionId, authorId, postType, layoutType,
    caption, media, clientCreatedAt, createdAt, updatedAt, status,
    deletedAt, deletedBy, schemaVersion)
