package com.example.legoscanner.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.IOException

data class PreparedImage(
    val bitmap: Bitmap,
    val base64: String
)

object ImageUtils {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 85

    fun prepare(image: ImageProxy): PreparedImage {
        val bytes = image.planes[0].buffer.let { buffer ->
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Nie udało się odczytać zdjęcia")

        return prepare(rotate(decoded, image.imageInfo.rotationDegrees))
    }

    fun prepare(context: Context, uri: Uri): PreparedImage {
        val decoded = context.contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IOException("Nie udało się odczytać obrazu")

        return prepare(decoded)
    }

    private fun prepare(bitmap: Bitmap): PreparedImage {
        val scaled = scaleDown(bitmap)
        val base64 = ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        }
        return PreparedImage(scaled, base64)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap

        val ratio = MAX_DIMENSION.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    }
}
