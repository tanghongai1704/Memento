package com.tangai.memento.database.dao

import androidx.room.*
import com.tangai.memento.database.model.*
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.LocalSyncStatus

@Dao
abstract class PostDao {
    // Membership guard also prevents a second signed-in account reading the first account's cache.
    @Query("""SELECT p.* FROM posts p JOIN connections c ON c.id = p.connectionId
        JOIN connection_members m ON m.connectionId = c.id AND m.userId = :currentUserId
        WHERE p.status = 'ACTIVE' AND c.status = 'ACTIVE' AND m.status = 'ACTIVE'
        AND (:connectionId IS NULL OR p.connectionId = :connectionId)
        AND (:authorFilter = 'ALL' OR (:authorFilter = 'MY' AND p.authorId = :currentUserId)
             OR (:authorFilter = 'RECEIVED' AND p.authorId != :currentUserId))
        AND (:postType IS NULL OR p.postType = :postType)
        AND (:fromTime IS NULL OR p.createdAt >= :fromTime)
        AND (:toTime IS NULL OR p.createdAt <= :toTime)
        ORDER BY COALESCE(p.createdAt, p.clientCreatedAt) DESC, p.id DESC""")
    abstract suspend fun getPosts(currentUserId: String, connectionId: String? = null,
        authorFilter: String = "ALL", postType: String? = null,
        fromTime: Long? = null, toTime: Long? = null): List<PostEntity>

    @Query("SELECT * FROM media_items WHERE connectionId = :connectionId AND postId = :postId ORDER BY position")
    abstract suspend fun getMedia(connectionId: String, postId: String): List<MediaItemEntity>

    @Upsert abstract suspend fun upsertPost(post: PostEntity)
    @Upsert abstract suspend fun upsertMedia(media: List<MediaItemEntity>)

    @Query("SELECT * FROM posts WHERE connectionId = :connectionId AND id = :postId LIMIT 1")
    abstract suspend fun getPost(connectionId: String, postId: String): PostEntity?

    @Query("""SELECT * FROM posts WHERE authorId = :authorId
        AND localSyncStatus IN ('PENDING', 'FAILED')
        ORDER BY clientCreatedAt DESC""")
    abstract suspend fun getRetryablePosts(authorId: String): List<PostEntity>

    @Query("""UPDATE posts SET localSyncStatus = :syncStatus,
        createdAt = :createdAt, updatedAt = :updatedAt
        WHERE connectionId = :connectionId AND id = :postId""")
    abstract suspend fun updateSyncState(
        connectionId: String,
        postId: String,
        syncStatus: LocalSyncStatus,
        createdAt: Long? = null,
        updatedAt: Long? = null
    )

    @Transaction
    open suspend fun loadPosts(uid: String): List<Post> = getPosts(uid).map {
        it.toDomain(getMedia(it.connectionId, it.id).map { media -> media.toDomain() })
    }
}
