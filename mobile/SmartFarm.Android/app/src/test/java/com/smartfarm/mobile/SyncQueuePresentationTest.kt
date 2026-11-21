package com.smartfarm.mobile

import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.features.syncstatus.SyncRecordState
import com.smartfarm.mobile.features.syncstatus.syncState
import com.smartfarm.mobile.features.syncstatus.syncQueueSummary
import com.smartfarm.mobile.features.syncstatus.syncRecordType
import com.smartfarm.mobile.features.syncstatus.syncNeedsParent
import com.smartfarm.mobile.features.syncstatus.canRetryPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncQueuePresentationTest {
    private fun operation(id: String, farm: String, user: String,
        status: String = "pending", error: String? = null) =
        PendingOperation(id, user, farm, "harvest", "{}", 10L, status, error)
    private fun photo(id: String, farm: String, user: String,
        observation: String? = null, status: String = "pending", error: String? = null) =
        PendingPhoto(id, user, farm, "crop", observation, "parent",
            "image/jpeg", 10L, status, error)

    @Test fun summariesScopeByFarmAndSignedInAccount() {
        val ops = listOf(
            operation("a", "farm1", "alice"),
            operation("b", "farm1", "alice", "review", "Stock changed"),
            operation("c", "farm1", "bob", "review"),
            operation("d", "farm2", "alice"))
        val photos = listOf(photo("p", "farm1", "alice"), photo("q", "farm2", "bob"))
        val farm = syncQueueSummary(ops, photos, "farm1", "alice")
        assertEquals(3, farm.total)
        assertEquals(2, farm.waiting)
        assertEquals(1, farm.needsReview)
        assertEquals(1, farm.photos)
        assertEquals(4, syncQueueSummary(ops, photos, userId = "alice").total)
    }
    @Test fun errorsAreAlwaysVisibleAsReviewEvenIfStatusIsPending() {
        assertEquals(SyncRecordState.WAITING, syncState("pending", null))
        assertEquals(SyncRecordState.NEEDS_REVIEW, syncState("review", null))
        assertEquals(SyncRecordState.NEEDS_REVIEW, syncState("pending", "Rejected"))
        assertEquals(SyncRecordState.NEEDS_REVIEW, syncState("failed", null))
    }
    @Test fun sendingRequiresAnActiveTransferAndNeverHidesReview() {
        assertEquals(SyncRecordState.SENDING, syncState("pending", null, sending = true))
        assertEquals(SyncRecordState.WAITING, syncState("pending", null, sending = false))
        assertEquals(SyncRecordState.NEEDS_REVIEW, syncState("review", null, sending = true))
        assertEquals(SyncRecordState.NEEDS_REVIEW, syncState("pending", "Rejected", sending = true))
    }
    @Test fun dependentPhotosAreNeverBlindlyRetried() {
        val dependent = photo("a", "f", "u", null, "review")
        val linked = photo("b", "f", "u", "observation", "review")
        assertTrue(syncNeedsParent(dependent))
        assertFalse(canRetryPhoto(dependent))
        assertFalse(syncNeedsParent(linked))
        assertTrue(canRetryPhoto(linked))
        assertFalse(canRetryPhoto(linked.copy(status = "pending")))
    }
    @Test fun labelsExplainTheOperation() {
        assertEquals("Harvest", syncRecordType("harvest"))
        assertEquals("Crop inspection", syncRecordType("observation"))
        assertEquals("Field activity with supplies", syncRecordType("activity_stock"))
    }
}
