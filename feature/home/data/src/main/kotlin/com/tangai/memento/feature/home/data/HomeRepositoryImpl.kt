package com.tangai.memento.feature.home.data

import android.content.Context
import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.FirebaseStorage
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.MediaItemEntity
import com.tangai.memento.database.model.PostEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.domain.model.*
import com.tangai.memento.feature.home.domain.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class HomeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MementoDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val storage: FirebaseStorage,
    private val connections: com.tangai.memento.feature.connection.domain.ConnectionRepository) : HomeRepository {
    private val postSyncMutex = Mutex()
    private val paginationLock = Any()
    private var paginationUid: String? = null
    private val activeConnectionIds = mutableSetOf<String>()
    private val pageCursors = mutableMapOf<String, DocumentSnapshot>()
    private val pageHasMore = mutableMapOf<String, Boolean>()
    private val connectionsWithOlderPages = mutableSetOf<String>()

    override suspend fun loadPosts(): Result<List<Post>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        database.postDao().loadPosts(uid).also { check(auth.currentUser?.uid == uid) }
    }

    override fun observePosts(): Flow<Result<PostFeedPage>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close(IllegalStateException("User is not signed in."))
            return@callbackFlow
        }

        val postListeners = mutableMapOf<String, List<ListenerRegistration>>()
        synchronized(paginationLock) { resetPagination(uid) }

        fun syncSnapshot(
            connectionId: String,
            documents: List<DocumentSnapshot>,
            updatePagination: Boolean
        ) = launch {
            try {
                check(auth.currentUser?.uid == uid) { "Account changed during post sync." }
                if (updatePagination) synchronized(paginationLock) {
                    if (paginationUid == uid && connectionId in activeConnectionIds &&
                        connectionId !in connectionsWithOlderPages) {
                        documents.lastOrNull()?.let { pageCursors[connectionId] = it }
                            ?: pageCursors.remove(connectionId)
                        pageHasMore[connectionId] = documents.size.toLong() == POST_PAGE_SIZE
                    }
                }
                val cacheError = postSyncMutex.withLock {
                    syncDocuments(connectionId, documents)
                }
                trySend(Result.success(currentPage(uid)))
                cacheError?.let { trySend(Result.failure(it)) }
            } catch (error: Throwable) {
                trySend(Result.failure(error))
            }
        }

        fun listenToPosts(connectionId: String): List<ListenerRegistration> {
            val feedListener = postsQuery(connectionId)
                .limit(POST_PAGE_SIZE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) syncSnapshot(connectionId, snapshot.documents, true)
                }
            val deletionListener = firestore.collection("connections")
                .document(connectionId)
                .collection("posts")
                .whereEqualTo("status", PostStatus.DELETED.name)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(DELETED_POST_PAGE_SIZE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    if (snapshot != null) syncSnapshot(connectionId, snapshot.documents, false)
                }
            return listOf(feedListener, deletionListener)
        }

        val connectionListener = firestore.collection("connections")
            .whereArrayContains("memberIds", uid)
            .whereEqualTo("status", ConnectionStatus.ACTIVE.name)
            .orderBy("lastPostAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                val activeIds = snapshot.documents.map(DocumentSnapshot::getId).toSet()
                synchronized(paginationLock) {
                    if (paginationUid != uid) resetPagination(uid)
                    activeConnectionIds.retainAll(activeIds)
                    activeConnectionIds.addAll(activeIds)
                    pageCursors.keys.retainAll(activeIds)
                    pageHasMore.keys.retainAll(activeIds)
                    connectionsWithOlderPages.retainAll(activeIds)
                }
                (postListeners.keys - activeIds).forEach { connectionId ->
                    postListeners.remove(connectionId)?.forEach(ListenerRegistration::remove)
                }
                (activeIds - postListeners.keys).forEach { connectionId ->
                    postListeners[connectionId] = listenToPosts(connectionId)
                }
                if (activeIds.isEmpty()) {
                    launch {
                        runCatching { currentPage(uid) }
                            .also { trySend(it) }
                    }
                }
            }

        awaitClose {
            connectionListener.remove()
            postListeners.values.flatten().forEach(ListenerRegistration::remove)
            postListeners.clear()
            synchronized(paginationLock) {
                if (paginationUid == uid) resetPagination(null)
            }
        }
    }

    override suspend fun loadOlderPosts(connectionId: String?): Result<PostFeedPage> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        val targets = synchronized(paginationLock) {
            if (paginationUid != uid) resetPagination(uid)
            activeConnectionIds
                .filter { connectionId == null || it == connectionId }
                .mapNotNull { targetConnectionId ->
                    val cursor = pageCursors[targetConnectionId]
                    if (pageHasMore[targetConnectionId] == true && cursor != null) {
                        PageTarget(targetConnectionId, cursor)
                    } else {
                        null
                    }
                }
        }

        targets.forEach { target ->
            check(auth.currentUser?.uid == uid) { "Account changed while loading older posts." }
            val documents = postsQuery(target.connectionId)
                .startAfter(target.cursor)
                .limit(POST_PAGE_SIZE)
                .get()
                .awaitTask()
                .documents

            val cacheError = postSyncMutex.withLock {
                syncDocuments(target.connectionId, documents)
            }
            cacheError?.let { throw it }

            synchronized(paginationLock) {
                if (paginationUid == uid && target.connectionId in activeConnectionIds) {
                    connectionsWithOlderPages += target.connectionId
                    documents.lastOrNull()?.let { pageCursors[target.connectionId] = it }
                    pageHasMore[target.connectionId] = documents.size.toLong() == POST_PAGE_SIZE
                }
            }
        }

        check(auth.currentUser?.uid == uid) { "Account changed while loading older posts." }
        currentPage(uid)
    }

    override suspend fun deletePost(post: Post): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        check(post.authorId == uid) { "Only the author can delete this post." }
        val response = functions.getHttpsCallable("softDeletePost")
            .call(mapOf("connectionId" to post.connectionId, "postId" to post.id))
            .awaitTask().data.asMap()
        val deletedAt = response.requiredLong("deletedAtMillis")
        val updatedAt = response.requiredLong("updatedAtMillis")
        check(auth.currentUser?.uid == uid) { "Account changed while deleting the post." }
        database.postDao().markDeleted(post.connectionId, post.id, uid, deletedAt, updatedAt)
        post.mediaItems.forEach { media ->
            cachedMediaFile(post.connectionId, post.id, media.mediaId).delete()
        }
    }

    override suspend fun loadUsers(userIds: Set<String>): Result<List<User>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        userIds.mapNotNull { database.userDao().getUserById(it)?.toDomain() }
            .also { check(auth.currentUser?.uid == uid) { "Account changed while loading authors." } }
    }

    private suspend fun storePost(
        connectionId: String,
        document: DocumentSnapshot
    ): List<MediaItemEntity> {
        val post = document.toPostEntity(connectionId)
        val media = document.toMediaEntities(connectionId)
        database.withTransaction {
            database.postDao().upsertPost(post)
            database.postDao().upsertMedia(media)
        }
        return media
    }

    private suspend fun syncDocuments(
        connectionId: String,
        documents: List<DocumentSnapshot>
    ): Throwable? {
        var firstCacheError: Throwable? = null
        documents.forEach { document ->
            val status = PostStatus.valueOf(requireNotNull(document.getString("status")))
            val media = storePost(connectionId, document)
            if (status == PostStatus.DELETED) {
                media.forEach { cachedMediaFile(it.connectionId, it.postId, it.mediaId).delete() }
            } else {
                media.forEach { item ->
                    runCatching { cacheMedia(item) }
                        .onFailure { if (firstCacheError == null) firstCacheError = it }
                }
            }
        }
        return firstCacheError
    }

    private fun postsQuery(connectionId: String): Query = firestore.collection("connections")
        .document(connectionId)
        .collection("posts")
        .orderBy("createdAt", Query.Direction.DESCENDING)

    private suspend fun currentPage(uid: String): PostFeedPage = PostFeedPage(
        posts = database.postDao().loadPosts(uid),
        connectionIdsWithMore = synchronized(paginationLock) {
            if (paginationUid == uid) {
                activeConnectionIds.filterTo(mutableSetOf()) { pageHasMore[it] == true }
            } else {
                emptySet()
            }
        }
    )

    private fun resetPagination(uid: String?) {
        paginationUid = uid
        activeConnectionIds.clear()
        pageCursors.clear()
        pageHasMore.clear()
        connectionsWithOlderPages.clear()
    }

    private suspend fun cacheMedia(media: MediaItemEntity) {
        if (media.mediaType != MediaType.IMAGE) return
        val output = cachedMediaFile(media.connectionId, media.postId, media.mediaId)
        if (output.exists() && output.length() == media.sizeBytes) return

        output.parentFile?.mkdirs()
        val partial = File(output.parentFile, "${output.name}.download")
        if (partial.exists()) partial.delete()
        try {
            storage.reference.child(media.storagePath).getFile(partial).awaitDownload()
            check(partial.length() == media.sizeBytes) { "Downloaded photo size does not match its post." }
            if (output.exists()) check(output.delete()) { "Could not replace the cached photo." }
            check(partial.renameTo(output)) { "Could not move the downloaded photo into cache." }
        } finally {
            if (partial.exists()) partial.delete()
        }
    }
    override suspend fun loadConnections(): Result<List<Connection>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("User is not signed in.")
        connections.getCurrentUserConnections().getOrThrow()
        database.connectionMemberDao().getActiveMembershipsForUser(uid).mapNotNull { member ->
            database.connectionDao().getConnectionById(member.connectionId)
                ?.takeIf { it.status == ConnectionStatus.ACTIVE }
                ?.let { connection ->
                    connection.toDomain(
                        database.connectionMemberDao().getMembersByConnectionId(connection.id)
                            .map { it.toDomain() }
                    )
                }
        }.sortedByDescending { it.lastPostAt }.also { check(auth.currentUser?.uid == uid) }
    }
    override suspend fun loadConnectedUsers(): Result<List<User>> = connections.loadConnections()
    override fun observeConnectedUsers() = connections.observeConnections()

    override fun getFilteredPosts(posts: List<Post>, filter: FeedFilter): List<Post> =
        posts.filter { filter is FeedFilter.All || (filter is FeedFilter.Connection && it.connectionId == filter.connectionId) }
            .sortedByDescending { it.createdAt ?: it.clientCreatedAt }

    private fun DocumentSnapshot.toPostEntity(connectionId: String) = PostEntity(
        id = id,
        connectionId = connectionId,
        authorId = requireNotNull(getString("authorId")),
        postType = PostType.valueOf(requireNotNull(getString("postType"))),
        layoutType = LayoutType.valueOf(requireNotNull(getString("layoutType"))),
        caption = getString("caption"),
        clientCreatedAt = requireNotNull(getLong("clientCreatedAt")),
        createdAt = getTimestamp("createdAt")?.toDate()?.time,
        updatedAt = getTimestamp("updatedAt")?.toDate()?.time,
        status = PostStatus.valueOf(requireNotNull(getString("status"))),
        deletedAt = getTimestamp("deletedAt")?.toDate()?.time,
        deletedBy = getString("deletedBy"),
        schemaVersion = (getLong("schemaVersion") ?: 1).toInt(),
        localSyncStatus = LocalSyncStatus.SYNCED
    )

    private fun DocumentSnapshot.toMediaEntities(connectionId: String): List<MediaItemEntity> =
        (get("mediaItems") as? List<*>)?.mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            MediaItemEntity(
                connectionId = connectionId,
                postId = id,
                mediaId = item.requiredString("mediaId"),
                mediaType = MediaType.valueOf(item.requiredString("mediaType")),
                storagePath = item.requiredString("storagePath"),
                thumbnailPath = item["thumbnailPath"] as? String,
                mimeType = item.requiredString("mimeType"),
                width = item.requiredLong("width").toInt(),
                height = item.requiredLong("height").toInt(),
                durationMs = (item["durationMs"] as? Number)?.toLong(),
                sizeBytes = item.requiredLong("sizeBytes"),
                position = item.requiredLong("position").toInt()
            )
        }.orEmpty()

    private fun Map<*, *>.requiredString(key: String): String =
        this[key] as? String ?: error("Post media is missing $key.")

    private fun Map<*, *>.requiredLong(key: String): Long =
        (this[key] as? Number)?.toLong() ?: error("Post media is missing $key.")

    private fun Any?.asMap(): Map<*, *> = this as? Map<*, *>
        ?: error("Unexpected response from post service.")

    private fun cachedMediaFile(connectionId: String, postId: String, mediaId: String) = File(
        context.filesDir,
        "pending_media/$connectionId/$postId/$mediaId.jpg"
    )

    private companion object {
        const val POST_PAGE_SIZE = 20L
        const val DELETED_POST_PAGE_SIZE = 20L
    }

    private data class PageTarget(
        val connectionId: String,
        val cursor: DocumentSnapshot
    )
}

private suspend fun <T> Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    }

private suspend fun FileDownloadTask.awaitDownload(): FileDownloadTask.TaskSnapshot =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        continuation.invokeOnCancellation { cancel() }
    }
