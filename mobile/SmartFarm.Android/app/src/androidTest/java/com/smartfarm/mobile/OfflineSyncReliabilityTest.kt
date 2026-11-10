package com.smartfarm.mobile

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.sync.LocalPhotoFiles
import com.smartfarm.mobile.data.sync.OfflineObservationQueue
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineSyncReliabilityTest {
    @Test fun inspectionAndPhotoCommitTogetherAndRollbackTogether() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        try {
            val queue = OfflineObservationQueue(database)
            val operationId = UUID.randomUUID().toString()
            val firstPhotoId = UUID.randomUUID().toString()
            val operation = PendingOperation(operationId, "account-a", "farm-a",
                "observation", "{}", System.currentTimeMillis())
            fun photo(id: String, userId: String = "account-a") =
                PendingPhoto(id, userId, "farm-a", "crop-a", null, operationId,
                    "image/jpeg", System.currentTimeMillis())

            queue.add(operation, photo(firstPhotoId))
            assertEquals(1, database.pendingOperationDao().pending("account-a").size)
            assertEquals(firstPhotoId,
                database.pendingPhotoDao().forObservationOperation("account-a", operationId)
                    .single().photoId)

            // A second photo insert should roll back when its parent ID already exists.
            val secondPhotoId = UUID.randomUUID().toString()
            var rolledBack = false
            try {
                queue.add(operation, photo(secondPhotoId))
            } catch (_: Exception) {
                rolledBack = true
            }
            assertTrue("The transaction must reject a duplicate operation", rolledBack)
            assertNull(database.pendingPhotoDao().find("account-a", secondPhotoId))
            assertEquals(1, database.pendingOperationDao().pending("account-a").size)
            assertEquals(1,
                database.pendingPhotoDao().forObservationOperation("account-a", operationId).size)

            val secondOperation = operation.copy(operationId = UUID.randomUUID().toString())
            val mismatchedPhoto = photo(UUID.randomUUID().toString(), "account-b")
                .copy(parentOperationId = secondOperation.operationId)
            var rejectedOtherAccount = false
            try {
                queue.add(secondOperation, mismatchedPhoto)
            } catch (_: IllegalArgumentException) {
                rejectedOtherAccount = true
            }
            assertTrue(rejectedOtherAccount)
            assertNull(database.pendingPhotoDao().find("account-b", mismatchedPhoto.photoId))
        } finally {
            database.close()
        }
    }

    @Test fun abandonedPhotoCleanupKeepsQueuedAndRecentFiles() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val files = LocalPhotoFiles(context)
        val queued = UUID.randomUUID().toString()
        val abandoned = UUID.randomUUID().toString()
        val recent = UUID.randomUUID().toString()
        val stagingId = UUID.randomUUID().toString()
        val queuedFile = files.file(queued)
        val abandonedFile = files.file(abandoned)
        val recentFile = files.file(recent)
        val staging = java.io.File(queuedFile.parentFile, "$stagingId.source")
        val old = System.currentTimeMillis() - 48L * 60 * 60 * 1000
        try {
            queuedFile.parentFile!!.mkdirs()
            listOf(queuedFile, abandonedFile, recentFile, staging)
                .forEach { it.writeText("fixture") }
            assertTrue(queuedFile.setLastModified(old))
            assertTrue(abandonedFile.setLastModified(old))
            assertTrue(staging.setLastModified(old))

            files.pruneAbandoned(setOf(queued))
            assertTrue("Pending photo must remain", queuedFile.exists())
            assertFalse("Stale orphan must be deleted", abandonedFile.exists())
            assertTrue("Recent file must remain", recentFile.exists())
            assertFalse("Stale partial compression must be deleted", staging.exists())
        } finally {
            listOf(queuedFile, abandonedFile, recentFile, staging).forEach { it.delete() }
        }
    }
}
