package com.smartfarm.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

            assertTrue(original.length() > LocalPhotoFiles.MAX_BYTES)
            assertEquals("image/jpeg", ImageOptimizer.optimize(original, optimized, "image/jpeg"))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(optimized.absolutePath, bounds)
            assertTrue(maxOf(bounds.outWidth, bounds.outHeight) <= ImageOptimizer.MAX_EDGE)
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
}
