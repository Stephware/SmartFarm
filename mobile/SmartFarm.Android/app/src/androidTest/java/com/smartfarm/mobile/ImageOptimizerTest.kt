package com.smartfarm.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartfarm.mobile.data.sync.ImageOptimizer
import com.smartfarm.mobile.data.sync.LocalPhotoFiles
import java.io.File
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageOptimizerTest {
    @Test fun largeJpegIsCompressedWithoutExceedingPhotoLimit() {
        val original = File.createTempFile("smartfarm-large", ".jpg")
        val optimized = File.createTempFile("smartfarm-optimized", ".jpg")
        try {
            val width = 3000
            val height = 2000
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val random = Random(42)
            for (i in pixels.indices) pixels[i] = (0xff000000.toInt() or random.nextInt(0x1000000))
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            bitmap.recycle()

            // Rotate the source using EXIF; the optimized image should bake it in.
            val exif = ExifInterface(original.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_ROTATE_90.toString())
            exif.saveAttributes()
            assertTrue(original.length() > LocalPhotoFiles.MAX_BYTES)
            assertEquals("image/jpeg", ImageOptimizer.optimize(original, optimized, "image/jpeg"))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(optimized.absolutePath, bounds)
            assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= ImageOptimizer.MAX_EDGE)
            assertTrue(bounds.outHeight > bounds.outWidth)
            assertEquals(0, ExifInterface(optimized.absolutePath).rotationDegrees)
            assertTrue(optimized.length() in 1L..LocalPhotoFiles.MAX_BYTES.toLong())
        } finally {
            original.delete()
            optimized.delete()
        }
    }

    @Test fun smallTransparentPngRemainsLossless() {
        val original = File.createTempFile("smartfarm-png", ".png")
        val optimized = File.createTempFile("smartfarm-optimized", ".png")
        try {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.TRANSPARENT)
            original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            assertEquals("image/png", ImageOptimizer.optimize(original, optimized, "image/png"))
            assertTrue(original.readBytes().contentEquals(optimized.readBytes()))
        } finally {
            original.delete()
            optimized.delete()
        }
    }
    @Test fun smallJpegPreservesOriginalBytesAndQuality() {
        val original = File.createTempFile("smartfarm-small", ".jpg")
        val optimized = File.createTempFile("smartfarm-optimized", ".jpg")
        try {
            val bitmap = Bitmap.createBitmap(320, 200, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.GREEN)
            original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it) }
            bitmap.recycle()
            assertEquals("image/jpeg", ImageOptimizer.optimize(original, optimized, "image/jpeg"))
            assertTrue(original.readBytes().contentEquals(optimized.readBytes()))
        } finally {
            original.delete()
            optimized.delete()
        }
    }

    @Test fun corruptSmallPngIsRejectedBeforeOfflineQueue() {
        val original = File.createTempFile("smartfarm-corrupt", ".png")
        val optimized = File.createTempFile("smartfarm-optimized", ".png")
        try {
            original.writeBytes(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10, 0, 0))
            var rejected = false
            try {
                ImageOptimizer.optimize(original, optimized, "image/png")
            } catch (_: IOException) {
                rejected = true
            }
            assertTrue("Corrupt image must never reach the upload queue", rejected)
        } finally {
            original.delete()
            optimized.delete()
        }
    }

}
