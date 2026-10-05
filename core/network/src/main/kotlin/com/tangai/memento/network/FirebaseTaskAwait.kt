package com.tangai.memento.network

import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.UploadTask
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class FirebaseTaskCanceledException(
    message: String = "Firebase canceled the operation. Please try again."
) : Exception(message)

private val directExecutor = Executor(Runnable::run)

suspend fun <T> Task<T>.awaitFirebaseTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener(directExecutor) { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener(directExecutor) { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener(directExecutor) {
        if (continuation.isActive) {
            continuation.resumeWithException(FirebaseTaskCanceledException())
        }
    }
}

suspend fun UploadTask.awaitFirebaseStorageTask(
    onProgress: ((UploadTask.TaskSnapshot) -> Unit)? = null
): UploadTask.TaskSnapshot = suspendCancellableCoroutine { continuation ->
    if (onProgress != null) addOnProgressListener(onProgress)
    addOnSuccessListener(directExecutor) { result ->
        if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener(directExecutor) { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener(directExecutor) {
        if (continuation.isActive) {
            continuation.resumeWithException(FirebaseTaskCanceledException())
        }
    }
    continuation.invokeOnCancellation { cancel() }
}

suspend fun FileDownloadTask.awaitFirebaseStorageTask(): FileDownloadTask.TaskSnapshot =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener(directExecutor) { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener(directExecutor) { error ->
            if (continuation.isActive) continuation.resumeWithException(error)
        }
        addOnCanceledListener(directExecutor) {
            if (continuation.isActive) {
                continuation.resumeWithException(FirebaseTaskCanceledException())
            }
        }
        continuation.invokeOnCancellation { cancel() }
    }
