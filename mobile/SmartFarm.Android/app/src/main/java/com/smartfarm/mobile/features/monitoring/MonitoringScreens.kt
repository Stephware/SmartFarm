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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.items
import com.smartfarm.mobile.ui.components.RecordPage
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
    quick: String = ""
) {
    // Quick modes only change presentation; the same offline-capable handlers save data.
    var showAll by rememberSaveable(state.farmId, state.cycleId, quick) {
        mutableStateOf(quick != "watering" && quick != "inspection")
    }
    var activityType by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(1) }
    var activityNotes by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf("") }
    var health by rememberSaveable(state.farmId, state.cycleId) { mutableStateOf(0) }
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

    val progressPhotos = state.observations
        .flatMap { observation -> observation.photos.map { observation to it } }
        .sortedWith(compareBy({ it.first.observedAtUtc }, { it.second.uploadedAtUtc }))

    RecordPage {
        item(key = "forms") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Crop monitoring", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            OutlinedButton(onClick = onBack) { Text("Back to crops") }
            if (state.busy) CircularProgressIndicator()
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                OutlinedButton(onClick = onRetry, enabled = !state.busy) { Text("Try again") }
            }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            OfflineDataNotice(state.offline, state.cachedAtUtcMillis)

            if (state.canRecord) {
                if (!showAll) {
                    Text(if (quick == "watering") "Quick log: Watering" else "Quick log: Inspection",
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = { showAll = true }) { Text("Show all logging options") }
                }
                if (showAll || quick != "inspection") {
                Text("Record farm activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                val activities = listOf("Watering", "Fertilizing", "Pesticide", "Weeding", "Other")
                activities.forEachIndexed { index, label ->
                    FilterChip(selected = activityType == index + 1, onClick = { activityType = index + 1 },
                        enabled = !state.busy, label = { Text(label) })
                }
                OutlinedTextField(activityNotes, { activityNotes = it }, enabled = !state.busy,
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onActivity(activityType, activityNotes) },
                    enabled = !state.busy) { Text("Save activity") }

                }
                if (showAll || quick != "watering") {
                HorizontalDivider()
                Text("Record inspection / observation", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
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
            }

            HorizontalDivider()
            Text("Activity history", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            PendingRecords(pending, state.farmId, state.cycleId,
                setOf("activity", "observation"), onSync)
            if (state.activities.isEmpty()) Text("No activities recorded.")
            }
        }
        items(state.activities, key = { "activity-${it.id}" }) { activity ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val name = when (activity.activityType) {
                    1 -> "Watering"; 2 -> "Fertilizing"; 3 -> "Pesticide"
                    4 -> "Weeding"; else -> "Other"
                }
                Text("$name · ${activity.occurredAtUtc}")
                activity.notes?.let { Text(it) }
            }
        }
        item(key = "photo-progress-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                Text("Crop photo progress", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                Text("Inspection photos in date order. Tap one to inspect it.")
                if (progressPhotos.isEmpty()) Text("No inspection photos yet.")
            }
        }
        items(progressPhotos, key = { "progress-${it.second.id}" }) { (observation, photo) ->
            ObservationPhotoRow(
                photoId = photo.id,
                label = "${observation.observedAtUtc.take(10)} · ${observation.growthStage ?: "Inspection"}",
                onThumbnail = onThumbnail,
                enabled = !state.busy,
                onOpen = { openPhoto(photo.id) })
        }
        item(key = "observations-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider()
            Text("Observations and photos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (state.observations.isEmpty()) Text("No observations recorded.")
            }
        }
        items(state.observations, key = { "observation-${it.id}" }) { observation ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val status = when (observation.healthStatus) {
                    0 -> "Not assessed"; 1 -> "Healthy"; 2 -> "Needs attention"; else -> "Critical"
                }
                Text("$status · ${observation.observedAtUtc}", style = MaterialTheme.typography.titleMedium)
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
                HorizontalDivider()
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
