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

    suspend fun copyFrom(resolver: ContentResolver, uri: Uri, id: String): String =
        withContext(Dispatchers.IO) {
            val target = file(id)
            directory.mkdirs()
            val buffer = ByteArray(8192)
            var length = 0
            try {
                val firstBytes = ByteArray(12)
                var firstCount = 0
                resolver.openInputStream(uri)?.use { source ->
                    target.outputStream().use { destination ->
                        while (true) {
                            val size = source.read(buffer)
                            if (size < 0) break
                            length += size
                            if (length > MAX_BYTES) throw IOException("Photo exceeds 5 MB.")
                            if (firstCount < 12) {
                                val take = minOf(size, 12 - firstCount)
                                buffer.copyInto(firstBytes, firstCount, 0, take)
                                firstCount += take
                            }
                            destination.write(buffer, 0, size)
                        }
                    }
                } ?: throw IOException("Cannot open selected photo.")
                val mime = when {
                    firstCount >= 3 && firstBytes[0] == 0xff.toByte() &&
                        firstBytes[1] == 0xd8.toByte() && firstBytes[2] == 0xff.toByte() -> "image/jpeg"
                    firstCount >= 8 && firstBytes.copyOfRange(0, 8).contentEquals(
                        byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)) -> "image/png"
                    firstCount >= 12 && String(firstBytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                        String(firstBytes, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
                    else -> throw IOException("Choose a JPEG, PNG, or WebP photo.")
                }
                if (length == 0) throw IOException("Empty photo.")
                mime
            } catch (error: Exception) {
                target.delete()
                throw error
            }
        }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { file(id).delete() }
}
