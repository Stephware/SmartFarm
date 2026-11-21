package com.smartfarm.mobile.features.monitoring

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Makes small, correctly oriented previews from authenticated photo bytes.
 * Only the downscaled preview is kept for each visible history item.
 */
internal object PhotoThumbnailFactory {
    const val MAX_EDGE = 320

    fun create(bytes: ByteArray): ByteArray {
        if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) {
            throw IOException("Invalid observation photo.")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > 120_000_000L) {
            throw IOException("Observation photo cannot be decoded.")
        }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE * 2) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Observation photo cannot be decoded.")
        var current = decoded
        try {
            val exif = try {
                ExifInterface(ByteArrayInputStream(bytes))
            } catch (_: IOException) { null }
            val rotation = exif?.rotationDegrees ?: 0
            val flipped = exif?.isFlipped == true
            if (rotation != 0 || flipped) {
                val matrix = Matrix().apply {
                    if (flipped) postScale(-1f, 1f)
                    postRotate(rotation.toFloat())
                }
                current = Bitmap.createBitmap(
                    decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }

            val longest = maxOf(current.width, current.height)
            if (longest > MAX_EDGE) {
                val ratio = MAX_EDGE.toFloat() / longest
                val smaller = Bitmap.createScaledBitmap(current,
                    maxOf(1, (current.width * ratio).toInt()),
                    maxOf(1, (current.height * ratio).toInt()), true)
                if (current !== decoded) current.recycle()
                current = smaller
            }

            val output = ByteArrayOutputStream()
            val format = if (current.hasAlpha()) Bitmap.CompressFormat.PNG
                else Bitmap.CompressFormat.JPEG
            val quality = if (format == Bitmap.CompressFormat.JPEG) 88 else 100
            if (!current.compress(format, quality, output)) {
                throw IOException("Could not create photo thumbnail.")
            }
            return output.toByteArray()
        } finally {
            if (current !== decoded) current.recycle()
            decoded.recycle()
        }
    }
}
