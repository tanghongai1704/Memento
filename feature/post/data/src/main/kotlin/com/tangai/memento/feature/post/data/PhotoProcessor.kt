package com.tangai.memento.feature.post.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.max

data class ProcessedPhoto(val file: File, val width: Int, val height: Int, val sizeBytes: Long)

class PhotoProcessor @Inject constructor(@ApplicationContext private val context: Context) {
    fun outputFile(connectionId: String, postId: String, mediaId: String): File =
        File(context.filesDir, "pending_media/$connectionId/$postId/$mediaId.jpg")

    fun process(source: String, connectionId: String, postId: String, mediaId: String): ProcessedPhoto {
        val uri = source.toUri()
        require(context.contentResolver.getType(uri)?.startsWith("image/") != false) {
            "Please choose an image."
        }
        val decoded = decodeSampled(uri) ?: error("The selected image could not be opened.")
        val oriented = applyOrientation(decoded, readOrientation(uri))
        if (oriented !== decoded) decoded.recycle()
        var bitmap = resizeLongestEdge(oriented, MAX_EDGE)
        if (bitmap !== oriented) oriented.recycle()

        val output = outputFile(connectionId, postId, mediaId)
        output.parentFile?.mkdirs()
        var quality = 82
        writeJpeg(bitmap, output, quality)
        while (output.length() > MAX_BYTES && quality > 46) {
            quality -= 8
            writeJpeg(bitmap, output, quality)
        }
        while (output.length() > MAX_BYTES && max(bitmap.width, bitmap.height) > 640) {
            val resized = resizeLongestEdge(bitmap, (max(bitmap.width, bitmap.height) * 0.82f).toInt())
            bitmap.recycle()
            bitmap = resized
            quality = 74
            writeJpeg(bitmap, output, quality)
        }
        check(output.length() in 1..MAX_BYTES) { "The photo is still larger than 5 MiB after compression." }
        return ProcessedPhoto(output, bitmap.width, bitmap.height, output.length()).also { bitmap.recycle() }
    }

    private fun decodeSampled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun readOrientation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postScale(-1f, 1f); matrix.postRotate(270f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postScale(-1f, 1f); matrix.postRotate(90f) }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun resizeLongestEdge(source: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= maxEdge) return source
        val scale = maxEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).toInt().coerceAtLeast(1),
            (source.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun writeJpeg(bitmap: Bitmap, output: File, quality: Int) {
        FileOutputStream(output, false).use {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it)) { "Could not encode JPEG." }
        }
    }

    private companion object {
        const val MAX_EDGE = 1920
        const val MAX_BYTES = 5L * 1024L * 1024L
    }
}
