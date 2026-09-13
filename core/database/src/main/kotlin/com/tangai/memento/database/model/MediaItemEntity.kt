package com.tangai.memento.database.model

import androidx.room.*
import com.tangai.memento.domain.model.*

@Entity(tableName = "media_items", primaryKeys = ["connectionId", "postId", "mediaId"],
    indices = [Index("postId"), Index(value = ["connectionId", "postId"])],
    foreignKeys = [ForeignKey(entity = PostEntity::class,
        parentColumns = ["connectionId", "id"], childColumns = ["connectionId", "postId"], onDelete = ForeignKey.CASCADE)])
data class MediaItemEntity(
    val connectionId: String, val postId: String, val mediaId: String,
    val mediaType: String, val storagePath: String, val thumbnailPath: String?,
    val mimeType: String, val width: Int, val height: Int, val durationMs: Long?,
    val sizeBytes: Long, val position: Int
)
fun MediaItemEntity.toDomain() = MediaItem(mediaId, MediaType.valueOf(mediaType), storagePath,
    thumbnailPath, mimeType, width, height, durationMs, sizeBytes, position)
