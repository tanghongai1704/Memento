package com.tangai.memento.feature.post.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageException
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.MediaItemEntity
import com.tangai.memento.database.model.PostEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.ConnectionStatus
import com.tangai.memento.domain.model.LayoutType
import com.tangai.memento.domain.model.LocalSyncStatus
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.MemberStatus
import com.tangai.memento.domain.model.Post
import com.tangai.memento.domain.model.PostStatus
import com.tangai.memento.domain.model.PostType
import com.tangai.memento.feature.post.domain.PendingPhotoPost
import com.tangai.memento.feature.post.domain.PostRepository
import com.tangai.memento.network.NetworkStatusProvider
import com.tangai.memento.network.awaitFirebaseStorageTask
import com.tangai.memento.network.awaitFirebaseTask
import com.tangai.memento.network.di.IoDispatcher
import com.tangai.memento.network.runSuspendCatching
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val database: MementoDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val photoProcessor: PhotoProcessor,
    private val networkStatusProvider: NetworkStatusProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PostRepository {
    override suspend fun preparePhotoPost(
        connectionId: String,
        media: List<LocalMediaItem>,
        layoutType: LayoutType,
        caption: String?,
        onProgress: (Float) -> Unit
    ): Result<PendingPhotoPost> = withContext(ioDispatcher) { runSuspendCatching {
        val uid = auth.currentUser?.uid ?: error("Please sign in and try again.")
        check(getLatestPendingPhoto().getOrThrow() == null) {
            "Finish or discard the pending photo before creating another one."
        }
        require(media.size in 1..MAX_PHOTOS_PER_POST && media.all { it.type == MediaType.IMAGE }) {
            "Choose between 1 and $MAX_PHOTOS_PER_POST photos."
        }
        require((media.size == 1 && layoutType == LayoutType.SINGLE) ||
            (media.size > 1 && layoutType != LayoutType.SINGLE)) {
            "Choose a multi-photo layout when posting more than one photo."
        }
        val cleanCaption = caption?.trim()?.takeIf { it.isNotEmpty() }
        require((cleanCaption?.length ?: 0) <= 1000) { "Caption must be 1,000 characters or fewer." }
        val connection = database.connectionDao().getConnectionById(connectionId)
        val membership = database.connectionMemberDao().getMember(connectionId, uid)
        check(connection?.status == ConnectionStatus.ACTIVE && membership?.status == MemberStatus.ACTIVE) {
            "This connection is no longer active."
        }

        val postId = firestore.collection("connections").document(connectionId)
            .collection("posts").document().id
        val clientCreatedAt = System.currentTimeMillis()
        val pendingEntity = PostEntity(
            id = postId, connectionId = connectionId, authorId = uid,
            postType = PostType.PHOTO, layoutType = layoutType, caption = cleanCaption,
            clientCreatedAt = clientCreatedAt, createdAt = null, updatedAt = null,
            status = PostStatus.ACTIVE, deletedAt = null, deletedBy = null,
            schemaVersion = 1, localSyncStatus = LocalSyncStatus.PENDING
        )
        database.postDao().upsertPost(pendingEntity)

        try {
            val processedMedia = media.mapIndexed { position, item ->
                val mediaId = UUID.randomUUID().toString()
                val processed = photoProcessor.process(item.uri, connectionId, postId, mediaId)
                onProgress((position + 1).toFloat() / media.size.toFloat())
                processed to MediaItemEntity(
                    connectionId = connectionId, postId = postId, mediaId = mediaId,
                    mediaType = MediaType.IMAGE,
                    storagePath = storagePath(connectionId, postId, mediaId),
                    thumbnailPath = null, mimeType = "image/jpeg",
                    width = processed.width, height = processed.height,
                    durationMs = null, sizeBytes = processed.sizeBytes, position = position
                )
            }
            val mediaEntities = processedMedia.map { it.second }
            database.postDao().upsertMedia(mediaEntities)
            PendingPhotoPost(
                post = pendingEntity.toDomain(mediaEntities.map { it.toDomain() }),
                localUris = processedMedia.map { it.first.file.toUri().toString() },
                originalSizeBytes = processedMedia.map { it.first.originalSizeBytes }
            )
        } catch (error: Throwable) {
            database.postDao().updateSyncState(connectionId, postId, LocalSyncStatus.FAILED)
            throw error
        }
    } }

    override suspend fun uploadPendingPhoto(
        pending: PendingPhotoPost,
        onProgress: (Float) -> Unit
    ): Result<Post> = runSuspendCatching {
        val uid = auth.currentUser?.uid ?: error("Please sign in and try again.")
        val post = pending.post
        check(post.authorId == uid) { "This pending post belongs to another account." }
        val media = post.mediaItems.sortedBy { it.position }
        check(media.size in 1..MAX_PHOTOS_PER_POST && pending.localUris.size == media.size) {
            "A photo post contains an invalid number of media items."
        }
        val localFiles = withContext(ioDispatcher) {
            pending.localUris.map { File(requireNotNull(Uri.parse(it).path)) }.also { files ->
                check(files.zip(media).all { (file, item) ->
                    file.exists() && file.length() == item.sizeBytes
                }) {
                    "The processed photo is missing. Please select it again."
                }
            }
        }
        check(networkStatusProvider.isOnline()) {
            "No internet connection. Your photo is saved on this device; reconnect and tap Retry upload."
        }

        try {
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("authorId", uid)
                .build()
            media.zip(localFiles).forEachIndexed { index, (item, file) ->
                withTimeout(UPLOAD_TIMEOUT_MS) {
                    storage.reference.child(item.storagePath)
                        .putFile(file.toUri(), metadata)
                        .awaitFirebaseStorageTask { snapshot ->
                            val total = snapshot.totalByteCount.takeIf { it > 0 }
                                ?: return@awaitFirebaseStorageTask
                            val itemProgress = snapshot.bytesTransferred.toFloat() / total.toFloat()
                            onProgress((index + itemProgress) / media.size.toFloat())
                        }
                }
            }
            val response = withTimeout(FINALIZE_TIMEOUT_MS) {
                functions.getHttpsCallable("finalizePhotoPost").call(mapOf(
                    "connectionId" to post.connectionId,
                    "postId" to post.id,
                    "clientCreatedAt" to post.clientCreatedAt,
                    "caption" to post.caption,
                    "layoutType" to post.layoutType.name,
                    "mediaItems" to media.map { item ->
                        mapOf(
                            "mediaId" to item.mediaId,
                            "storagePath" to item.storagePath,
                            "mimeType" to item.mimeType,
                            "width" to item.width,
                            "height" to item.height,
                            "sizeBytes" to item.sizeBytes,
                            "position" to item.position
                        )
                    }
                )).awaitFirebaseTask().data as? Map<*, *> ?: error("Unexpected publish response.")
            }
            val createdAt = (response["createdAtMillis"] as? Number)?.toLong()
                ?: error("Publish response is missing its timestamp.")
            val updatedAt = (response["updatedAtMillis"] as? Number)?.toLong() ?: createdAt
            check(auth.currentUser?.uid == uid) { "Account changed during upload." }
            database.withTransaction {
                database.postDao().updateSyncState(
                    post.connectionId,
                    post.id,
                    LocalSyncStatus.SYNCED,
                    createdAt,
                    updatedAt
                )
                database.connectionDao().updateActivity(post.connectionId, createdAt, updatedAt)
            }
            post.copy(createdAt = createdAt, updatedAt = updatedAt)
        } catch (error: CancellationException) {
            currentCoroutineContext().ensureActive()
            database.postDao().updateSyncState(post.connectionId, post.id, LocalSyncStatus.FAILED)
            throw error.asUploadError()
        } catch (error: Throwable) {
            database.postDao().updateSyncState(post.connectionId, post.id, LocalSyncStatus.FAILED)
            throw error.asUploadError()
        }
    }

    override suspend fun getLatestPendingPhoto(): Result<PendingPhotoPost?> =
        withContext(ioDispatcher) { runSuspendCatching {
            val uid = auth.currentUser?.uid ?: return@runSuspendCatching null
            database.postDao().getRetryablePosts(uid).firstNotNullOfOrNull { entity ->
                val media = database.postDao().getMedia(entity.connectionId, entity.id)
                    .sortedBy { it.position }
                val files = media.map {
                    photoProcessor.outputFile(entity.connectionId, entity.id, it.mediaId)
                }
                if (media.isEmpty() || media.size > MAX_PHOTOS_PER_POST ||
                    files.zip(media).any { (file, item) ->
                        !file.exists() || file.length() != item.sizeBytes
                    }) {
                    database.postDao().deleteLocalDraft(entity.connectionId, entity.id)
                    photoProcessor.outputFile(entity.connectionId, entity.id, "placeholder")
                        .parentFile?.deleteRecursively()
                    null
                } else {
                    PendingPhotoPost(
                        entity.toDomain(media.map { it.toDomain() }),
                        files.map { it.toUri().toString() }
                    )
                }
            }
        } }

    override suspend fun discardPendingPhoto(pending: PendingPhotoPost): Result<Unit> =
        withContext(ioDispatcher) { runSuspendCatching {
            val uid = auth.currentUser?.uid ?: error("Please sign in and try again.")
            check(pending.post.authorId == uid) { "This pending post belongs to another account." }
            val deleted = database.postDao().deleteLocalDraft(pending.post.connectionId, pending.post.id)
            check(deleted == 1) { "This post is no longer a local draft." }
            pending.post.mediaItems.firstOrNull()?.let { media ->
                photoProcessor.outputFile(pending.post.connectionId, pending.post.id, media.mediaId)
                    .parentFile?.deleteRecursively()
            }
            Unit
        } }

    private fun storagePath(connectionId: String, postId: String, mediaId: String) =
        "connections/$connectionId/posts/$postId/$mediaId.jpg"

    private companion object {
        const val MAX_PHOTOS_PER_POST = 5
        const val UPLOAD_TIMEOUT_MS = 2 * 60 * 1000L
        const val FINALIZE_TIMEOUT_MS = 30 * 1000L
    }
}

private fun Throwable.asUploadError(): Throwable = when (this) {
    is TimeoutCancellationException -> IllegalStateException(
            "The upload timed out. Your photos are saved; check the connection and tap Retry upload.",
        this
    )
    is FirebaseNetworkException -> IllegalStateException(
            "The network connection was lost. Your photos are saved; reconnect and tap Retry upload.",
        this
    )
    is FirebaseFunctionsException -> when (code) {
        FirebaseFunctionsException.Code.UNAVAILABLE,
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> IllegalStateException(
            "The server could not be reached. Your photos are saved; tap Retry upload in a moment.",
            this
        )
        else -> this
    }
    is StorageException -> when (errorCode) {
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> IllegalStateException(
            "The upload could not finish on this connection. Your photos are saved; tap Retry upload.",
            this
        )
        else -> this
    }
    else -> this
}
