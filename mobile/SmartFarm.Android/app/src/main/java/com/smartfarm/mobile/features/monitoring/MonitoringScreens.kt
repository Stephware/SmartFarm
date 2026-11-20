package com.smartfarm.mobile.features.monitoring

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.ui.components.UnsavedChanges
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.UUID
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.dto.ObservationDto
import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import java.math.BigDecimal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.ui.components.OfflineDataNotice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmEmptyState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import com.smartfarm.mobile.ui.components.GlidePhoto

@Composable
fun MonitoringScreen(
    state: MonitoringUiState,
    onBack: () -> Unit,
    onActivity: (Int, String) -> Unit,
    onObservation: (Int, String, String, Uri?) -> Unit,
    onUpload: (String, Uri) -> Unit,
    onPreview: (String) -> Unit,
    onThumbnail: suspend (String) -> ByteArray,
    onDismissPreview: () -> Unit,
    pending: List<PendingOperation> = emptyList(),
    onSync: () -> Unit = {},
    onRetry: () -> Unit = {},
    quick: String = "",
    cropLabel: String? = null, linkedTask: Boolean = false,
    onIssueObservation: (Int, String, String, Int, Int, Uri?) -> Unit = { _, _, _, _, _, _ -> },
    canManageIssues: Boolean = false,
    taskOptions: List<TaskDto> = emptyList(),
    onFollowUp: (ObservationDto, Int, String?) -> Unit = { _, _, _ -> },
    onActivityWithStock: (Int, String, InventoryItemDto, String) -> Unit = { _, _, _, _ -> }
,
    farmTimeZone: String? = null) {
    // A quick link selects one form; saving still uses the existing offline queue.
    var focus by rememberSaveable(state.farmId, state.cycleId, quick) {
        mutableStateOf(if (quick == "inspection") "inspection" else "activity")
    }
    var historyTab by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("activities") }
    var moreActivities by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(false) }
    var showGrowthStage by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(false) }
    var activityType by rememberSaveable(state.farmId, state.cycleId, quick) {
        mutableStateOf(when (quick) { "fertilizing" -> 2; "other" -> 5; else -> 1 })
    }
    var activityNotes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var useStock by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(false) }
    var supplyId by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    var stockQuantity by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var health by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(0) }
    var issueCategory by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(0) }
    var issueSeverity by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(0) }
    var stage by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var observationNotes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var targetObservation by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    var newPhoto by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<Uri?>(null) }
    var cameraPath by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    var viewerPhotoId by rememberSaveable(state.farmId, state.cycleId) {
        mutableStateOf<String?>(null)
    }
    fun openPhoto(photoId: String) {
        viewerPhotoId = photoId
        onPreview(photoId)
    }
    val activityReceipt = state.saveReceipts["${state.farmId}/${state.cycleId}/activity"]
    val stockReceipt = state.saveReceipts["${state.farmId}/${state.cycleId}/activity_stock"]
    val observationReceipt = state.saveReceipts["${state.farmId}/${state.cycleId}/observation"]
    var handledActivity by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(activityReceipt) }
    var handledObservation by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(observationReceipt) }
    var handledStockReceipt by rememberSaveable(state.farmId, state.cycleId) {
        mutableStateOf(stockReceipt)
    }
    LaunchedEffect(activityReceipt) {
        if (activityReceipt != null && activityReceipt != handledActivity) {
            activityNotes = ""
            activityType = when (quick) { "fertilizing" -> 2; "other" -> 5; else -> 1 }
            handledActivity = activityReceipt
        }
    }
    LaunchedEffect(stockReceipt) {
        if (stockReceipt != null && stockReceipt != handledStockReceipt) {
            activityNotes = ""
            stockQuantity = ""
            useStock = false
            activityType = when (quick) { "fertilizing" -> 2; "other" -> 5; else -> 1 }
            handledStockReceipt = stockReceipt
        }
    }
    LaunchedEffect(observationReceipt) {
        if (observationReceipt != null && observationReceipt != handledObservation) {
            newPhoto = null; observationNotes = ""; stage = ""; health = 0
            issueCategory = 0; issueSeverity = 0
            handledObservation = observationReceipt
        }
    }
    UnsavedChanges("activity", activityNotes.isNotEmpty() || useStock || stockQuantity.isNotEmpty() ||
        activityType != when (quick) { "fertilizing" -> 2; "other" -> 5; else -> 1 },
        activityNotes, useStock.toString(), stockQuantity, activityType.toString(), supplyId.orEmpty())
    UnsavedChanges("inspection", observationNotes.isNotEmpty() || stage.isNotEmpty() || newPhoto != null ||
        health != 0 || issueCategory != 0 || issueSeverity != 0,
        observationNotes, stage, newPhoto?.toString().orEmpty(), health.toString(), issueCategory.toString(), issueSeverity.toString())
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val target = targetObservation
            if (target != null) onUpload(target, uri) else newPhoto = uri
        }
        targetObservation = null
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) {
            val target = targetObservation
            if (target != null) onUpload(target, uri) else newPhoto = uri
        } else {
            cameraPath?.let { File(it).delete() }
        }
        targetObservation = null
        cameraUri = null
    }
    fun capture(target: String?) {
        val folder = File(context.filesDir, "camera").apply { mkdirs() }
        val file = File(folder, "capture-${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
        cameraPath = file.absolutePath
        cameraUri = uri
        targetObservation = target
        camera.launch(uri)
    }

    val progressPhotos = state.observations
        .flatMap { observation -> observation.photos.map { observation to it } }
        .sortedWith(compareByDescending<Pair<ObservationDto, com.smartfarm.mobile.data.remote.dto.ObservationPhotoDto>> {
            it.first.observedAtUtc
        }.thenByDescending { it.second.uploadedAtUtc })

    RecordPage(title = uiText(R.string.ui_crop_monitoring_d14cf4e, "Crop monitoring"), onBack = onBack) {
        item(key = "forms") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            FarmHero(when (quick) {
                "watering" -> uiText(R.string.ui_record_watering_d15d0cc, "Record watering"); "fertilizing" -> uiText(R.string.ui_apply_fertilizer_7d3967d, "Apply fertilizer")
                "inspection" -> uiText(R.string.ui_inspect_crop_a790b90, "Inspect crop"); "other" -> uiText(R.string.ui_other_fieldwork_f4c7adc, "Other fieldwork"); else -> uiText(R.string.ui_crop_monitoring_d14cf4e, "Crop monitoring")
            }, cropLabel ?: uiText(R.string.ui_selected_crop_record_activities_and_check_history_55260d4, "Selected crop · Record activities and check history."))
            if (linkedTask) Text(uiText(R.string.ui_linked_to_your_selected_task_completion_is_confirmed_498a0cb, "Linked to your selected task. Completion is confirmed only after sending."),
                style = MaterialTheme.typography.bodyMedium)
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text(uiText(R.string.ui_try_again_d8b8392, "Try again")) }
            }
            state.notice?.let { Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("activity" to uiText(R.string.ui_activity_38da150, "Activity"), "inspection" to uiText(R.string.ui_inspection_6e4fa13, "Inspection"),
                    "history" to uiText(R.string.ui_history_0e76960, "History")).forEach { (key, label) ->
                    FilterChip(selected = focus == key, onClick = { focus = key },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        label = { Text(label) })
                }
            }
            PendingRecords(pending, state.farmId, state.cycleId,
                setOf("activity", "activity_stock", "observation", "photo"), onSync)
            if (!state.canRecord) {
                FarmEmptyState(uiText(R.string.ui_read_only_crop_cycle_58b6ec6, "Read-only crop cycle"),
                    uiText(R.string.ui_new_field_records_require_an_active_crop_cycle_exist_a5a7440, "New field records require an active crop cycle. Existing history is still available."))
                OutlinedButton(onClick = { focus = "history" }, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_open_recorded_history_ae259e8, "Open recorded history"))
                }
            }

            if (state.canRecord && focus != "history") {
                Text(uiText(R.string.ui_saved_on_your_phone_first_sent_when_online_5610d4c, "Saved on your phone first; sent when online."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (focus == "activity") {
                FarmSectionCard(uiText(R.string.ui_record_farm_activity_efbf5c0, "Record farm activity"),
                    subtitle = uiText(R.string.ui_choose_an_activity_and_save_the_work_to_this_crop_cy_0d8782d, "Choose an activity and save the work to this crop cycle.")) {
                val activities = listOf(uiText(R.string.ui_watering_2ee1114, "Watering"), uiText(R.string.ui_fertilizing_3cc00f9, "Fertilizing"), uiText(R.string.ui_pesticide_3e04b39, "Pesticide"), uiText(R.string.ui_weeding_4fbeba9, "Weeding"), uiText(R.string.ui_other_f97e9da, "Other"))
                if (quick !in setOf("watering", "fertilizing")) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    activities.take(2).forEachIndexed { index, label ->
                        FilterChip(selected = activityType == index + 1,
                            onClick = { activityType = index + 1 }, enabled = !state.busy,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text(label) })
                    }
                }
                TextButton(onClick = { moreActivities = !moreActivities }, enabled = !state.busy) {
                    Text(if (moreActivities) uiText(R.string.ui_fewer_activity_types_fb32cd5, "Fewer activity types") else uiText(R.string.ui_other_activity_types_6ecb2ac, "Other activity types"))
                }
                if (moreActivities || activityType > 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            activities.drop(2).take(2).forEachIndexed { offset, label ->
                                FilterChip(selected = activityType == offset + 3,
                                    onClick = { activityType = offset + 3 }, enabled = !state.busy,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                    label = { Text(label) })
                            }
                        }
                        FilterChip(selected = activityType == 5,
                            onClick = { activityType = 5 }, enabled = !state.busy,
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                            label = { Text(uiText(R.string.ui_other_f97e9da, "Other")) })
                    }
                }
                } else {
                    Text(if (quick == "watering") uiText(R.string.ui_watering_2ee1114, "Watering") else uiText(R.string.ui_fertilizer_application_6910078, "Fertilizer application"),
                        style = MaterialTheme.typography.titleMedium)
                }
                // Editable note guides: never save guessed work or invent inspection findings.
                val noteGuide = when (activityType) {
                    1 -> uiText(R.string.ui_area_watered_amount_or_soil_condition_8f9a042, "Area watered, amount or soil condition")
                    2 -> uiText(R.string.ui_fertilizer_used_quantity_and_application_area_58969c1, "Fertilizer used, quantity and application area")
                    3 -> uiText(R.string.ui_product_used_quantity_and_treatment_area_1938dbc, "Product used, quantity and treatment area")
                    4 -> uiText(R.string.ui_area_weeded_and_observations_b9faa12, "Area weeded and observations")
                    else -> uiText(R.string.ui_work_performed_affected_area_and_observations_ed0c258, "Work performed, affected area and observations")
                }
                OutlinedTextField(activityNotes, { activityNotes = it }, enabled = !state.busy,
                    label = { Text(uiText(R.string.ui_notes_optional_6239b93, "Notes (optional)")) },
                    placeholder = { Text(noteGuide) }, modifier = Modifier.fillMaxWidth())
                val mayUseStock = activityType in 2..3
                if (mayUseStock) {
                    FilterChip(
                        selected = useStock,
                        onClick = { useStock = !useStock },
                        enabled = !state.busy,
                        label = { Text(uiText(R.string.ui_deduct_supply_from_farm_inventory_11eec48, "Deduct supply from farm inventory")) }
                    )
                    if (useStock) {
                        val category = if (activityType == 2) 2 else 3
                        val available = state.supplies.filter {
                            it.category == category && it.archivedAtUtc == null
                        }
                        var selectSupply by rememberSaveable(state.farmId, state.cycleId) {
                            mutableStateOf(false)
                        }
                        val selected = available.firstOrNull { it.id == supplyId }
                        state.supplyError?.let {
                            Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.suppliesOffline) {
                            Text(uiText(R.string.ui_using_saved_inventory_balances_the_server_will_check_9adcc61, "Using saved inventory balances. The server will check stock ") +
                                uiText(R.string.ui_and_may_require_review_when_syncing_31ae542, "and may require review when syncing."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (available.isEmpty()) {
                            FarmEmptyState(uiText(R.string.ui_no_matching_supplies_6fd89c6, "No matching supplies"),
                                uiText(R.string.ui_add_fertilizer_or_pesticide_inventory_before_recordi_d2ecc2e, "Add fertilizer or pesticide inventory before recording linked use."))
                        } else {
                            Text(uiText(R.string.ui_supply_to_consume_8034f68, "Supply to consume"), style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { selectSupply = true },
                                enabled = !state.busy) {
                                Text(selected?.let { "${it.name} · ${it.quantityOnHand} ${it.unit}" }
                                    ?: uiText(R.string.ui_choose_supply_775f2f6, "Choose supply"))
                            }
                            DropdownMenu(
                                expanded = selectSupply,
                                onDismissRequest = { selectSupply = false }
                            ) {
                                available.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.name} · ${item.quantityOnHand} ${item.unit}") },
                                        onClick = {
                                            supplyId = item.id
                                            stockQuantity = ""
                                            selectSupply = false
                                        }
                                    )
                                }
                            }
                            OutlinedTextField(stockQuantity, { stockQuantity = it },
                                label = { Text(uiText(R.string.ui_quantity_used_1_s_7f3de5d, "Quantity used (%1\$s)", (selected?.unit ?: "item unit"))) },
                                placeholder = { Text(uiText(R.string.ui_example_1_5_c9897d6, "Example: 1.5")) },
                                modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                                singleLine = true)
                            Text(uiText(R.string.ui_this_activity_and_its_stock_deduction_will_be_queued_4839875, "This activity and its stock deduction will be queued as one ") +
                                uiText(R.string.ui_change_the_balance_is_not_reduced_until_the_server_c_8d13e95, "change. The balance is not reduced until the server confirms it."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val selectedStock = state.supplies.firstOrNull {
                    it.id == supplyId &&
                        it.category == if (activityType == 2) 2 else 3
                }
                val amount = stockQuantity.trim().toBigDecimalOrNull()
                val needsStock = mayUseStock && useStock
                val validStock = !needsStock || (selectedStock != null &&
                    amount != null && amount > BigDecimal.ZERO &&
                    amount.scale() <= 3 && amount <= selectedStock.quantityOnHand)
                Button(onClick = {
                    if (needsStock) {
                        selectedStock?.let {
                            onActivityWithStock(activityType, activityNotes, it, stockQuantity)
                        }
                    } else onActivity(activityType, activityNotes)
                }, enabled = !state.busy && validStock,
                    modifier = Modifier.fillMaxWidth()) {
                    Text(if (needsStock) uiText(R.string.ui_save_activity_and_supply_use_725919b, "Save activity and supply use") else uiText(R.string.ui_save_activity_4dd5cbf, "Save activity"))
                }
                }
                }
                if (focus == "inspection") {
                FarmSectionCard(uiText(R.string.ui_record_inspection_observation_1f96e37, "Record inspection / observation"),
                    subtitle = uiText(R.string.ui_check_crop_condition_and_attach_a_photo_if_available_de73b58, "Check crop condition and attach a photo if available.")) {
                Text(uiText(R.string.ui_how_are_the_crops_looking_c1f7006, "How are the crops looking?"), style = MaterialTheme.typography.labelLarge)
                listOf(uiText(R.string.ui_not_sure_dca5fa4, "Not sure"), uiText(R.string.ui_healthy_7f1e323, "Healthy"), uiText(R.string.ui_needs_attention_c1ebc78, "Needs attention"), uiText(R.string.ui_critical_427dd29, "Critical"))
                    .chunked(2).forEachIndexed { rowIndex, options ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEachIndexed { index, label ->
                                val value = rowIndex * 2 + index
                                FilterChip(selected = health == value, onClick = {
                                    health = value
                                    if (value < 2) {
                                        issueCategory = 0
                                        issueSeverity = 0
                                    }
                                }, enabled = !state.busy,
                                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                    label = { Text(label) })
                            }
                        }
                    }
                if (health >= 2) {
                    Text(uiText(R.string.ui_what_did_you_notice_optional_4eda447, "What did you notice? (optional)"),
                        style = MaterialTheme.typography.labelLarge)
                    listOf(uiText(R.string.ui_none_dc937b5, "None"), uiText(R.string.ui_pest_d908813, "Pest"), uiText(R.string.ui_disease_7bce5b9, "Disease"), uiText(R.string.ui_nutrient_b814f65, "Nutrient"), uiText(R.string.ui_environment_9e47195, "Environment"), uiText(R.string.ui_other_f97e9da, "Other"))
                        .chunked(2).forEachIndexed { rowIndex, options ->
                            Row(Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                options.forEachIndexed { index, label ->
                                    val value = rowIndex * 2 + index
                                    FilterChip(selected = issueCategory == value, onClick = {
                                        issueCategory = value
                                        if (value == 0) issueSeverity = 0
                                    }, enabled = !state.busy,
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                        label = { Text(label) })
                                }
                            }
                        }
                }
                if (health >= 2 && issueCategory != 0) {
                    Text(uiText(R.string.ui_how_serious_is_it_7cbd47c, "How serious is it?"), style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(uiText(R.string.ui_low_f793de2, "Low"), uiText(R.string.ui_moderate_5c42afc, "Moderate"), uiText(R.string.ui_high_c4ebc6d, "High")).forEachIndexed { index, label ->
                            FilterChip(selected = issueSeverity == index + 1,
                                onClick = { issueSeverity = index + 1 }, enabled = !state.busy,
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                label = { Text(label) })
                        }
                    }
                    Text(uiText(R.string.ui_choose_needs_attention_or_critical_health_when_recor_a54bd44, "Choose Needs attention or Critical health when recording a specific issue. ") +
                        uiText(R.string.ui_descriptions_are_observations_not_confirmed_diagnose_19ea2f7, "Descriptions are observations, not confirmed diagnoses."),
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(observationNotes, { observationNotes = it }, enabled = !state.busy,
                    label = { Text(uiText(R.string.ui_inspection_notes_optional_aaaec7d, "Inspection notes (optional)")) },
                    placeholder = { Text(uiText(R.string.ui_describe_what_you_see_no_diagnosis_is_needed_e530cec, "Describe what you see; no diagnosis is needed")) },
                    modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showGrowthStage = !showGrowthStage }, enabled = !state.busy) {
                    Text(if (showGrowthStage) uiText(R.string.ui_hide_growth_stage_5171043, "Hide growth stage") else uiText(R.string.ui_add_growth_stage_optional_87d60a7, "Add growth stage (optional)"))
                }
                if (showGrowthStage) {
                    OutlinedTextField(stage, { stage = it }, enabled = !state.busy,
                        label = { Text(uiText(R.string.ui_growth_stage_906162c, "Growth stage")) }, placeholder = {
                            Text(uiText(R.string.ui_vegetative_flowering_fruiting_or_observed_stage_8f9cafb, "Vegetative, flowering, fruiting, or observed stage"))
                        }, modifier = Modifier.fillMaxWidth())
                }
                Text(uiText(R.string.ui_photo_optional_attach_it_to_this_inspection_in_one_s_769932d, "Photo (optional) — attach it to this inspection in one save."),
                    style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { capture(null) }, enabled = !state.busy,
                        modifier = Modifier.weight(1f)) { Text(uiText(R.string.ui_take_photo_7100ac9, "Take photo")) }
                    OutlinedButton(onClick = {
                        targetObservation = null
                        picker.launch("image/*")
                    }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                        Text(uiText(R.string.ui_choose_photo_9c5bb7a, "Choose photo"))
                    }
                }
                newPhoto?.let { photo ->
                    Text(uiText(R.string.ui_photo_selected_for_offline_upload_52b8bef, "Photo selected for offline upload"))
                    GlidePhoto(photo, uiText(R.string.ui_selected_crop_inspection_photo_dd285f5, "Selected crop inspection photo"),
                        Modifier.fillMaxWidth().height(220.dp))
                    OutlinedButton(onClick = { newPhoto = null }, enabled = !state.busy) { Text(uiText(R.string.ui_remove_selected_photo_f6ee0cf, "Remove selected photo")) }
                }
                val issueValid = issueCategory == 0 && issueSeverity == 0 ||
                    issueCategory in 1..5 && issueSeverity in 1..3 && health in 2..3
                Button(onClick = {
                    if (issueCategory == 0) onObservation(health, stage, observationNotes, newPhoto)
                    else onIssueObservation(health, stage, observationNotes,
                        issueCategory, issueSeverity, newPhoto)
                }, enabled = !state.busy && issueValid, modifier = Modifier.fillMaxWidth()) {
                    Text(uiText(R.string.ui_save_inspection_185b594, "Save inspection"))
                }
                }
                }
            }

            if (focus == "history") {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("activities" to uiText(R.string.ui_activities_4d0076e, "Activities"), "inspections" to uiText(R.string.ui_inspections_20cbe85, "Inspections"),
                        "photos" to uiText(R.string.ui_photos_5e3147a, "Photos")).forEach { (key, label) ->
                        FilterChip(selected = historyTab == key,
                            onClick = { historyTab = key },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text(label) })
                    }
                }
            }
            if (focus == "history" && historyTab == "activities") {
                FarmSectionHeader(uiText(R.string.ui_activity_history_e468751, "Activity history"),
                    uiText(R.string.ui_previous_fieldwork_records_for_this_crop_cycle_fcc870b, "Previous fieldwork records for this crop cycle."))
                if (state.activities.isEmpty())
                    FarmEmptyState(uiText(R.string.ui_no_activities_recorded_0aab895, "No activities recorded."), uiText(R.string.ui_saved_fieldwork_will_appear_here_a6dfc23, "Saved fieldwork will appear here."))
            }
            }
        }
        if (focus == "history" && historyTab == "activities") items(state.activities, key = { "activity-${it.id}" }) { activity ->
            val name = when (activity.activityType) {
                1 -> uiText(R.string.ui_watering_2ee1114, "Watering"); 2 -> uiText(R.string.ui_fertilizing_3cc00f9, "Fertilizing"); 3 -> uiText(R.string.ui_pesticide_3e04b39, "Pesticide")
                4 -> uiText(R.string.ui_weeding_4fbeba9, "Weeding"); else -> uiText(R.string.ui_other_f97e9da, "Other")
            }
            FarmSectionCard(name, subtitle = uiText(R.string.ui_recorded_1_s_utc_8372bed, "Recorded %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(activity.occurredAtUtc, farmTimeZone)))) {
                activity.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (focus == "history" && historyTab == "photos") item(key = "photo-progress-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                FarmSectionHeader(uiText(R.string.ui_crop_photo_progress_d4f31bc, "Crop photo progress"),
                    uiText(R.string.ui_inspection_photos_in_date_order_tap_one_to_inspect_i_c0622c7, "Inspection photos in date order. Tap one to inspect it."))
                if (progressPhotos.isEmpty())
                    FarmEmptyState(uiText(R.string.ui_no_inspection_photos_yet_1572ddf, "No inspection photos yet."), uiText(R.string.ui_add_photos_to_crop_observations_to_compare_them_here_612c786, "Add photos to crop observations to compare them here."))
            }
        }
        if (focus == "history" && historyTab == "photos") items(progressPhotos, key = { "progress-${it.second.id}" }) { (observation, photo) ->
            ObservationPhotoRow(
                photoId = photo.id,
                label = "${observation.observedAtUtc.take(10)} · ${observation.growthStage ?: "Inspection"}",
                onThumbnail = onThumbnail,
                enabled = !state.busy,
                onOpen = { openPhoto(photo.id) })
        }
        if (focus == "history" && historyTab == "inspections") item(key = "observations-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider()
            FarmSectionHeader(uiText(R.string.ui_observations_and_photos_37c7aaf, "Observations and photos"), uiText(R.string.ui_crop_health_and_inspection_records_fba1d8b, "Crop health and inspection records."))
            if (state.observations.isEmpty())
                FarmEmptyState(uiText(R.string.ui_no_observations_recorded_3eee6e7, "No observations recorded."), uiText(R.string.ui_new_inspection_records_will_appear_here_f1820d4, "New inspection records will appear here."))
            }
        }
        if (focus == "history" && historyTab == "inspections") items(state.observations, key = { "observation-${it.id}" }) { observation ->
            val status = when (observation.healthStatus) {
                0 -> uiText(R.string.ui_not_assessed_025761a, "Not assessed"); 1 -> uiText(R.string.ui_healthy_7f1e323, "Healthy"); 2 -> uiText(R.string.ui_needs_attention_c1ebc78, "Needs attention"); else -> uiText(R.string.ui_critical_427dd29, "Critical")
            }
            FarmSectionCard(uiText(R.string.ui_crop_observation_c89f419, "Crop observation"), subtitle = uiText(R.string.ui_recorded_1_s_utc_8372bed, "Recorded %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(observation.observedAtUtc, farmTimeZone)))) {
                FarmStatusChip(status, when (observation.healthStatus) {
                    1 -> FarmStatusTone.Active
                    2, 3 -> FarmStatusTone.Warning
                    else -> FarmStatusTone.Neutral
                })
                if (observation.issueCategory != 0) {
                    val label = listOf(uiText(R.string.ui_none_dc937b5, "None"), uiText(R.string.ui_pest_d908813, "Pest"), uiText(R.string.ui_suspected_disease_37f8b7b, "Suspected disease"),
                        uiText(R.string.ui_suspected_nutrient_deficiency_e55a8eb, "Suspected nutrient deficiency"), uiText(R.string.ui_environmental_stress_6b4374d, "Environmental stress"), uiText(R.string.ui_other_f97e9da, "Other"))
                        .getOrElse(observation.issueCategory) { uiText(R.string.ui_issue_48dc76d, "Issue") }
                    Text(uiText(R.string.ui_issue_1_s_8676304, "Issue: %1\$s", (label)))
                    FarmStatusChip(
                        uiText(R.string.ui_severity_e642a41, "Severity: ") + listOf(uiText(R.string.ui_none_dc937b5, "None"), uiText(R.string.ui_low_f793de2, "Low"), uiText(R.string.ui_moderate_5c42afc, "Moderate"), uiText(R.string.ui_high_c4ebc6d, "High"))
                            .getOrElse(observation.issueSeverity) { uiText(R.string.ui_unknown_b764cdc, "Unknown") },
                        if (observation.issueSeverity >= 2) FarmStatusTone.Warning
                        else FarmStatusTone.Planned
                    )
                    FarmStatusChip(
                        if (observation.followUpStatus == 2) uiText(R.string.ui_resolved_after_checking_be01a2a, "Resolved after checking")
                        else uiText(R.string.ui_needs_follow_up_7594b78, "Needs follow-up"),
                        if (observation.followUpStatus == 2) FarmStatusTone.Active
                        else FarmStatusTone.Warning
                    )
                    observation.followUpTaskId?.let { Text(uiText(R.string.ui_linked_task_1_s_5296b4c, "Linked task: %1\$s", (it))) }
                    if (canManageIssues && !state.offline && observation.rowVersion.isNotBlank()) {
                        var followStatus by rememberSaveable(observation.id) {
                            mutableStateOf(observation.followUpStatus)
                        }
                        var followTask by rememberSaveable(observation.id) {
                            mutableStateOf(observation.followUpTaskId)
                        }
                        var chooseTask by rememberSaveable(observation.id) { mutableStateOf(false) }
                        Text(uiText(R.string.ui_manager_follow_up_59d062c, "Manager follow-up"), style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = followStatus == 1,
                                onClick = { followStatus = 1 }, label = { Text(uiText(R.string.ui_open_ed077f3, "Open")) })
                            FilterChip(selected = followStatus == 2,
                                onClick = { followStatus = 2 }, label = { Text(uiText(R.string.ui_resolved_5be3c2c, "Resolved")) })
                        }
                        val eligible = taskOptions.filter {
                            it.cropCycleId == state.cycleId && it.taskType in listOf(4, 6)
                        }
                        OutlinedButton(onClick = { chooseTask = true }) {
                            Text(eligible.firstOrNull { it.id == followTask }?.let {
                                uiText(R.string.ui_task_due_1_s_c946e4a, "Task due %1\$s", (it.dueDate))
                            } ?: uiText(R.string.ui_no_linked_inspection_task_5f9b862, "No linked inspection task"))
                        }
                        DropdownMenu(expanded = chooseTask, onDismissRequest = { chooseTask = false }) {
                            DropdownMenuItem(text = { Text(uiText(R.string.ui_no_linked_task_65d7a19, "No linked task")) }, onClick = {
                                followTask = null; chooseTask = false
                            })
                            eligible.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text("${task.dueDate} · ${task.description ?: "Inspection"}") },
                                    onClick = { followTask = task.id; chooseTask = false })
                            }
                        }
                        Button(onClick = { onFollowUp(observation, followStatus, followTask) },
                            enabled = !state.busy) { Text(uiText(R.string.ui_save_issue_follow_up_a80ca33, "Save issue follow-up")) }
                        Text(uiText(R.string.ui_completing_an_inspection_task_does_not_automatically_9df5018, "Completing an inspection task does not automatically mark a crop issue resolved."),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                observation.growthStage?.let { Text(uiText(R.string.ui_stage_1_s_e50c962, "Stage: %1\$s", (it))) }
                observation.notes?.let { Text(it) }
                observation.photos.forEach { photo ->
                    ObservationPhotoRow(
                        photoId = photo.id,
                        label = uiText(R.string.ui_uploaded_1_s_073da49, "Uploaded %1\$s", (photo.uploadedAtUtc.take(10))),
                        onThumbnail = onThumbnail,
                        enabled = !state.busy,
                        onOpen = { openPhoto(photo.id) })
                }
                if (state.canRecord) {
                    Button(onClick = {
                        targetObservation = observation.id
                        picker.launch("image/*")
                    }, enabled = !state.busy) { Text(uiText(R.string.ui_attach_photo_offline_max_5_mb_182a20e, "Attach photo offline (max 5 MB)")) }
                    OutlinedButton(onClick = { capture(observation.id) },
                        enabled = !state.busy) { Text(uiText(R.string.ui_take_photo_for_observation_165d675, "Take photo for observation")) }
                }
            }
        }
    }
    viewerPhotoId?.let { photoId ->
        fun dismiss() {
            viewerPhotoId = null
            onDismissPreview()
        }
        Dialog(
            onDismissRequest = { dismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.84f),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(uiText(R.string.ui_inspection_photo_4c31148, "Inspection photo"), style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { dismiss() }) { Text(uiText(R.string.ui_close_7d9eb7a, "Close")) }
                    }
                    when {
                        state.previewPhotoId != photoId || state.busy ->
                            Box(Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        state.preview != null ->
                            GlidePhoto(state.preview, uiText(R.string.ui_enlarged_crop_inspection_photo_2f573c0, "Enlarged crop inspection photo"),
                                Modifier.fillMaxWidth().weight(1f))
                        else ->
                            Column(Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(uiText(R.string.ui_photo_unavailable_check_your_connection_526f482, "Photo unavailable. Check your connection."))
                                OutlinedButton(onClick = { onPreview(photoId) }) {
                                    Text(uiText(R.string.ui_retry_photo_22a7a49, "Retry photo"))
                                }
                            }
                    }
                }
            }
        }
    }

}

