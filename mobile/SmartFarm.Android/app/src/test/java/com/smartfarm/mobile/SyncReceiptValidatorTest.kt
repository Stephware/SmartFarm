package com.smartfarm.mobile

import com.google.gson.JsonObject
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.dto.ObservationPhotoDto
import com.smartfarm.mobile.data.remote.dto.SyncWriteResponse
import com.smartfarm.mobile.data.remote.dto.ActivityStockInputDto
import com.smartfarm.mobile.data.remote.dto.ActivityInputDto
import java.math.BigDecimal
import com.smartfarm.mobile.data.sync.SyncReceiptValidator
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncReceiptValidatorTest {
    private val operationId = UUID.randomUUID().toString()
    private val observationId = UUID.randomUUID().toString()
    private val photoId = UUID.randomUUID().toString()

    private fun receipt(id: String = operationId, type: String = "observation",
        result: JsonObject = JsonObject().apply { addProperty("id", observationId) }) =
        SyncWriteResponse(id, type, result, false)

    @Test fun onlyMatchingOperationReceiptMayClearPendingWrite() {
        assertTrue(SyncReceiptValidator.operationMatches(
            receipt(), operationId, "observation"))
        assertFalse(SyncReceiptValidator.operationMatches(
            receipt(UUID.randomUUID().toString()), operationId, "observation"))
        assertFalse(SyncReceiptValidator.operationMatches(
            receipt(type = "harvest"), operationId, "observation"))
    }

    @Test fun malformedObservationReceiptKeepsOperationForReview() {
        assertEquals(observationId, SyncReceiptValidator.observationId(receipt()))
        assertNull(SyncReceiptValidator.observationId(receipt(result = JsonObject())))
        assertNull(SyncReceiptValidator.observationId(
            receipt(result = JsonObject().apply { addProperty("id", "not-a-guid") })))
    }

    @Test fun combinedFieldAndStockReceiptRequiresTwoMatchingIds() {
        val activityId = UUID.randomUUID().toString()
        val itemId = UUID.randomUUID().toString()
        val input = ActivityStockInputDto(ActivityInputDto(2, "Applied fertilizer"),
            itemId, BigDecimal("1.500"), "stock-row")
        val valid = JsonObject().apply {
            add("activity", JsonObject().apply { addProperty("id", activityId) })
            add("inventory", JsonObject().apply { addProperty("id", itemId) })
        }
        assertTrue(SyncReceiptValidator.activityStockMatches(
            receipt(type = "activity_stock", result = valid), input))
        assertFalse(SyncReceiptValidator.activityStockMatches(
            receipt(type = "activity_stock", result = valid), input.copy(
                inventoryItemId = UUID.randomUUID().toString())))
        assertFalse(SyncReceiptValidator.activityStockMatches(
            receipt(type = "activity_stock", result = JsonObject()), input))
        val corrupted = JsonObject().apply {
            add("activity", JsonObject().apply { addProperty("id", "not-a-guid") })
            add("inventory", JsonObject().apply { addProperty("id", itemId) })
        }
        assertFalse(SyncReceiptValidator.activityStockMatches(
            receipt(type = "activity_stock", result = corrupted), input))
        assertFalse(SyncReceiptValidator.activityStockMatches(
            receipt(type = "activity_stock", result = valid), null))
    }

    @Test fun onlyMatchingPhotoReceiptMayRemoveLocalPhoto() {
        val photo = PendingPhoto(photoId, "account-a", "farm-a", "cycle-a",
            observationId, null, "image/jpeg", 1L)
        fun saved(id: String = photoId, observation: String = observationId) =
            ObservationPhotoDto(id, observation, "image/jpeg", 10L, "2026-11-10T00:00:00Z")

        assertTrue(SyncReceiptValidator.photoMatches(saved(), photo))
        assertFalse(SyncReceiptValidator.photoMatches(
            saved(id = UUID.randomUUID().toString()), photo))
        assertFalse(SyncReceiptValidator.photoMatches(
            saved(observation = UUID.randomUUID().toString()), photo))
    }
}
