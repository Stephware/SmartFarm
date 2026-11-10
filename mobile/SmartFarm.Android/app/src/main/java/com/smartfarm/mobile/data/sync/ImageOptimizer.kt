package com.smartfarm.mobile.data.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

/**
 * Optimizes a photo exactly once before it enters the offline sync queue.
 * Keeps already-small PNG/WebP files lossless and respects the API's 5 MB limit.
 */
internal object ImageOptimizer {
    const val MAX_SOURCE_BYTES = 30L * 1024 * 1024
    const val MAX_EDGE = 2560
    const val JPEG_QUALITY = 93
    private val jpegQualities = intArrayOf(JPEG_QUALITY, 90, 87, 85)

    fun optimize(source: File, destination: File, mime: String): String {
        require(source != destination) { "Source and destination must differ." }
        if (source.length() !in 1L..MAX_SOURCE_BYTES) {
            throw IOException("Photo must be smaller than 30 MB before optimization.")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > 120_000_000L) {
            throw IOException("Photo cannot be decoded at a safe size.")
        }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(source.absolutePath, options)
            ?: throw IOException("Photo cannot be decoded.")

        // Preserve source bytes whenever processing would not improve a small photo.
        // This also validates the image pixels before allowing a lossless copy.
        if (source.length() <= LocalPhotoFiles.MAX_BYTES &&
            (mime != "image/jpeg" ||
                (source.length() <= 1024L * 1024L &&
                    maxOf(bounds.outWidth, bounds.outHeight) <= MAX_EDGE))) {
            try {
                source.copyTo(destination, overwrite = true)
                return mime
            } catch (error: Exception) {
                destination.delete()
                throw error
            } finally {
                decoded.recycle()
            }
        }
        var working = decoded
        try {
            val exif = ExifInterface(source.absolutePath)
            if (exif.rotationDegrees != 0 || exif.isFlipped) {
                val matrix = Matrix().apply {
                    if (exif.isFlipped) postScale(-1f, 1f)
                    postRotate(exif.rotationDegrees.toFloat())
                }
                working = Bitmap.createBitmap(
                    decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
            val longest = maxOf(working.width, working.height)
            if (longest > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(working,
                    maxOf(1, (working.width * scale).toInt()),
                    maxOf(1, (working.height * scale).toInt()), true)
                if (working !== decoded) working.recycle()
                working = scaled
            }

            if (mime == "image/jpeg") {
                for (quality in jpegQualities) {
                    write(working, destination, Bitmap.CompressFormat.JPEG, quality)
                    if (destination.length() <= LocalPhotoFiles.MAX_BYTES) break
                }
            } else {
                // Oversized lossless photos stay lossless; fail instead of removing transparency.
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.PNG
                write(working, destination, format, 100)
            }

            // Never reject a valid under-limit source merely because re-encoding is larger.
            // Require at least 10% savings before introducing JPEG generation loss.
            if (source.length() <= LocalPhotoFiles.MAX_BYTES &&
                (destination.length() > LocalPhotoFiles.MAX_BYTES ||
                    destination.length() * 10 >= source.length() * 9)) {
                source.copyTo(destination, overwrite = true)
                return mime
            }
            if (destination.length() > LocalPhotoFiles.MAX_BYTES) {
                throw IOException("Photo cannot fit within 5 MB without reducing quality further.")
            }
            return if (mime == "image/jpeg") "image/jpeg"
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "image/webp"
                else "image/png"
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally {
            if (working !== decoded) working.recycle()
            decoded.recycle()
        }
    }

    private fun write(bitmap: Bitmap, target: File, format: Bitmap.CompressFormat, quality: Int) {
        target.outputStream().use {
            if (!bitmap.compress(format, quality, it)) throw IOException("Could not optimize photo.")
        }
    }
}
