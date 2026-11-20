package com.smartfarm.mobile.features.syncstatus

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.sync.StockRetryPreview
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.pendingRecordDescription
import com.smartfarm.mobile.ui.components.LocalSyncProgress
import com.smartfarm.mobile.ui.components.farmDateTime

/**
 * Work remains on this device until a validated server receipt removes it from Room.
 * Do not claim an operation is uploaded merely because WorkManager was scheduled.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SyncStatusScreen(
    operations: List<PendingOperation>,
    photos: List<PendingPhoto>,
    stockPreview: StockRetryPreview?,
    error: String?,
    onBack: () -> Unit,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    onRetryPhoto: (String) -> Unit,
    onDiscardPhoto: (String) -> Unit,
    onReviewStock: (String) -> Unit,
    onApproveStock: () -> Unit,
    onCancelStock: () -> Unit,
    onSyncNow: () -> Unit,
    currentFarmId: String? = null,
    farmNames: Map<String, String> = emptyMap(),
    farmTimeZones: Map<String, String> = emptyMap(),
    notice: String? = null,
    farmTimeZone: String? = null,
    onAccount: (() -> Unit)? = null
) {
    var discard by rememberSaveable { mutableStateOf<String?>(null) }
    var discardPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var allFarms by rememberSaveable(currentFarmId) { mutableStateOf(currentFarmId.isNullOrBlank()) }
    var statusFilter by rememberSaveable { mutableStateOf("all") }
    val farmId = if (allFarms) null else currentFarmId
    val shownOperations = operations.filter { farmId == null || it.farmId == farmId }
        .filter { row ->
            statusFilter == "all" ||
                (syncState(row.status, row.errorMessage) == SyncRecordState.NEEDS_REVIEW) ==
                    (statusFilter == "review")
        }.sortedWith(compareByDescending<PendingOperation> {
            syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW
        }.thenByDescending { it.createdAtUtc })
    val shownPhotos = photos.filter { farmId == null || it.farmId == farmId }
        .filter { photo ->
            statusFilter == "all" ||
                (syncState(photo.status, photo.errorMessage) == SyncRecordState.NEEDS_REVIEW) ==
                    (statusFilter == "review")
        }.sortedWith(compareByDescending<PendingPhoto> {
            syncState(it.status, it.errorMessage) == SyncRecordState.NEEDS_REVIEW
        }.thenByDescending { it.savedAtUtcMillis })
    val summary = syncQueueSummary(operations, photos, farmId)
    val global = syncQueueSummary(operations, photos)
    val progress = LocalSyncProgress.current
    val confirmed = progress.confirmedFarms.any { (farm, count) -> count > 0 && (farmId == null || farm == farmId) }
    fun farmLabel(id: String): String = farmNames[id] ?: uiText(R.string.ui_farm_1_s_32a0759, "Farm %1\$s", (id.take(8)))

    RecordPage(title = uiText(R.string.ui_offline_sync_843fdb9, "Offline sync"), onBack = onBack) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                FarmHero(uiText(R.string.ui_offline_sync_843fdb9, "Offline sync"), uiText(R.string.ui_records_saved_on_this_phone_b1f0066, "Records saved on this phone."))
                FarmSectionCard(uiText(R.string.ui_sync_queue_39c8598, "Sync queue"), subtitle = uiText(R.string.ui_waiting_for_server_confirmation_791ceb0, "Waiting for server confirmation.")) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FarmStatusChip(uiText(R.string.ui_pending_1_s_9a00006, "Pending: %1\$s", (summary.waiting)), FarmStatusTone.Planned)
                        FarmStatusChip(uiText(R.string.ui_needs_review_1_s_38d2e3f, "Needs review: %1\$s", (summary.needsReview)),
                            if (summary.needsReview > 0) FarmStatusTone.Warning
                            else FarmStatusTone.Neutral)
                    }
                    Text(uiText(R.string.ui_1_s_local_2_s_on_this_view_2a87990, "%1\$s local %2\$s on this view", (summary.total), (if (summary.total == 1) "record" else "records")),
                        style = MaterialTheme.typography.bodyMedium)
                    if (global.total > summary.total) {
                        Text(uiText(R.string.ui_1_s_more_saved_in_other_farms_d6e246b, "%1\$s more saved in other farms.", (global.total - summary.total)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (confirmed) {
                    FarmStatusChip(uiText(R.string.ui_sent_c16bc82, "Sent"), FarmStatusTone.Active)
                    Text(uiText(R.string.ui_server_confirmed_work_is_available_in_farm_history_864ee72, "Server-confirmed work is available in farm history."),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (progress.signInRequired) {
                    Text(uiText(R.string.ui_sign_in_again_to_send_your_saved_work_cd65708, "Sign in again to send your saved work."),
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (onAccount != null) OutlinedButton(onClick = onAccount) { Text(uiText(R.string.ui_open_account_81046a4, "Open account")) }
                }
                if (currentFarmId != null) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = !allFarms, onClick = { allFarms = false },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = { Text(uiText(R.string.ui_this_farm_37bc053, "This farm")) })
                        FilterChip(selected = allFarms, onClick = { allFarms = true },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = { Text(uiText(R.string.ui_all_my_farms_a091ca9, "All my farms")) })
                    }
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to uiText(R.string.ui_all_a52ace4, "All"), "review" to uiText(R.string.ui_needs_review_07297fa, "Needs review"),
                        "waiting" to uiText(R.string.ui_waiting_6e293a8, "Waiting")).forEach { (key, label) ->
                        FilterChip(selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = { Text(label) })
                    }
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                notice?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                Button(onClick = onSyncNow,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                    Text(uiText(R.string.ui_sync_now_885e8f4, "Sync now"))
                }
                if (shownOperations.isEmpty() && shownPhotos.isEmpty()) {
                    FarmEmptyState(
                        if (statusFilter == "review") uiText(R.string.ui_no_records_need_review_055313c, "No records need review")
                        else if (statusFilter == "waiting") uiText(R.string.ui_no_waiting_records_f4cc2f4, "No waiting records")
                        else if (summary.total == 0) uiText(R.string.ui_no_records_waiting_on_this_phone_f0acb5c, "No records waiting on this phone")
                        else uiText(R.string.ui_no_matching_records_4f36612, "No matching records"),
                        if (summary.total == 0)
                            uiText(R.string.ui_previously_confirmed_work_is_available_in_its_farm_h_98f5120, "Previously confirmed work is available in its farm history.")
                        else uiText(R.string.ui_choose_another_filter_or_view_all_farms_to_find_save_910e8ca, "Choose another filter or view all farms to find saved records.")
                    )
                }
                if (summary.total > 0) {
                    Text(uiText(R.string.ui_records_are_confirmed_only_after_server_acceptance_p_ac07c1c, "Records are confirmed only after server acceptance. Pending work stays on this phone."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (operations.size >= 100 || photos.size >= 100) {
                    Text(uiText(R.string.ui_showing_the_most_recent_local_entries_older_records__2c28ebd, "Showing the most recent local entries. Older records are kept on this device."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (shownOperations.isNotEmpty()) item(key = "records-heading") {
            FarmSectionHeader(uiText(R.string.ui_field_records_3048ebd, "Field records"), uiText(R.string.ui_needs_review_entries_appear_first_cc1b43f, "Needs-review entries appear first."))
        }
        items(shownOperations, key = { "operation-${it.operationId}" }) { operation ->
            val transfer = syncState(operation.status, operation.errorMessage,
                progress.userId == operation.userId && progress.sendingId == operation.operationId)
            val needs = transfer == SyncRecordState.NEEDS_REVIEW
            val isStock = operation.operationType in setOf("inventory", "activity_stock")
            FarmSectionCard(syncRecordType(operation.operationType),
                subtitle = "${farmLabel(operation.farmId)} · " +
                    farmDateTime(java.time.Instant.ofEpochMilli(operation.createdAtUtc).toString(),
                        farmTimeZones[operation.farmId] ?: farmTimeZone)) {
                FarmStatusChip(if (needs) uiText(R.string.ui_needs_review_07297fa, "Needs review") else if (transfer == SyncRecordState.SENDING) uiText(R.string.ui_sending_e595f17, "Sending") else uiText(R.string.ui_saved_on_phone_a8ae994, "Saved on phone"),
                    if (needs) FarmStatusTone.Warning else FarmStatusTone.Planned,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(pendingRecordDescription(operation))
                if (needs) {
                    operation.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    }
                    Text(if (isStock)
                        uiText(R.string.ui_stock_may_have_changed_check_its_latest_balance_befo_0767216, "Stock may have changed. Check its latest balance before retrying.")
                        else uiText(R.string.ui_check_the_record_and_message_before_retrying_13dd628, "Check the record and message before retrying."),
                        style = MaterialTheme.typography.bodySmall)
                    if (operation.status == "review") {
                        if (isStock) {
                            OutlinedButton(onClick = { onReviewStock(operation.operationId) },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text(uiText(R.string.ui_check_latest_stock_b629f41, "Check latest stock"))
                            }
                        } else {
                            OutlinedButton(onClick = { onRetry(operation.operationId) },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                                Text(uiText(R.string.ui_retry_this_change_200e584, "Retry this change"))
                            }
                        }
                    }
                    TextButton(onClick = { discard = operation.operationId }) {
                        Text(uiText(R.string.ui_discard_and_re_enter_b5661e4, "Discard and re-enter"))
                    }
                }
            }
        }
        if (shownPhotos.isNotEmpty()) item(key = "photos-heading") {
            FarmSectionHeader(uiText(R.string.ui_crop_photos_294ed7b, "Crop photos"), uiText(R.string.ui_photos_upload_after_their_linked_inspections_9f4cf79, "Photos upload after their linked inspections."))
        }
        items(shownPhotos, key = { "photo-${it.photoId}" }) { photo ->
            val transfer = syncState(photo.status, photo.errorMessage,
                progress.userId == photo.userId && progress.sendingId == photo.photoId)
            val needs = transfer == SyncRecordState.NEEDS_REVIEW
            FarmSectionCard(uiText(R.string.ui_inspection_photo_4c31148, "Inspection photo"),
                subtitle = "${farmLabel(photo.farmId)} · " +
                    farmDateTime(java.time.Instant.ofEpochMilli(photo.savedAtUtcMillis).toString(),
                        farmTimeZones[photo.farmId] ?: farmTimeZone)) {
                FarmStatusChip(if (needs) uiText(R.string.ui_needs_review_07297fa, "Needs review") else if (transfer == SyncRecordState.SENDING) uiText(R.string.ui_sending_e595f17, "Sending") else uiText(R.string.ui_saved_on_phone_a8ae994, "Saved on phone"),
                    if (needs) FarmStatusTone.Warning else FarmStatusTone.Planned,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                if (photo.observationId == null) {
                    Text(uiText(R.string.ui_waiting_for_its_inspection_to_be_accepted_before_upl_1f40fdc, "Waiting for its inspection to be accepted before uploading the photo."),
                        style = MaterialTheme.typography.bodySmall)
                }
                if (needs) {
                    photo.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    }
                    if (canRetryPhoto(photo)) {
                        OutlinedButton(onClick = { onRetryPhoto(photo.photoId) },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                            Text(uiText(R.string.ui_retry_photo_22a7a49, "Retry photo"))
                        }
                    } else if (syncNeedsParent(photo)) {
                        Text(uiText(R.string.ui_review_the_inspection_entry_above_first_retrying_thi_3452d13, "Review the inspection entry above first. Retrying this photo alone cannot finish it."),
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { discardPhoto = photo.photoId }) {
                        Text(uiText(R.string.ui_discard_local_photo_7559db3, "Discard local photo"))
                    }
                }
            }
        }
    }

    stockPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = onCancelStock,
            title = { Text(uiText(R.string.ui_confirm_stock_retry_b6ec8b0, "Confirm stock retry")) },
            text = {
                Text(uiText(R.string.ui_1_s_ncurrent_stock_2_s_n_16b768e, "%1\$s\nCurrent stock: %2\$s\n", (preview.itemName), (preview.currentBalance)) +
                    uiText(R.string.ui_queued_change_1_s_n_986c73a, "Queued change: %1\$s\n", (preview.change)) +
                    uiText(R.string.ui_stock_after_change_1_s_n_ff166e2, "Stock after change: %1\$s\n", (preview.expectedBalance)) +
                    (if (preview.includesFieldActivity)
                        uiText(R.string.ui_the_field_activity_and_supply_consumption_will_be_sa_8a76112, "The field activity and supply consumption will be saved together. ")
                    else "") +
                    uiText(R.string.ui_only_retry_if_this_movement_still_reflects_actual_fa_16cd94e, "Only retry if this movement still reflects actual farm work."))
            },
            confirmButton = { TextButton(onClick = onApproveStock) { Text(uiText(R.string.ui_approve_and_retry_eb69ed2, "Approve and retry")) } },
            dismissButton = { TextButton(onClick = onCancelStock) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) } }
        )
    }
    discardPhoto?.let { photoId ->
        AlertDialog(onDismissRequest = { discardPhoto = null },
            title = { Text(uiText(R.string.ui_discard_offline_photo_40c0e83, "Discard offline photo?")) },
            text = { Text(uiText(R.string.ui_the_photo_file_will_be_permanently_deleted_from_this_58b4ffb, "The photo file will be permanently deleted from this phone. This cannot be undone.")) },
            confirmButton = { TextButton(onClick = {
                onDiscardPhoto(photoId); discardPhoto = null
            }) { Text(uiText(R.string.ui_discard_photo_95943ad, "Discard photo")) } },
            dismissButton = { TextButton(onClick = { discardPhoto = null }) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) } })
    }
    discard?.let { operationId ->
        AlertDialog(onDismissRequest = { discard = null },
            title = { Text(uiText(R.string.ui_discard_unsynced_change_56fa99e, "Discard unsynced change?")) },
            text = { Text(uiText(R.string.ui_this_saved_local_record_will_be_permanently_deleted__72e3313, "This saved local record will be permanently deleted. Linked unsynced photos may also be removed. This cannot be undone.")) },
            confirmButton = { TextButton(onClick = {
                onDiscard(operationId); discard = null
            }) { Text(uiText(R.string.ui_discard_eb1a70e, "Discard")) } },
            dismissButton = { TextButton(onClick = { discard = null }) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) } })
    }
}
