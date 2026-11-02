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
