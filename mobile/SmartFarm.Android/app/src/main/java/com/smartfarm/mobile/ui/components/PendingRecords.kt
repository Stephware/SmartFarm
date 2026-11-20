package com.smartfarm.mobile.ui.components

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import com.smartfarm.mobile.data.sync.SyncProgress
import com.google.gson.JsonParser
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.features.syncstatus.SyncRecordState
import com.smartfarm.mobile.features.syncstatus.syncState

val LocalSyncProgress = staticCompositionLocalOf { SyncProgress() }

fun pendingRecordDescription(operation: PendingOperation): String = try {
    val json = JsonParser.parseString(operation.payloadJson).asJsonObject
    when (operation.operationType) {
        "harvest" -> uiText(R.string.ui_harvest_quantity_4b53f38, "Harvest quantity: ") + json.getAsJsonObject("harvest").get("quantity").asString
        "inventory" -> uiText(R.string.ui_stock_quantity_change_3036cc0, "Stock quantity change: ") + json.getAsJsonObject("movement").get("quantityChange").asString
        "activity_stock" -> uiText(R.string.ui_field_activity_supply_used_919108d, "Field activity + supply used: ") +
            json.getAsJsonObject("activityStock").get("quantityUsed").asString +
            uiText(R.string.ui_both_save_together_after_sync_e32f89f, " (both save together after sync)")
        "activity" -> listOf(uiText(R.string.ui_watering_2ee1114, "Watering"), uiText(R.string.ui_fertilizing_3cc00f9, "Fertilizing"), uiText(R.string.ui_pesticide_3e04b39, "Pesticide"), uiText(R.string.ui_weeding_4fbeba9, "Weeding"), uiText(R.string.ui_other_f97e9da, "Other"))
            .getOrElse(json.getAsJsonObject("activity").get("activityType").asInt - 1) { uiText(R.string.ui_activity_38da150, "Activity") }
        "observation" -> uiText(R.string.ui_observation_c24d198, "Observation: ") + listOf(uiText(R.string.ui_not_assessed_025761a, "Not assessed"), uiText(R.string.ui_healthy_7f1e323, "Healthy"), uiText(R.string.ui_needs_attention_c1ebc78, "Needs attention"), uiText(R.string.ui_critical_427dd29, "Critical"))
            .getOrElse(json.getAsJsonObject("observation").get("healthStatus").asInt) { uiText(R.string.ui_not_assessed_025761a, "Not assessed") }
        else -> uiText(R.string.ui_saved_local_record_721fe6a, "Saved local record")
    }
} catch (_: Exception) { uiText(R.string.ui_saved_local_record_721fe6a, "Saved local record") }

@Composable
fun PendingRecords(operations: List<PendingOperation>, farmId: String, targetId: String,
    types: Set<String>, onSync: () -> Unit) {
    val pending = operations.filter { operation ->
        operation.farmId == farmId && operation.operationType in types && runCatching {
            JsonParser.parseString(operation.payloadJson).asJsonObject.get("targetId").asString == targetId
        }.getOrDefault(false)
    }
    if (pending.isEmpty()) return
    val progress = LocalSyncProgress.current
    val needsReview = pending.any { syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW }
    FarmSectionCard(
        uiText(R.string.ui_saved_on_phone_a8ae994, "Saved on phone"),
        subtitle = uiText(R.string.ui_these_records_are_not_server_confirmed_and_are_not_i_d6631c1, "These records are not server-confirmed and are not included in the totals above.")
    ) {
        pending.forEach { operation ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val transfer = syncState(operation.status, operation.errorMessage,
                    progress.userId == operation.userId && progress.sendingId == operation.operationId)
                FarmStatusChip(
                    when (transfer) { SyncRecordState.NEEDS_REVIEW -> uiText(R.string.ui_needs_review_07297fa, "Needs review"); SyncRecordState.SENDING -> uiText(R.string.ui_sending_e595f17, "Sending"); else -> uiText(R.string.ui_saved_on_phone_a8ae994, "Saved on phone") },
                    if (transfer == SyncRecordState.NEEDS_REVIEW)
                        FarmStatusTone.Warning else FarmStatusTone.Planned,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                Text(pendingRecordDescription(operation), style = MaterialTheme.typography.bodyMedium)
                operation.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (needsReview || progress.signInRequired) {
            if (progress.signInRequired) Text(uiText(R.string.ui_sign_in_again_to_send_your_saved_work_cd65708, "Sign in again to send your saved work."))
            OutlinedButton(onClick = onSync, modifier = Modifier.fillMaxWidth()) {
                Text(if (needsReview) uiText(R.string.ui_review_saved_work_2165dad, "Review saved work") else uiText(R.string.ui_open_sync_status_6ec1bcf, "Open sync status"))
            }
        } else Text(uiText(R.string.ui_uploads_automatically_when_a_connection_is_available_30fa19b, "Uploads automatically when a connection is available."))
    }
}
