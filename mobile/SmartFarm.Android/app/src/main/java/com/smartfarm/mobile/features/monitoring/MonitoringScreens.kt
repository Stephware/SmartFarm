package com.smartfarm.mobile.features.monitoring

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.util.UUID
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.ui.components.OfflineDataNotice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    pending: List<PendingOperation> = emptyList(),
    onSync: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    var activityType by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(1) }
    var activityNotes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var health by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(0) }
    var stage by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var observationNotes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var targetObservation by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    var newPhoto by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<Uri?>(null) }
    var cameraPath by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf<String?>(null) }
    val activityReceipt = state.saveReceipts["${state.farmId}/${state.cycleId}/activity"]
    val observationReceipt = state.saveReceipts["${state.farmId}/${state.cycleId}/observation"]
    var handledActivity by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(activityReceipt) }
    var handledObservation by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(observationReceipt) }
    LaunchedEffect(activityReceipt) {
        if (activityReceipt != null && activityReceipt != handledActivity) {
            activityNotes = ""
            handledActivity = activityReceipt
        }
    }
    LaunchedEffect(observationReceipt) {
        if (observationReceipt != null && observationReceipt != handledObservation) {
            newPhoto = null; observationNotes = ""; stage = ""
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

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)
            .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Crop monitoring", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back to crops") }
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
            }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)

            if (state.canRecord) {
                Text("Record farm activity", style = MaterialTheme.typography.titleLarge)
                val activities = listOf("Watering", "Fertilizing", "Pesticide", "Weeding", "Other")
                activities.forEachIndexed { index, label ->
                    FilterChip(selected = activityType == index + 1, onClick = { activityType = index + 1 },
                        enabled = !state.busy, label = { Text(label) })
                }
                OutlinedTextField(activityNotes, { activityNotes = it }, enabled = !state.busy,
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onActivity(activityType, activityNotes) },
                    enabled = !state.busy) { Text("Save activity") }

                HorizontalDivider()
                Text("Record inspection / observation", style = MaterialTheme.typography.titleLarge)
                listOf("Not assessed", "Healthy", "Needs attention", "Critical")
                    .forEachIndexed { index, label ->
                        FilterChip(selected = health == index, onClick = { health = index },
                        enabled = !state.busy, label = { Text(label) })
                    }
                OutlinedTextField(stage, { stage = it }, enabled = !state.busy,
                    label = { Text("Growth stage") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(observationNotes, { observationNotes = it }, enabled = !state.busy,
                    label = { Text("Observation notes") }, modifier = Modifier.fillMaxWidth())
                Text("Optional photo — saved locally with this observation.")
                OutlinedButton(onClick = {
                    targetObservation = null
                    picker.launch("image/*")
                }, enabled = !state.busy) { Text("Select photo") }
                OutlinedButton(onClick = { capture(null) },
                    enabled = !state.busy) { Text("Take photo") }
                newPhoto?.let { photo ->
                    Text("Photo selected for offline upload")
                    GlidePhoto(photo, "Selected crop inspection photo",
                        Modifier.fillMaxWidth().height(220.dp))
                    OutlinedButton(onClick = { newPhoto = null }, enabled = !state.busy) { Text("Remove selected photo") }
                }
                Button(onClick = {
                    onObservation(health, stage, observationNotes, newPhoto)
                }, enabled = !state.busy) { Text("Save observation") }
            }

            HorizontalDivider()
            Text("Activity history", style = MaterialTheme.typography.titleLarge)
            PendingRecords(pending, state.farmId, state.cycleId,
                setOf("activity", "observation"), onSync)
            if (state.activities.isEmpty()) Text("No activities recorded.")
            state.activities.forEach { activity ->
                val name = when (activity.activityType) {
                    1 -> "Watering"; 2 -> "Fertilizing"; 3 -> "Pesticide"
                    4 -> "Weeding"; else -> "Other"
                }
                Text("$name · ${activity.occurredAtUtc}")
                activity.notes?.let { Text(it) }
            }

            HorizontalDivider()
            Text("Observations and photos", style = MaterialTheme.typography.titleLarge)
            if (state.observations.isEmpty()) Text("No observations recorded.")
            state.observations.forEach { observation ->
                val status = when (observation.healthStatus) {
                    0 -> "Not assessed"; 1 -> "Healthy"; 2 -> "Needs attention"; else -> "Critical"
                }
                Text("$status · ${observation.observedAtUtc}", style = MaterialTheme.typography.titleMedium)
                observation.growthStage?.let { Text("Stage: $it") }
                observation.notes?.let { Text(it) }
                observation.photos.forEach { photo ->
                    OutlinedButton(onClick = { onPreview(photo.id) }, enabled = !state.busy) {
                        Text("View photo · ${photo.uploadedAtUtc}")
                    }
                    if (state.previewPhotoId == photo.id && state.preview != null) {
                        GlidePhoto(state.preview, "Crop observation photo",
                            Modifier.fillMaxWidth().height(220.dp))
                    }
                }
                if (state.canRecord) {
                    Button(onClick = {
                        targetObservation = observation.id
                        picker.launch("image/*")
                    }, enabled = !state.busy) { Text("Attach photo offline (max 5 MB)") }
                    OutlinedButton(onClick = { capture(observation.id) },
                        enabled = !state.busy) { Text("Take photo for observation") }
                }
                HorizontalDivider()
            }
        }
    }
}

