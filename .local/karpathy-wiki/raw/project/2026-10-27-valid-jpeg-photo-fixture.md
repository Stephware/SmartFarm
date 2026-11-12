# Valid JPEG fixture and photo-test results

> Source: SmartFarm source, emulator/JVM result XML, local Git and confirmed GitHub PR response, https://github.com/Stephware/SmartFarm/pull/47
> Collected: 2026-10-27
> Published: Unknown

## Reproduction and scope

The original photoBytesAreCopiedIntoPrivateStorageBeforeSync instrumentation test failed on Pixel_9 with java.io.IOException: Photo cannot be decoded at a safe size. Its fixture contained only six JPEG-like bytes. The fixture now creates a 16 by 16 Bitmap, compresses it to JPEG, verifies that the privately stored image decodes at the expected dimensions, recycles both bitmaps, and retains deletion checks. Exact compressed bytes are not the contract because ImageOptimizer may recompress. No application code or schema changed.

## Executed verification

The existing Android SDK was passed through ANDROID_HOME=/Users/stephen/Library/Android/sdk and the existing Android Studio JDK through JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home; no project configuration or dependencies changed.

Photo command: bash ./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartfarm.mobile.OfflinePhotoQueueTest,com.smartfarm.mobile.ImageOptimizerTest --offline --console=plain

The JVM task was initially UP-TO-DATE, then actually rerun with: bash ./gradlew :app:testDebugUnitTest --rerun-tasks --offline --console=plain

