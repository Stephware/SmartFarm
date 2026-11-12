package com.smartfarm.mobile.features.monitoring

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
    onIssueObservation: (Int, String, String, Int, Int, Uri?) -> Unit = { _, _, _, _, _, _ -> },
    canManageIssues: Boolean = false,
    taskOptions: List<TaskDto> = emptyList(),
    onFollowUp: (ObservationDto, Int, String?) -> Unit = { _, _, _ -> },
    onActivityWithStock: (Int, String, InventoryItemDto, String) -> Unit = { _, _, _, _ -> }
) {
    // A quick link selects one form; saving still uses the existing offline queue.
    var focus by rememberSaveable(state.farmId, state.cycleId, quick) {
        mutableStateOf(if (quick == "inspection") "inspection" else "activity")
    }
    var historyTab by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("activities") }
    var moreActivities by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(false) }
    var showGrowthStage by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(false) }
    var activityType by rememberSaveable(state.farmId, state.cycleId, quick) {
        mutableStateOf(if (quick == "fertilizing") 2 else 1)
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
            handledActivity = activityReceipt
        }
    }
    LaunchedEffect(stockReceipt) {
        if (stockReceipt != null && stockReceipt != handledStockReceipt) {
            activityNotes = ""
            stockQuantity = ""
            useStock = false
            handledStockReceipt = stockReceipt
        }
    }
    LaunchedEffect(observationReceipt) {
        if (observationReceipt != null && observationReceipt != handledObservation) {
            newPhoto = null; observationNotes = ""; stage = ""
            issueCategory = 0; issueSeverity = 0
            handledObservation = observationReceipt
        }
    }
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

    RecordPage {
        item(key = "forms") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            FarmHero("Crop monitoring", "Record today's work, inspect crops, or review past records.")
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
            }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("activity" to "Activity", "inspection" to "Inspection",
                    "history" to "History").forEach { (key, label) ->
                    FilterChip(selected = focus == key, onClick = { focus = key },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                        label = { Text(label) })
                }
            }
            PendingRecords(pending, state.farmId, state.cycleId,
                setOf("activity", "activity_stock", "observation", "photo"), onSync)
            if (!state.canRecord) {
                FarmEmptyState("Read-only crop cycle",
                    "New field records require an active crop cycle. Existing history is still available.")
                OutlinedButton(onClick = { focus = "history" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open recorded history")
                }
            }

            if (state.canRecord && focus != "history") {
                Text("Save now, sync later: your record is first saved to this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (focus == "activity") {
                FarmSectionCard("Record farm activity",
                    subtitle = "Choose an activity and save the work to this crop cycle.") {
                val activities = listOf("Watering", "Fertilizing", "Pesticide", "Weeding", "Other")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    activities.take(2).forEachIndexed { index, label ->
                        FilterChip(selected = activityType == index + 1,
                            onClick = { activityType = index + 1 }, enabled = !state.busy,
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text(label) })
                    }
                }
                TextButton(onClick = { moreActivities = !moreActivities }, enabled = !state.busy) {
                    Text(if (moreActivities) "Fewer activity types" else "Other activity types")
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
                            label = { Text("Other") })
                    }
                }
                // Editable note guides: never save guessed work or invent inspection findings.
                val noteGuide = when (activityType) {
                    1 -> "Area watered, amount or soil condition"
                    2 -> "Fertilizer used, quantity and application area"
                    3 -> "Product used, quantity and treatment area"
                    4 -> "Area weeded and observations"
                    else -> "Work performed, affected area and observations"
                }
                OutlinedTextField(activityNotes, { activityNotes = it }, enabled = !state.busy,
                    label = { Text("Notes (optional)") },
                    placeholder = { Text(noteGuide) }, modifier = Modifier.fillMaxWidth())
                val mayUseStock = activityType in 2..3
                if (mayUseStock) {
                    FilterChip(
                        selected = useStock,
                        onClick = { useStock = !useStock },
                        enabled = !state.busy,
                        label = { Text("Deduct supply from farm inventory") }
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
                            Text(it, color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (state.suppliesOffline) {
                            Text("Using saved inventory balances. The server will check stock " +
                                "and may require review when syncing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (available.isEmpty()) {
                            FarmEmptyState("No matching supplies",
                                "Add fertilizer or pesticide inventory before recording linked use.")
                        } else {
                            Text("Supply to consume", style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { selectSupply = true },
                                enabled = !state.busy) {
                                Text(selected?.let { "${it.name} · ${it.quantityOnHand} ${it.unit}" }
                                    ?: "Choose supply")
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
                                label = { Text("Quantity used (${selected?.unit ?: "item unit"})") },
                                placeholder = { Text("Example: 1.5") },
                                modifier = Modifier.fillMaxWidth(), enabled = !state.busy,
                                singleLine = true)
                            Text("This activity and its stock deduction will be queued as one " +
                                "change. The balance is not reduced until the server confirms it.",
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
                    Text(if (needsStock) "Save activity and supply use" else "Save activity")
                }
                }
                }
                if (focus == "inspection") {
                FarmSectionCard("Record inspection / observation",
                    subtitle = "Check crop condition and attach a photo if available.") {
                Text("How are the crops looking?", style = MaterialTheme.typography.labelLarge)
                listOf("Not assessed", "Healthy", "Needs attention", "Critical")
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
                    Text("What did you notice? (optional)",
                        style = MaterialTheme.typography.labelLarge)
                    listOf("None", "Pest", "Disease", "Nutrient", "Environment", "Other")
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
                    Text("How serious is it?", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Low", "Moderate", "High").forEachIndexed { index, label ->
                            FilterChip(selected = issueSeverity == index + 1,
                                onClick = { issueSeverity = index + 1 }, enabled = !state.busy,
                                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                label = { Text(label) })
                        }
                    }
                    Text("Choose Needs attention or Critical health when recording a specific issue. " +
                        "Descriptions are observations, not confirmed diagnoses.",
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(observationNotes, { observationNotes = it }, enabled = !state.busy,
                    label = { Text("Inspection notes (optional)") },
                    placeholder = { Text("Describe what you see; no diagnosis is needed") },
                    modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showGrowthStage = !showGrowthStage }, enabled = !state.busy) {
                    Text(if (showGrowthStage) "Hide growth stage" else "Add growth stage (optional)")
                }
                if (showGrowthStage) {
                    OutlinedTextField(stage, { stage = it }, enabled = !state.busy,
                        label = { Text("Growth stage") }, placeholder = {
                            Text("Vegetative, flowering, fruiting, or observed stage")
                        }, modifier = Modifier.fillMaxWidth())
                }
                Text("Photo (optional) — attach it to this inspection in one save.",
                    style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { capture(null) }, enabled = !state.busy,
                        modifier = Modifier.weight(1f)) { Text("Take photo") }
                    OutlinedButton(onClick = {
                        targetObservation = null
                        picker.launch("image/*")
                    }, enabled = !state.busy, modifier = Modifier.weight(1f)) {
                        Text("Choose photo")
                    }
                }
                newPhoto?.let { photo ->
                    Text("Photo selected for offline upload")
                    GlidePhoto(photo, "Selected crop inspection photo",
                        Modifier.fillMaxWidth().height(220.dp))
                    OutlinedButton(onClick = { newPhoto = null }, enabled = !state.busy) { Text("Remove selected photo") }
                }
                val issueValid = issueCategory == 0 && issueSeverity == 0 ||
                    issueCategory in 1..5 && issueSeverity in 1..3 && health in 2..3
                Button(onClick = {
                    if (issueCategory == 0) onObservation(health, stage, observationNotes, newPhoto)
                    else onIssueObservation(health, stage, observationNotes,
                        issueCategory, issueSeverity, newPhoto)
                }, enabled = !state.busy && issueValid, modifier = Modifier.fillMaxWidth()) {
                    Text("Save inspection")
                }
                }
                }
            }

            if (focus == "history") {
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("activities" to "Activities", "inspections" to "Inspections",
                        "photos" to "Photos").forEach { (key, label) ->
                        FilterChip(selected = historyTab == key,
                            onClick = { historyTab = key },
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                            label = { Text(label) })
                    }
                }
            }
            if (focus == "history" && historyTab == "activities") {
                FarmSectionHeader("Activity history",
                    "Previous fieldwork records for this crop cycle.")
                if (state.activities.isEmpty())
                    FarmEmptyState("No activities recorded.", "Saved fieldwork will appear here.")
            }
            }
        }
        if (focus == "history" && historyTab == "activities") items(state.activities, key = { "activity-${it.id}" }) { activity ->
            val name = when (activity.activityType) {
                1 -> "Watering"; 2 -> "Fertilizing"; 3 -> "Pesticide"
                4 -> "Weeding"; else -> "Other"
            }
            FarmSectionCard(name, subtitle = "Recorded ${activity.occurredAtUtc.take(10)} (UTC)") {
                activity.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (focus == "history" && historyTab == "photos") item(key = "photo-progress-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                FarmSectionHeader("Crop photo progress",
                    "Inspection photos in date order. Tap one to inspect it.")
                if (progressPhotos.isEmpty())
                    FarmEmptyState("No inspection photos yet.", "Add photos to crop observations to compare them here.")
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
            FarmSectionHeader("Observations and photos", "Crop health and inspection records.")
            if (state.observations.isEmpty())
                FarmEmptyState("No observations recorded.", "New inspection records will appear here.")
            }
        }
        if (focus == "history" && historyTab == "inspections") items(state.observations, key = { "observation-${it.id}" }) { observation ->
            val status = when (observation.healthStatus) {
                0 -> "Not assessed"; 1 -> "Healthy"; 2 -> "Needs attention"; else -> "Critical"
            }
            FarmSectionCard("Crop observation", subtitle = "Recorded ${observation.observedAtUtc.take(10)} (UTC)") {
                FarmStatusChip(status, when (observation.healthStatus) {
                    1 -> FarmStatusTone.Active
                    2, 3 -> FarmStatusTone.Warning
                    else -> FarmStatusTone.Neutral
                })
                if (observation.issueCategory != 0) {
                    val label = listOf("None", "Pest", "Suspected disease",
                        "Suspected nutrient deficiency", "Environmental stress", "Other")
                        .getOrElse(observation.issueCategory) { "Issue" }
                    Text("Issue: $label")
                    FarmStatusChip(
                        "Severity: " + listOf("None", "Low", "Moderate", "High")
                            .getOrElse(observation.issueSeverity) { "Unknown" },
                        if (observation.issueSeverity >= 2) FarmStatusTone.Warning
                        else FarmStatusTone.Planned
                    )
                    FarmStatusChip(
                        if (observation.followUpStatus == 2) "Resolved after checking"
                        else "Needs follow-up",
                        if (observation.followUpStatus == 2) FarmStatusTone.Active
                        else FarmStatusTone.Warning
                    )
                    observation.followUpTaskId?.let { Text("Linked task: $it") }
                    if (canManageIssues && !state.offline && observation.rowVersion.isNotBlank()) {
                        var followStatus by rememberSaveable(observation.id) {
                            mutableStateOf(observation.followUpStatus)
                        }
                        var followTask by rememberSaveable(observation.id) {
                            mutableStateOf(observation.followUpTaskId)
                        }
                        var chooseTask by rememberSaveable(observation.id) { mutableStateOf(false) }
                        Text("Manager follow-up", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = followStatus == 1,
                                onClick = { followStatus = 1 }, label = { Text("Open") })
                            FilterChip(selected = followStatus == 2,
                                onClick = { followStatus = 2 }, label = { Text("Resolved") })
                        }
                        val eligible = taskOptions.filter {
                            it.cropCycleId == state.cycleId && it.taskType in listOf(4, 6)
                        }
                        OutlinedButton(onClick = { chooseTask = true }) {
                            Text(eligible.firstOrNull { it.id == followTask }?.let {
                                "Task due ${it.dueDate}"
                            } ?: "No linked inspection task")
                        }
                        DropdownMenu(expanded = chooseTask, onDismissRequest = { chooseTask = false }) {
                            DropdownMenuItem(text = { Text("No linked task") }, onClick = {
                                followTask = null; chooseTask = false
                            })
                            eligible.forEach { task ->
                                DropdownMenuItem(
                                    text = { Text("${task.dueDate} · ${task.description ?: "Inspection"}") },
                                    onClick = { followTask = task.id; chooseTask = false })
                            }
                        }
                        Button(onClick = { onFollowUp(observation, followStatus, followTask) },
                            enabled = !state.busy) { Text("Save issue follow-up") }
                        Text("Completing an inspection task does not automatically mark a crop issue resolved.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                observation.growthStage?.let { Text("Stage: $it") }
                observation.notes?.let { Text(it) }
                observation.photos.forEach { photo ->
                    ObservationPhotoRow(
                        photoId = photo.id,
                        label = "Uploaded ${photo.uploadedAtUtc.take(10)}",
                        onThumbnail = onThumbnail,
                        enabled = !state.busy,
                        onOpen = { openPhoto(photo.id) })
                }
                if (state.canRecord) {
                    Button(onClick = {
                        targetObservation = observation.id
                        picker.launch("image/*")
                    }, enabled = !state.busy) { Text("Attach photo offline (max 5 MB)") }
                    OutlinedButton(onClick = { capture(observation.id) },
                        enabled = !state.busy) { Text("Take photo for observation") }
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
                        Text("Inspection photo", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { dismiss() }) { Text("Close") }
                    }
                    when {
                        state.previewPhotoId != photoId || state.busy ->
                            Box(Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        state.preview != null ->
                            GlidePhoto(state.preview, "Enlarged crop inspection photo",
                                Modifier.fillMaxWidth().weight(1f))
                        else ->
                            Column(Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Photo unavailable. Check your connection.")
                                OutlinedButton(onClick = { onPreview(photoId) }) {
                                    Text("Retry photo")
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
                GlidePhoto(photo.bytes, "Thumbnail for $label", Modifier.size(96.dp))
            ThumbnailState.Loading ->
                Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            ThumbnailState.Failed ->
                Column(Modifier.size(96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("Unavailable", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = { retry++ }) { Text("Retry") }
                }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onOpen, enabled = enabled) {
                Text("Enlarge photo")
            }
        }
    }
}
