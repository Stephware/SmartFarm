package com.smartfarm.mobile.data.sync

import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.dto.ObservationPhotoDto
import com.smartfarm.mobile.data.remote.dto.SyncWriteResponse
import java.util.UUID

/** Never remove queued changes unless their server receipts identify those changes. */
internal object SyncReceiptValidator {
    fun operationMatches(response: SyncWriteResponse, operationId: String, type: String): Boolean =
        response.operationId.equals(operationId, ignoreCase = true) &&
            response.operationType == type

    fun observationId(response: SyncWriteResponse): String? = runCatching {
        UUID.fromString(response.result.get("id")?.asString).toString()
    }.getOrNull()

    fun photoMatches(response: ObservationPhotoDto, photo: PendingPhoto): Boolean =
        response.id.equals(photo.photoId, ignoreCase = true) &&
            response.observationId.equals(photo.observationId, ignoreCase = true)
}
