package com.smartfarm.mobile

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.entity.LocalMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FoundationTest {
    @Test fun workManagerInitializesWithoutSchedulingWork() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
            as SmartFarmApplication
        assertNotNull(application.container.workManager)
    }

    @Test fun offlineQueueTracksAccountsAndReviewStatus() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        try {
            val queue = database.pendingOperationDao()
            val first = com.smartfarm.mobile.data.local.entity.PendingOperation(
                operationId = java.util.UUID.randomUUID().toString(),
                userId = "account-one", farmId = "farm-one", operationType = "harvest",
                payloadJson = "{}", createdAtUtc = 10L)
            val second = first.copy(operationId = java.util.UUID.randomUUID().toString(),
                userId = "account-two", createdAtUtc = 11L)
            queue.add(first)
            queue.add(second)
            assertEquals(first.operationId, queue.pending("account-one").single().operationId)
            queue.needsReview(first.operationId, "account-one", "Needs review")
            assertEquals(0, queue.pending("account-one").size)
            queue.retryAfterReview(first.operationId, "account-one")
            assertEquals(1, queue.pending("account-one").size)
            queue.remove(first.operationId, "account-one")
            assertEquals(0, queue.pending("account-one").size)
            assertEquals(1, queue.pending("account-two").size)
        } finally {
            database.close()
        }
    }

    @Test fun metadataRoundTripsThroughRoom() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SmartFarmDatabase::class.java).build()
        try {
            database.metadataDao().write(LocalMetadata("foundation", "ready"))
            assertEquals("ready", database.metadataDao().read("foundation"))
        } finally {
            database.close()
        }
    }
}
