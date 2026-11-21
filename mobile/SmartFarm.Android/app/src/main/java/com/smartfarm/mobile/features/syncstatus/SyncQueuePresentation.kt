package com.smartfarm.mobile.features.syncstatus

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto

/** Queued rows have not been server-confirmed. Confirmed rows leave the Room queue. */
enum class SyncRecordState { WAITING, SENDING, NEEDS_REVIEW }

fun syncState(status: String, error: String?, sending: Boolean = false): SyncRecordState =
    if (status != "pending" || !error.isNullOrBlank()) SyncRecordState.NEEDS_REVIEW
    else if (sending) SyncRecordState.SENDING else SyncRecordState.WAITING

data class SyncQueueSummary(
    val waiting: Int = 0, val needsReview: Int = 0,
    val photos: Int = 0, val total: Int = waiting + needsReview
)

fun syncQueueSummary(
    operations: List<PendingOperation>, photos: List<PendingPhoto>,
    farmId: String? = null, userId: String? = null
): SyncQueueSummary {
    val ops = operations.filter { (farmId == null || it.farmId == farmId) &&
        (userId == null || it.userId == userId) }
    val images = photos.filter { (farmId == null || it.farmId == farmId) &&
        (userId == null || it.userId == userId) }
    val needs = ops.count { syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW } +
        images.count { syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW }
    return SyncQueueSummary(
        waiting = ops.size + images.size - needs,
        needsReview = needs,
        photos = images.size
    )
}

fun syncRecordType(type: String): String = when (type) {
    "activity" -> uiText(R.string.ui_field_activity_a50009c, "Field activity")
    "activity_stock" -> uiText(R.string.ui_field_activity_with_supplies_e9f634d, "Field activity with supplies")
    "observation" -> uiText(R.string.ui_crop_inspection_4a519db, "Crop inspection")
    "harvest" -> uiText(R.string.ui_harvest_c91ab8b, "Harvest")
    "inventory" -> uiText(R.string.ui_stock_movement_c93f4ce, "Stock movement")
    else -> uiText(R.string.ui_saved_field_record_f86b9ef, "Saved field record")
}

fun syncNeedsParent(photo: PendingPhoto): Boolean =
    photo.observationId == null && photo.parentOperationId != null

/** No automatic retry while a photo still depends on an unconfirmed inspection. */
fun canRetryPhoto(photo: PendingPhoto): Boolean =
    syncState(photo.status, photo.errorMessage) == SyncRecordState.NEEDS_REVIEW &&
        photo.observationId != null
