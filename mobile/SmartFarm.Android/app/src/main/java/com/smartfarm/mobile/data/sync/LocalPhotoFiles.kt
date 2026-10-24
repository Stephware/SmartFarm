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