TEST-com.smartfarm.mobile.ApiClientFactoryTest.xml: {"tests": "3", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.EnrollmentApiContractTest.xml: {"tests": "6", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.PhotoUploadContractTest.xml: {"tests": "1", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.PracticalInputsTest.xml: {"tests": "2", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.StandaloneApiIntegrationTest.xml: {"tests": "2", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.SyncApiContractTest.xml: {"tests": "1", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.TaskApiContractTest.xml: {"tests": "1", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.ThemeContrastTest.xml: {"tests": "1", "failures": "0", "errors": "0", "skipped": "0"}

TEST-com.smartfarm.mobile.WeatherApiContractTest.xml: {"tests": "1", "failures": "0", "errors": "0", "skipped": "0"}

Total testDebugUnitTest: {"tests": 18, "failures": 0, "errors": 0, "skipped": 0}

TEST-Pixel_9(AVD) - 17-_app-.xml: {"tests": "5", "failures": "0", "errors": "0", "skipped": "0"}

Total debug: {"tests": 5, "failures": 0, "errors": 0, "skipped": 0}


All five selected emulator tests and all 18 Android JVM tests passed, including PhotoUploadContractTest. The emulator was started for this task and shut down afterward. Existing migration-parameter naming warnings were unchanged. A generated Room schema 4.json was moved to /private/tmp/smartfarm-photo-room-schema-4.json instead of adding unrelated schema changes. No backend, SQL integration, or full device UI suite was rerun. Independent read-only review found no critical or important issues.


## Merge verification

PR 47 merged into dev at 53d4b389b1c14a4d09f1a38c502c54a8b83e89f7. Its Git tree matches the tested branch a8c2cf64907fd2c9f835ec4efddbb687bdde2d54. Local dev matches origin/dev and the working tree is clean.


## Repository file: mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/OfflinePhotoQueueTest.kt

package com.smartfarm.mobile

import androidx.room.Room
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.smartfarm.mobile.data.sync.LocalPhotoFiles
import java.io.File
import java.util.UUID
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePhotoQueueTest {
    @Test fun pendingPhotoFollowsObservationAndStaysWithItsAccount() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        try {
            val ops = database.pendingOperationDao()
            val photos = database.pendingPhotoDao()
            val operation = PendingOperation("observation-1", "farmer-a", "farm-1",
                "observation", "{}", 1L)
            ops.add(operation)
            photos.add(PendingPhoto("photo-1", "farmer-a", "farm-1", "crop-1",
                observationId = null, parentOperationId = operation.operationId,
                mimeType = "image/jpeg", savedAtUtcMillis = 1L))
            photos.add(PendingPhoto("photo-2", "farmer-b", "farm-1", "crop-1",
                observationId = "observation-b", parentOperationId = null,
                mimeType = "image/png", savedAtUtcMillis = 2L))

            assertTrue(photos.ready("farmer-a").isEmpty())
            assertEquals(1, photos.ready("farmer-b").size)
            photos.resolveObservation("farmer-a", operation.operationId, "observation-a")
            assertEquals("observation-a", photos.ready("farmer-a").single().observationId)

            photos.review("farmer-a", "photo-1", "Server rejected photo")
            assertTrue(photos.ready("farmer-a").isEmpty())
            photos.retry("farmer-a", "photo-1")
            assertEquals(1, photos.ready("farmer-a").size)
            photos.remove("farmer-a", "photo-1")
            assertNull(photos.find("farmer-a", "photo-1"))
            assertEquals(1, photos.ready("farmer-b").size)
            assertEquals(1, ops.pending("farmer-a").size)
        } finally {
            database.close()
        }
    }

    @Test fun photoBytesAreCopiedIntoPrivateStorageBeforeSync() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val staging = File(context.cacheDir, "photo-test-${UUID.randomUUID()}.jpg")
        val files = LocalPhotoFiles(context)
        val id = UUID.randomUUID().toString()
        val sample = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            staging.outputStream().use {
                assertTrue(sample.compress(Bitmap.CompressFormat.JPEG, 100, it))
            }
            assertEquals("image/jpeg",
                files.copyFrom(context.contentResolver, Uri.fromFile(staging), id))
            assertTrue(files.file(id).isFile)
            val stored = requireNotNull(BitmapFactory.decodeFile(files.file(id).absolutePath))
            try {
                assertEquals(16, stored.width)
                assertEquals(16, stored.height)
            } finally {
                stored.recycle()
            }
            files.delete(id)
            assertTrue(!files.file(id).exists())
        } finally {
            sample.recycle()
            staging.delete()
            files.file(id).delete()
        }
    }

    @Test fun reviewedInventoryChangesCanBeUpdatedWithoutChangingTheirId() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        try {
            val dao = db.pendingOperationDao()
            dao.add(PendingOperation("stock-1", "farmer-a", "farm-1", "inventory",
                "{\"old\":1}", 1L))
            dao.needsReview("stock-1", "farmer-a", "Inventory changed")
            assertEquals(0, dao.rebaseInventory("stock-1", "farmer-b", "{\"new\":2}"))
            assertEquals(1, dao.rebaseInventory("stock-1", "farmer-a", "{\"new\":2}"))
            assertEquals("stock-1", dao.pending("farmer-a").single().operationId)
            assertEquals("{\"new\":2}", dao.pending("farmer-a").single().payloadJson)
        } finally {
            db.close()
        }
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/ImageOptimizerTest.kt

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


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/ImageOptimizer.kt

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
        if (mime != "image/jpeg" && source.length() <= LocalPhotoFiles.MAX_BYTES) {
            source.copyTo(destination, overwrite = true)
            return mime
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > 120_000_000L) {
            throw IOException("Photo cannot be decoded at a safe size.")
        }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Photo cannot be decoded.")
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

            if (destination.length() > LocalPhotoFiles.MAX_BYTES) {
                throw IOException("Photo cannot fit within 5 MB without reducing quality further.")
            }
            if (source.length() <= LocalPhotoFiles.MAX_BYTES &&
                source.length() <= destination.length()) {
                source.copyTo(destination, overwrite = true)
                return mime
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


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/LocalPhotoFiles.kt

package com.smartfarm.mobile.data.sync

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Only app-generated file names are used. Files are not shared or backed up.
class LocalPhotoFiles(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "queued-photos")
    private val cameraDirectory = File(appContext.filesDir, "camera")
    companion object { const val MAX_BYTES = 5 * 1024 * 1024 }

    init {
        // Clean abandoned camera captures after a day; queued copies remain untouched.
        cameraDirectory.listFiles()?.forEach { file ->
            if (file.name.matches(Regex("capture-[0-9a-fA-F-]+\\.jpg")) &&
                System.currentTimeMillis() - file.lastModified() > 86_400_000L) {
                file.delete()
            }
        }
    }

    fun deleteTemporaryCameraSource(uri: Uri) {
        if (uri.authority != "${appContext.packageName}.photos") return
        val name = uri.lastPathSegment ?: return
        if (name.matches(Regex("capture-[0-9a-fA-F-]+\\.jpg")))
            File(cameraDirectory, name).delete()
    }

    fun file(id: String): File {
        val uuid = UUID.fromString(id).toString()
        return File(directory, "$uuid.image")
    }

    // Stage up to 30 MB, then store one compressed file for all upload retries.
    suspend fun copyFrom(resolver: ContentResolver, uri: Uri, id: String): String =
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val destination = file(id)
            val staging = File(directory, "${UUID.fromString(id)}.source")
            val buffer = ByteArray(8192)
            var length = 0L
            try {
                resolver.openInputStream(uri)?.use { input ->
                    staging.outputStream().use { output ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            length += count
                            if (length > ImageOptimizer.MAX_SOURCE_BYTES) {
                                throw IOException("Photo must be smaller than 30 MB.")
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: throw IOException("Cannot open selected photo.")

                val header = ByteArray(12)
                val headerSize = staging.inputStream().use { it.read(header) }
                val mime = when {
                    headerSize >= 3 && header[0] == 0xff.toByte() &&
                        header[1] == 0xd8.toByte() && header[2] == 0xff.toByte() -> "image/jpeg"
                    headerSize >= 8 && header.copyOfRange(0, 8).contentEquals(
                        byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)) -> "image/png"
                    headerSize >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                        String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
                    else -> throw IOException("Choose a JPEG, PNG, or WebP photo.")
                }
                if (length == 0L) throw IOException("Empty photo.")
                ImageOptimizer.optimize(staging, destination, mime)
            } catch (error: Exception) {
                destination.delete()
                throw error
            } finally {
                staging.delete()
            }
        }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { file(id).delete() }
}