private sealed interface ThumbnailState {
    data object Loading : ThumbnailState
    data object Failed : ThumbnailState
    data class Ready(val bytes: ByteArray) : ThumbnailState
}

@Composable
private fun ObservationPhotoRow(
    photoId: String,
    label: String,
    onThumbnail: suspend (String) -> ByteArray,
    enabled: Boolean,
    onOpen: () -> Unit
) {
    var retry by remember(photoId) { mutableStateOf(0) }
    var thumbnail by remember(photoId) { mutableStateOf<ThumbnailState>(ThumbnailState.Loading) }
    LaunchedEffect(photoId, retry) {
        thumbnail = ThumbnailState.Loading
        try {
            thumbnail = ThumbnailState.Ready(onThumbnail(photoId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            thumbnail = ThumbnailState.Failed
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        when (val photo = thumbnail) {
            is ThumbnailState.Ready ->
                GlidePhoto(photo.bytes, uiText(R.string.ui_thumbnail_for_1_s_48274fc, "Thumbnail for %1\$s", (label)), Modifier.size(96.dp))
            ThumbnailState.Loading ->
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            ThumbnailState.Failed ->
                Column(Modifier.size(96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text(uiText(R.string.ui_unavailable_ca18449, "Unavailable"), style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { retry++ }) { Text(uiText(R.string.ui_retry_942087c, "Retry")) }
                }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpen, enabled = enabled) {
                Text(uiText(R.string.ui_enlarge_photo_b65021b, "Enlarge photo"))
            }
        }
    }
}
