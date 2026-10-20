package com.smartfarm.mobile.features.farms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.smartfarm.mobile.data.remote.dto.FarmMemberDto
import com.smartfarm.mobile.data.remote.dto.PlotDto
import com.smartfarm.mobile.ui.components.OfflineDataNotice

@Composable
private fun FarmPage(title: String, content: @Composable () -> Unit) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            content()
        }
    }
}

@Composable
private fun Progress(state: FarmUiState) {
    if (state.loading) CircularProgressIndicator()
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
}

@Composable
fun FarmListScreen(state: FarmUiState, onOpen: (String) -> Unit,
    onCreate: (String, String, String, String, String) -> Unit,
    onAccept: (String) -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var zone by rememberSaveable { mutableStateOf("Asia/Manila") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var invitation by remember { mutableStateOf("") }
    FarmPage("My farms") {
        OutlinedButton(onClick = onBack) { Text("My account") }
        Progress(state)
        if (state.farms.isEmpty() && !state.loading) Text("No farms yet. Create one or accept an invitation.")
        state.farms.forEach { farm ->
            Button(onClick = { onOpen(farm.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("${farm.name} · ${farm.role}")
            }
        }
        HorizontalDivider()
        Text("Create farm", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        Text("Coordinates are optional, but required for weather advisories.")
        OutlinedTextField(zone, { zone = it }, label = { Text("Time zone (e.g., Asia/Manila)") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth())
        Button(onClick = { onCreate(name, location, zone, latitude, longitude) },
            enabled = !state.loading && !state.offline && name.isNotBlank() &&
                location.isNotBlank() && zone.isNotBlank() &&
                (latitude.isBlank() == longitude.isBlank())) { Text("Create") }
        HorizontalDivider()
        Text("Accept an invitation", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(invitation, { invitation = it }, label = { Text("Invitation token") },
            modifier = Modifier.fillMaxWidth())
        Button(onClick = { onAccept(invitation) },
            enabled = !state.loading && !state.offline && invitation.isNotBlank()) { Text("Join farm") }
    }
}

@Composable
fun FarmDetailScreen(state: FarmUiState, onBack: () -> Unit,
    onSaveFarm: (String, String, String, String, String) -> Unit, onArchiveFarm: () -> Unit,
    onAddPlot: (String, String) -> Unit, onSavePlot: (PlotDto, String, String) -> Unit,
    onArchivePlot: (PlotDto) -> Unit,
    onCrops: () -> Unit,
    onInventory: () -> Unit,
    onInvite: (String, Int) -> Unit,
    onChangeRole: (FarmMemberDto, Int) -> Unit, onRemoveMember: (FarmMemberDto) -> Unit) {
    val farm = state.selected
    val canManage = !state.offline && farm?.role in listOf("Owner", "Manager")
    val isOwner = !state.offline && farm?.role == "Owner"
    var name by rememberSaveable(farm?.id) { mutableStateOf(farm?.name.orEmpty()) }
    var location by rememberSaveable(farm?.id) { mutableStateOf(farm?.locationText.orEmpty()) }
    var zone by rememberSaveable(farm?.id) { mutableStateOf(farm?.timeZoneId ?: "Asia/Manila") }
    var latitude by rememberSaveable(farm?.id) { mutableStateOf(farm?.latitude?.toPlainString().orEmpty()) }
    var longitude by rememberSaveable(farm?.id) { mutableStateOf(farm?.longitude?.toPlainString().orEmpty()) }
    var plotName by rememberSaveable { mutableStateOf("") }
    var plotArea by rememberSaveable { mutableStateOf("") }
    var inviteEmail by rememberSaveable { mutableStateOf("") }
    var inviteRole by rememberSaveable { mutableStateOf(2) }

    FarmPage(farm?.name ?: "Farm") {
        OutlinedButton(onClick = onBack) { Text("All farms") }
        Progress(state)
        if (farm != null) {
            Text("${farm.locationText} · ${farm.role}")
            Button(onClick = onCrops) { Text("Crop management") }
            Button(onClick = onInventory) { Text("Inventory") }
            HorizontalDivider()
            Text("Weather forecast and advisories", style = MaterialTheme.typography.titleLarge)
            val weather = state.weather
            if (weather == null) {
                Text(state.weatherMessage ?: "Weather information has not been loaded.")
            } else if (!weather.hasCoordinates) {
                Text("Add both farm coordinates in Edit farm to enable weather checks.")
            } else {
                Text("Forecast time zone: ${weather.timeZoneId}. Saved by the weather worker.")
                if (weather.days.isEmpty()) {
                    Text("No saved forecast yet. Check the worker and weather provider.")
                } else {
                    weather.days.forEach { day ->
                        Text("${day.date}: ${day.minTemperatureC ?: "—"}–${day.maxTemperatureC ?: "—"} °C, " +
                            "${day.rainfallMm ?: "—"} mm rain, " +
                            "${day.maxPrecipitationProbability ?: "—"}% rain chance, " +
                            "${day.maxWindKph ?: "—"} km/h wind",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Checked ${day.retrievedAtUtc} (UTC)",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                val active = weather.advisories.filter { it.status == 0 }
                if (active.isEmpty()) Text("No active weather warnings.")
                active.forEach { advisory ->
                    Text("${advisory.date} · Severity ${advisory.severity}: ${advisory.message}",
                        color = MaterialTheme.colorScheme.error)
                }
            }
            if (canManage) {
                Text("Edit farm", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it }, label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth())
                Text("Weather checks need both coordinates and a valid time zone.")
                OutlinedTextField(zone, { zone = it }, label = { Text("Time zone") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = { onSaveFarm(name, location, zone, latitude, longitude) },
                    enabled = !state.loading && zone.isNotBlank() &&
                        (latitude.isBlank() == longitude.isBlank())) { Text("Save farm") }
                if (isOwner) OutlinedButton(onClick = onArchiveFarm, enabled = !state.loading) {
                    Text("Archive farm")
                }
            }
            HorizontalDivider()
            Text("Plots", style = MaterialTheme.typography.titleLarge)
            if (state.plots.isEmpty()) Text("No active plots.")
            state.plots.forEach { plot ->
                var editName by rememberSaveable(plot.id) { mutableStateOf(plot.name) }
                var editArea by rememberSaveable(plot.id) { mutableStateOf(plot.areaSqMeters.toPlainString()) }
                Text("${plot.name}: ${plot.areaSqMeters} m²")
                if (canManage) {
                    OutlinedTextField(editName, { editName = it },
                        label = { Text("Plot name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editArea, { editArea = it },
                        label = { Text("Area (m²)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onSavePlot(plot, editName, editArea) },
                        enabled = !state.loading && editName.isNotBlank() && editArea.isNotBlank()) {
                        Text("Save plot")
                    }
                    OutlinedButton(onClick = { onArchivePlot(plot) }, enabled = !state.loading) {
                        Text("Archive plot")
                    }
                }
                HorizontalDivider()
            }
            if (canManage) {
                Text("Add plot", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(plotName, { plotName = it }, label = { Text("Plot name") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(plotArea, { plotArea = it }, label = { Text("Area (m²)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = { onAddPlot(plotName, plotArea) },
                    enabled = !state.loading && plotName.isNotBlank() && plotArea.isNotBlank()) {
                    Text("Add plot")
                }
            }
            HorizontalDivider()
            Text("Members", style = MaterialTheme.typography.titleLarge)
            state.members.forEach { member ->
                Text("${member.fullName} · ${member.role}")
                if (isOwner) {
                    Button(onClick = { onChangeRole(member, if (member.role == "Manager") 2 else 1) },
                        enabled = !state.loading) {
                        Text(if (member.role == "Manager") "Set Field Worker" else "Set Manager")
                    }
                    OutlinedButton(onClick = { onRemoveMember(member) },
                        enabled = !state.loading) { Text("Remove member") }
                }
            }
            if (isOwner) {
                Text("Invite registered user", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(inviteEmail, { inviteEmail = it },
                    label = { Text("Registered email") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { inviteRole = if (inviteRole == 1) 2 else 1 }) {
                    Text("Role: ${if (inviteRole == 1) "Manager" else "Field Worker"}")
                }
                Button(onClick = { onInvite(inviteEmail, inviteRole) },
                    enabled = !state.loading && inviteEmail.isNotBlank()) { Text("Create invitation") }
                state.invitation?.let {
                    Text("Share this token privately with ${it.email}. It expires in 24 hours.")
                    SelectionContainer { Text(it.token) }
                }
            }
        }
    }
}



