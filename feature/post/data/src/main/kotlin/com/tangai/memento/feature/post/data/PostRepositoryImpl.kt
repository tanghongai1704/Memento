package com.tangai.memento.feature.post.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import com.tangai.memento.database.MementoDatabase
import com.tangai.memento.database.model.MediaItemEntity
import com.tangai.memento.database.model.PostEntity
import com.tangai.memento.database.model.toDomain
import com.tangai.memento.domain.model.LocalMediaItem
import com.tangai.memento.domain.model.MediaType
import com.tangai.memento.domain.model.Post
import com.tangai.memento.feature.post.domain.PendingPhotoPost
import com.tangai.memento.feature.post.domain.PostRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PostRepositoryImpl @Inject constructor(
    private val database: MementoDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val functions: FirebaseFunctions,
    private val photoProcessor: PhotoProcessor
) : PostRepository {
    override suspend fun preparePhotoPost(
        connectionId: String,
        media: LocalMediaItem,
        caption: String?
    ): Result<PendingPhotoPost> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Please sign in and try again.")
        require(media.type == MediaType.IMAGE) { "MVP currently supports one photo per post." }
        val cleanCaption = caption?.trim()?.takeIf { it.isNotEmpty() }
        require((cleanCaption?.length ?: 0) <= 1000) { "Caption must be 1,000 characters or fewer." }
        val connection = database.connectionDao().getConnectionById(connectionId)
        val membership = database.connectionMemberDao().getMember(connectionId, uid)
        check(connection?.status == "ACTIVE" && membership?.status == "ACTIVE") {
            "This connection is no longer active."
        }

        val postId = firestore.collection("connections").document(connectionId)
            .collection("posts").document().id
        val mediaId = UUID.randomUUID().toString()
        val clientCreatedAt = System.currentTimeMillis()
        val pendingEntity = PostEntity(
            id = postId, connectionId = connectionId, authorId = uid,
            postType = "PHOTO", layoutType = "SINGLE", caption = cleanCaption,
            clientCreatedAt = clientCreatedAt, createdAt = null, updatedAt = null,
            status = "ACTIVE", deletedAt = null, deletedBy = null,
            schemaVersion = 1, localSyncStatus = "PENDING"
        )
        database.postDao().upsertPost(pendingEntity)

        try {
            val processed = photoProcessor.process(media.uri, connectionId, postId, mediaId)
            val storagePath = storagePath(connectionId, postId, mediaId)
            val mediaEntity = MediaItemEntity(
                connectionId = connectionId, postId = postId, mediaId = mediaId,
                mediaType = "IMAGE", storagePath = storagePath, thumbnailPath = null,
                mimeType = "image/jpeg", width = processed.width, height = processed.height,
                durationMs = null, sizeBytes = processed.sizeBytes, position = 0
            )
            database.postDao().upsertMedia(listOf(mediaEntity))
            PendingPhotoPost(
                post = pendingEntity.toDomain(listOf(mediaEntity.toDomain())),
                localUri = processed.file.toUri().toString()
            )
        } catch (error: Throwable) {
            database.postDao().updateSyncState(connectionId, postId, "FAILED")
            throw error
        }
    }

    override suspend fun uploadPendingPhoto(
        pending: PendingPhotoPost,
        onProgress: (Float) -> Unit
    ): Result<Post> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Please sign in and try again.")
        val post = pending.post
        check(post.authorId == uid) { "This pending post belongs to another account." }
        val media = post.mediaItems.singleOrNull()
            ?: error("A photo post must contain exactly one media item.")
        val localFile = File(requireNotNull(Uri.parse(pending.localUri).path))
        check(localFile.exists() && localFile.length() == media.sizeBytes) {
            "The processed photo is missing. Please select it again."
        }

        try {
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("authorId", uid)
                .build()
            storage.reference.child(media.storagePath)
                .putFile(localFile.toUri(), metadata)
                .awaitUpload(onProgress)
            val response = functions.getHttpsCallable("finalizePhotoPost").call(mapOf(
                "connectionId" to post.connectionId,
                "postId" to post.id,
                "mediaId" to media.mediaId,
                "clientCreatedAt" to post.clientCreatedAt,
                "caption" to post.caption,
                "storagePath" to media.storagePath,
                "mimeType" to media.mimeType,
                "width" to media.width,
                "height" to media.height,
                "sizeBytes" to media.sizeBytes
            )).awaitTask().data as? Map<*, *> ?: error("Unexpected publish response.")
            val createdAt = (response["createdAtMillis"] as? Number)?.toLong()
                ?: error("Publish response is missing its timestamp.")
            val updatedAt = (response["updatedAtMillis"] as? Number)?.toLong() ?: createdAt
            check(auth.currentUser?.uid == uid) { "Account changed during upload." }
            database.withTransaction {
                database.postDao().updateSyncState(post.connectionId, post.id, "SYNCED", createdAt, updatedAt)
                database.connectionDao().updateActivity(post.connectionId, createdAt, updatedAt)
            }
            post.copy(createdAt = createdAt, updatedAt = updatedAt)
        } catch (error: Throwable) {
            database.postDao().updateSyncState(post.connectionId, post.id, "FAILED")
            throw error
        }
    }

    override suspend fun getLatestPendingPhoto(): Result<PendingPhotoPost?> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching null
        database.postDao().getRetryablePosts(uid).firstNotNullOfOrNull { entity ->
            val media = database.postDao().getMedia(entity.connectionId, entity.id).singleOrNull()
                ?: return@firstNotNullOfOrNull null
            val file = photoProcessor.outputFile(entity.connectionId, entity.id, media.mediaId)
            if (!file.exists() || file.length() != media.sizeBytes) null
            else PendingPhotoPost(entity.toDomain(listOf(media.toDomain())), file.toUri().toString())
        }
    }

    private fun storagePath(connectionId: String, postId: String, mediaId: String) =
        "connections/$connectionId/posts/$postId/$mediaId.jpg"
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
}

private suspend fun UploadTask.awaitUpload(onProgress: (Float) -> Unit): UploadTask.TaskSnapshot =
    suspendCancellableCoroutine { continuation ->
        addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount.takeIf { it > 0 } ?: return@addOnProgressListener
            onProgress(snapshot.bytesTransferred.toFloat() / total.toFloat())
        }
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        continuation.invokeOnCancellation { cancel() }
    }
