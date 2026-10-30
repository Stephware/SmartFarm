package com.smartfarm.mobile.features.farms

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.smartfarm.mobile.data.remote.dto.FarmJoinRequestDto

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import com.smartfarm.mobile.ui.components.TimeZoneInput
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedCard
import androidx.compose.ui.platform.testTag
import com.smartfarm.mobile.ui.components.RecordPage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.smartfarm.mobile.data.remote.dto.FarmMemberDto
import com.smartfarm.mobile.data.remote.dto.PlotDto
import com.smartfarm.mobile.ui.components.OfflineDataNotice
import com.smartfarm.mobile.ui.components.ConfirmActionButton

@Composable
private fun FarmPage(title: String, content: @Composable () -> Unit) {
    Scaffold { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 840.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            content()
            }
        }
    }
}

@Composable
private fun Progress(state: FarmUiState, onRetry: () -> Unit = {}) {
    if (state.loading) CircularProgressIndicator()
    state.error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        OutlinedButton(onClick = onRetry, enabled = !state.loading) { Text("Try again") }
    }
    OfflineDataNotice(state.offline, state.cachedAtUtcMillis)
}

@Composable
fun FarmListScreen(state: FarmUiState, onOpen: (String) -> Unit,
    onCreate: (String, String, String, String, String) -> Unit,
    onRequest: (String) -> Unit, onBack: () -> Unit, onRetry: () -> Unit = {},
    onCancelRequest: (FarmJoinRequestDto) -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var zone by rememberSaveable { mutableStateOf("Asia/Manila") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var farmCode by rememberSaveable { mutableStateOf("") }
    var creating by rememberSaveable { mutableStateOf(false) }
    var joining by rememberSaveable { mutableStateOf(false) }
    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("My farms", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                OutlinedButton(onClick = onBack) { Text("My account") }
                Progress(state, onRetry)
                if (state.farms.isEmpty() && !state.loading && state.error == null)
                    Text("No farms yet. Create a farm or request access with a farm code.")
                Button(onClick = { creating = !creating }) { Text(if (creating) "Close farm form" else "Add a farm") }
                OutlinedButton(onClick = { joining = !joining }) { Text(if (joining) "Close join form" else "Join a farm") }
            }
        }
        if (creating) item(key = "create") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create farm", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                Text("Coordinates are optional, but required for weather advisories.")
                TimeZoneInput(zone, { zone = it }, enabled = !state.loading && !state.offline)
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
            }
        }
        if (joining) item(key = "join") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Request farm access", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(farmCode, { farmCode = it }, label = { Text("Farm code") },
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = { onRequest(farmCode) },
                    enabled = !state.loading && !state.offline && farmCode.isNotBlank()) { Text("Request access") }
            }
        }
        item(key = "enrollment-feedback") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.enrollmentMessage?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                if (state.myRequests.isNotEmpty()) Text("My join requests", style = MaterialTheme.typography.titleMedium)
                if (state.myRequests.isNotEmpty() || state.enrollmentMessage != null)
                    OutlinedButton(onClick = onRetry, enabled = !state.loading) { Text("Refresh join requests") }
            }
        }
        items(state.myRequests.filter { it.status == "Pending" }, key = { "request-${it.id}" }) { request ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${request.farmName}: Pending · Awaiting owner approval",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text("Requested ${request.requestedAtUtc} (UTC)")
                ConfirmActionButton("Cancel request for ${request.farmName}",
                    "Cancel this request? You can apply again with a valid farm code.",
                    enabled = !state.loading && !state.offline, onConfirm = { onCancelRequest(request) })
            }
        }
        val history = state.myRequests.filter { it.status != "Pending" }
        if (history.isNotEmpty()) item(key = "request-history-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Request history", style = MaterialTheme.typography.titleMedium)
                Text("Enter the farm's current code to apply again after rejection or cancellation.")
            }
        }
        items(history, key = { "request-${it.id}" }) { request ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${request.farmName}: ${request.status}")
                Text("Requested ${request.requestedAtUtc} (UTC)")
                request.reviewedAtUtc?.let { Text("Updated $it (UTC)") }
            }
        }
        items(state.farms, key = { "farm-${it.id}" }) { farm ->
            OutlinedCard(onClick = { onOpen(farm.id) }, modifier = Modifier.fillMaxWidth().testTag("record-card")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(farm.name, style = MaterialTheme.typography.titleMedium)
                    Text(farm.locationText)
                    Text(farm.role, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

    }
}

@Composable
fun FarmDetailScreen(state: FarmUiState, onBack: () -> Unit,
    onSaveFarm: (String, String, String, String, String) -> Unit, onArchiveFarm: () -> Unit,
    onAddPlot: (String, String) -> Unit, onSavePlot: (PlotDto, String, String) -> Unit,
    onArchivePlot: (PlotDto) -> Unit,
    onCrops: () -> Unit,
    onInventory: () -> Unit,
    onDecision: (String, Boolean) -> Unit,
    onChangeRole: (FarmMemberDto, Int) -> Unit, onRemoveMember: (FarmMemberDto) -> Unit,
    onTasks: () -> Unit = {}, onRetry: () -> Unit = {}, onRegenerateCode: (String) -> Unit = {}) {
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
    var section by rememberSaveable(farm?.id) { mutableStateOf("Overview") }
    val visibleSection = if (section == "Settings" && !canManage) "Overview" else section
    val sectionState = rememberSaveableStateHolder()

    FarmPage(farm?.name ?: "Farm") {
        OutlinedButton(onClick = onBack) { Text("All farms") }
        Progress(state, onRetry)
        if (farm != null) {
            Text("${farm.locationText} · ${farm.role}")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("Overview", "Plots", "Members") + if (canManage) listOf("Settings") else emptyList()).forEach { item ->
                    FilterChip(selected = visibleSection == item, onClick = { section = item }, label = { Text(item) })
                }
            }
            if (visibleSection == "Overview") {
            if (farm.role == "Owner") EnrollmentPanel(state, onDecision, onRetry, onRegenerateCode)
            Button(onClick = onTasks) { Text("Farm tasks") }
            Button(onClick = onCrops) { Text("Crop management") }
            Button(onClick = onInventory) { Text("Inventory") }
            HorizontalDivider()
            Text("Weather forecast and advisories", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            val weather = state.weather
            if (weather == null) {
                Text(state.weatherMessage ?: "Weather information has not been loaded.")
            } else if (!weather.hasCoordinates) {
                Text("An owner or manager can add coordinates in Settings to enable weather forecasts.")
            } else {
                Text("Forecast time zone: ${weather.timeZoneId}. Latest saved forecast.")
                if (weather.days.isEmpty()) {
                    Text("No recent forecast is available. Try again later.")
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
            }
            if (canManage && visibleSection == "Settings") {
                Text("Edit farm", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it }, label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth())
                Text("Weather checks need both coordinates and a valid time zone.")
                TimeZoneInput(zone, { zone = it }, enabled = !state.loading)
                OutlinedTextField(latitude, { latitude = it }, label = { Text("Latitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(longitude, { longitude = it }, label = { Text("Longitude") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = { onSaveFarm(name, location, zone, latitude, longitude) },
                    enabled = !state.loading && zone.isNotBlank() &&
                        (latitude.isBlank() == longitude.isBlank())) { Text("Save farm") }
                if (isOwner) ConfirmActionButton("Archive farm",
                    "Archive ${farm.name}? Its history will be retained, and it will leave your active farms.",
                    enabled = !state.loading, onConfirm = onArchiveFarm)
            }
            HorizontalDivider()
            if (visibleSection == "Plots") {
            sectionState.SaveableStateProvider("plots-${farm.id}") {
            Text("Plots", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("Schematic overview only; tiles do not represent real positions or distances.",
                style = MaterialTheme.typography.bodySmall)
            if (state.plots.isNotEmpty()) {
                state.plots.chunked(2).forEach { rowPlots ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        rowPlots.forEach { plot ->
                            OutlinedCard(
                                modifier = Modifier.weight(1f),
                                onClick = onCrops
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(plot.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${plot.areaSqMeters} m²")
                                    Text("View crop records", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            if (state.plots.isEmpty()) Text("No active plots.")
            state.plots.forEach { plot ->
                key(plot.id) {
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
                    ConfirmActionButton("Archive plot", "Archive ${plot.name}? Its history will be retained.",
                        enabled = !state.loading, onConfirm = { onArchivePlot(plot) })
                }
                HorizontalDivider()
                }
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
            }
            }
            if (visibleSection == "Members") {
            Text("Members", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            state.members.forEach { member ->
                Text("${member.fullName} · ${member.role}")
                if (isOwner) {
                    ConfirmActionButton(if (member.role == "Manager") "Set Field Worker" else "Set Manager",
                        "Change the role for ${member.fullName}? Their available farm actions will change.",
                        enabled = !state.loading,
                        onConfirm = { onChangeRole(member, if (member.role == "Manager") 2 else 1) })
                    ConfirmActionButton("Remove member", "Remove ${member.fullName} from ${farm.name}? They will lose access.",
                        enabled = !state.loading, onConfirm = { onRemoveMember(member) })
                }
            }
            if (farm.role == "Owner") EnrollmentPanel(state, onDecision, onRetry, onRegenerateCode)

            }
        }
    }
}




@Composable
private fun EnrollmentPanel(state: FarmUiState, onDecision: (String, Boolean) -> Unit,
    onRetry: () -> Unit, onRegenerateCode: (String) -> Unit) {
    Text("Farm access requests", style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() })
    if (state.offline) {
        Text("Join requests and farm code are unavailable offline.")
        return
    }
    state.enrollmentMessage?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
    OutlinedButton(onClick = onRetry, enabled = !state.loading) { Text("Refresh join requests") }
    val overview = state.enrollment
    if (overview == null) {
        if (state.enrollmentMessage == null) Text("Join requests and farm code are unavailable. Refresh to try again.")
        return
    }
    Text("Share this farm code. Farmers receive Field Worker access only after you approve.")
    SelectionContainer { Text(overview.joinCode) }
    val context = LocalContext.current
    var copied by rememberSaveable(overview.joinCode) { mutableStateOf(false) }
    OutlinedButton(onClick = {
        context.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Farm code", overview.joinCode))
        copied = true
    }, enabled = !state.loading) { Text("Copy farm code") }
    if (copied) Text("Farm code copied.", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    ConfirmActionButton("Regenerate farm code",
        "Replace the farm code? The old code will stop accepting new requests. Existing requests and memberships stay unchanged.",
        enabled = !state.loading, onConfirm = { onRegenerateCode(overview.joinCode) })
    val pending = overview.requests.filter { it.status == "Pending" && it.farmId == state.selected?.id }
    if (pending.isEmpty()) Text("No pending join requests.")
    pending.forEach { request ->
        Text(request.fullName, style = MaterialTheme.typography.titleMedium)
        Text(request.email)
        Text("Requested ${request.requestedAtUtc} (UTC)")
        ConfirmActionButton("Approve ${request.fullName}", "Give ${request.fullName} Field Worker access?",
            enabled = !state.loading, onConfirm = { onDecision(request.id, true) })
        ConfirmActionButton("Reject ${request.fullName}", "Reject ${request.fullName}'s request?",
            enabled = !state.loading, onConfirm = { onDecision(request.id, false) })
    }
    val history = overview.history.filter { it.farmId == state.selected?.id && it.status != "Pending" }
    if (history.isNotEmpty()) {
        Text("Request history", style = MaterialTheme.typography.titleMedium)
        history.forEach { request ->
            Text("${request.fullName}: ${request.status}")
            Text(request.email)
            Text("Requested ${request.requestedAtUtc} (UTC)")
            request.reviewedAtUtc?.let { Text("Updated $it (UTC)") }
        }
    }

}
