package com.smartfarm.mobile.features.monitoring

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun MonitoringScreen(
    state: MonitoringUiState,
    onBack: () -> Unit,
    onActivity: (Int, String) -> Unit,
    onObservation: (Int, String, String) -> Unit,
    onUpload: (String, Uri) -> Unit,
    onPreview: (String) -> Unit
) {
    var activityType by rememberSaveable { mutableStateOf(1) }
    var activityNotes by rememberSaveable { mutableStateOf("") }
    var health by rememberSaveable { mutableStateOf(0) }
    var stage by rememberSaveable { mutableStateOf("") }
    var observationNotes by rememberSaveable { mutableStateOf("") }
    var targetObservation by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) targetObservation?.let { onUpload(it, uri) }
        targetObservation = null
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)
            .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Crop monitoring", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onBack) { Text("Back to crops") }
            if (state.busy) CircularProgressIndicator()
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (state.canRecord) {
                Text("Record farm activity", style = MaterialTheme.typography.titleLarge)
                val activities = listOf("Watering", "Fertilizing", "Pesticide", "Weeding", "Other")
                activities.forEachIndexed { index, label ->
                    OutlinedButton(onClick = { activityType = index + 1 }) {
                        Text("${if (activityType == index + 1) "✓ " else ""}$label")
                    }
                }
                OutlinedTextField(activityNotes, { activityNotes = it },
                    label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onActivity(activityType, activityNotes) },
                    enabled = !state.busy) { Text("Save activity") }

                HorizontalDivider()
                Text("Record inspection / observation", style = MaterialTheme.typography.titleLarge)
                listOf("Not assessed", "Healthy", "Needs attention", "Critical")
                    .forEachIndexed { index, label ->
                        OutlinedButton(onClick = { health = index }) {
                            Text("${if (health == index) "✓ " else ""}$label")
                        }
                    }
                OutlinedTextField(stage, { stage = it },
                    label = { Text("Growth stage") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(observationNotes, { observationNotes = it },
                    label = { Text("Observation notes") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onObservation(health, stage, observationNotes) },
                    enabled = !state.busy) { Text("Save observation") }
            }

            HorizontalDivider()
            Text("Activity history", style = MaterialTheme.typography.titleLarge)
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
                }
                if (state.canRecord) {
                    Button(onClick = {
                        targetObservation = observation.id
                        picker.launch("image/*")
                    }, enabled = !state.busy) { Text("Attach photo (max 5 MB)") }
                }
                HorizontalDivider()
            }
            state.preview?.let { bitmap ->
                Image(bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected observation photo",
                    modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
            }
        }
    }
}

