package com.smartfarm.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfarm.mobile.features.monitoring.PhotoThumbnailFactory
import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoThumbnailTest {
    @Test fun largePhotoGetsSmallReadableThumbnail() {
        val bitmap = Bitmap.createBitmap(1600, 900, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.GREEN)
            val input = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, input)
            val thumbnail = PhotoThumbnailFactory.create(input.toByteArray())
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.size, bounds)
            assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= PhotoThumbnailFactory.MAX_EDGE)
            assertTrue(bounds.outWidth > bounds.outHeight)
            assertTrue(thumbnail.isNotEmpty())
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun transparentPhotoKeepsTransparency() {
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.TRANSPARENT)
            val input = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, input)
            val thumbnail = PhotoThumbnailFactory.create(input.toByteArray())
            val decoded = requireNotNull(
                BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.size))
            try {
                assertEquals(0, Color.alpha(decoded.getPixel(0, 0)))
            } finally {
                decoded.recycle()
            }
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun exifRotationIsAppliedToThumbnail() {
        val source = File.createTempFile("smartfarm-thumbnail", ".jpg")
        try {
            val bitmap = Bitmap.createBitmap(80, 40, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.GREEN)
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            bitmap.recycle()
            val exif = ExifInterface(source.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString())
            exif.saveAttributes()

            val thumbnail = PhotoThumbnailFactory.create(source.readBytes())
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.size, bounds)
            assertTrue(bounds.outHeight > bounds.outWidth)
        } finally {
            source.delete()
        }
    }

    @Test fun invalidBytesAreRejected() {
        var rejected = false
        try {
            PhotoThumbnailFactory.create(byteArrayOf(1, 2, 3, 4))
        } catch (_: IOException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}
