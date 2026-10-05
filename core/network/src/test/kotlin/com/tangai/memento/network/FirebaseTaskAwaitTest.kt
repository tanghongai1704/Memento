package com.tangai.memento.network

import android.app.Activity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import java.util.concurrent.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseTaskAwaitTest {
    @Test
    fun `awaitFirebaseTask returns successful result`() = runBlocking {
        val source = TaskCompletionSource<String>()
        val result = async { source.task.awaitFirebaseTask() }

        source.setResult("done")

        assertEquals("done", withTimeout(1_000) { result.await() })
    }

    @Test
    fun `awaitFirebaseTask converts Firebase cancellation to retryable failure`() = runBlocking {
        val result = async { runCatching { CanceledTask<String>().awaitFirebaseTask() } }

        val error = withTimeout(1_000) { result.await().exceptionOrNull() }
        assertTrue(error is FirebaseTaskCanceledException)
    }

    @Test
    fun `awaitFirebaseTask preserves caller cancellation`() = runBlocking {
        val source = TaskCompletionSource<String>()
        val result = async { source.task.awaitFirebaseTask() }

        result.cancelAndJoin()

        assertTrue(result.isCancelled)
    }

    @Test
    fun `runSuspendCatching does not swallow cancellation`() = runBlocking {
        val result = async {
            runSuspendCatching<String> { throw CancellationException("stopped") }
        }

        result.join()

        assertTrue(result.isCancelled)
    }
}

private class CanceledTask<T> : Task<T>() {
    override fun isComplete() = true
    override fun isSuccessful() = false
    override fun isCanceled() = true
    override fun getResult(): T = throw CancellationException("canceled")
    override fun <X : Throwable> getResult(exceptionType: Class<X>): T =
        throw CancellationException("canceled")
    override fun getException(): Exception? = null

    override fun addOnSuccessListener(listener: OnSuccessListener<in T>) = this
    override fun addOnSuccessListener(
        executor: java.util.concurrent.Executor,
        listener: OnSuccessListener<in T>
    ) = this
    override fun addOnSuccessListener(activity: Activity, listener: OnSuccessListener<in T>) = this

    override fun addOnFailureListener(listener: OnFailureListener) = this
    override fun addOnFailureListener(
        executor: java.util.concurrent.Executor,
        listener: OnFailureListener
    ) = this
    override fun addOnFailureListener(activity: Activity, listener: OnFailureListener) = this

    override fun addOnCanceledListener(
        executor: java.util.concurrent.Executor,
        listener: com.google.android.gms.tasks.OnCanceledListener
    ): Task<T> = apply { executor.execute(listener::onCanceled) }
}
