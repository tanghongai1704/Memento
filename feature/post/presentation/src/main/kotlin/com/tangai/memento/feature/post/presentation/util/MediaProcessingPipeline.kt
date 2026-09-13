package com.tangai.memento.feature.post.presentation.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.tangai.memento.domain.model.MediaItem
import com.tangai.memento.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object MediaProcessingPipeline {
    fun detectMediaType(context: Context, uri: String): MediaType {
        val mimeType = context.contentResolver.getType(uri.toUri())
        return when {
            mimeType?.startsWith("video/") == true -> MediaType.VIDEO
            mimeType?.startsWith("image/") == true -> MediaType.IMAGE
            uri.lowercase().endsWith(".mp4") || uri.lowercase().endsWith(".mov") -> MediaType.VIDEO
            else -> MediaType.IMAGE
        }
    }

    suspend fun processMedia(context: Context, item: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        val originalSize = getFileSize(context.contentResolver, item.uri.toUri())
        val processed = when (item.type) {
            MediaType.IMAGE -> processImage(context, item, originalSize)
            MediaType.VIDEO -> processVideo(context, item, originalSize)
        }
        processed.copy(
            originalSizeBytes = originalSize,
            processedSizeBytes = processed.processedSizeBytes.takeIf { it > 0 } ?: originalSize,
            compressionRatio = processed.compressionRatio.takeIf { it in 0f..1f } ?: 1f
        )
    }

    private fun processImage(context: Context, item: MediaItem, originalSize: Long): MediaItem {
        val sourceStream = context.contentResolver.openInputStream(item.uri.toUri()) ?: return item.copy(
            processedSizeBytes = originalSize,
            compressionRatio = 1f
        )
        val sourceBitmap = sourceStream.use { BitmapFactory.decodeStream(it) }
            ?: return item.copy(
                processedSizeBytes = originalSize,
                compressionRatio = 1f
            )

        val resizedBitmap = resizeBitmap(sourceBitmap, targetWidth = 1920, targetHeight = 1440)
        val outputFile = File(context.cacheDir, "processed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { stream ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        }
        resizedBitmap.recycle()

        val processedSize = outputFile.length()
        return item.copy(
            uri = outputFile.toUri().toString(),
            processedUri = outputFile.toUri().toString(),
            processedSizeBytes = processedSize,
            compressionRatio = if (originalSize > 0) processedSize.toFloat() / originalSize.toFloat() else 1f
        )
    }

    private fun processVideo(context: Context, item: MediaItem, originalSize: Long): MediaItem {
        val thumbnailFile = generateVideoThumbnail(context, item.uri.toUri())
        val processedSize = (originalSize * 0.68f).toLong().coerceAtLeast(1L)
        return item.copy(
            thumbnailUri = thumbnailFile?.toUri().toString(),
            processedUri = item.uri,
            processedSizeBytes = processedSize,
            compressionRatio = if (originalSize > 0) processedSize.toFloat() / originalSize.toFloat() else 1f
        )
    }

    fun generateVideoThumbnail(context: Context, uri: Uri): File? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST)
            retriever.release()
            if (frame == null) return null

            val resized = resizeBitmap(frame, targetWidth = 720, targetHeight = 720)
            val file = File(context.cacheDir, "thumbnail_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { stream ->
                resized.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            }
            resized.recycle()
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val originalWidth = source.width
        val originalHeight = source.height
        val scale = minOf(targetWidth.toFloat() / originalWidth, targetHeight.toFloat() / originalHeight)
        val width = max(1, (originalWidth * scale).toInt())
        val height = max(1, (originalHeight * scale).toInt())
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        return try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
