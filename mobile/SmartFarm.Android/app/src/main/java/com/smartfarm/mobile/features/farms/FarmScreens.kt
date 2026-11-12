package com.smartfarm.mobile.features.farms

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
import androidx.compose.foundation.layout.defaultMinSize
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
import com.smartfarm.mobile.ui.components.FarmAppBar
import com.smartfarm.mobile.ui.components.LocalFarmHeaderNavigation
import com.smartfarm.mobile.ui.components.FarmHero
import com.smartfarm.mobile.ui.components.FarmSectionCard
import com.smartfarm.mobile.ui.components.FarmSectionHeader
import com.smartfarm.mobile.ui.components.FarmStatusChip
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmPlotPreview
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.RecordPage
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun FarmPage(title: String, subtitle: String = "Manage your farm and team.",
    onBack: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Scaffold(topBar = { FarmAppBar() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 840.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (onBack != null) {
                OutlinedButton(onClick = onBack,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                    Text("Back")
                }
            }
            FarmHero(title, subtitle)
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
    var locationOptions by rememberSaveable { mutableStateOf(false) }

    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero("My farms", "Choose a farm to start work, or join a team.")
                Progress(state, onRetry)
                if (state.farms.isEmpty() && !state.loading && state.error == null)
                    FarmEmptyState("No farms yet", "Start your own farm or enter the code from an owner.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { creating = !creating; if (creating) joining = false },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp)) {
                        Text(if (creating) "Close farm form" else "Add a farm")
                    }
                    OutlinedButton(onClick = { joining = !joining; if (joining) creating = false },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp)) {
                        Text(if (joining) "Close join form" else "Join a farm")
                    }
                }
                if (state.farms.isNotEmpty()) {
                    Text("Your farms", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                }
            }
        }
        if (creating) item(key = "create") {
            FarmSectionCard("Create farm", subtitle = "Start with the farm name and its area.") {
                OutlinedTextField(name, { name = it }, label = { Text("Farm name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it; latitude = ""; longitude = "" },
                    label = { Text("City, town or farm address") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("You can add plots and crops after creating your farm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { locationOptions = !locationOptions }) {
                    Text(if (locationOptions) "Hide location options" else "Location pin & time zone (optional)")
                }
                if (locationOptions) {
                    Text("A pin enables local weather forecasts. Your farm can be created without one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FarmLocationPicker(location, latitude, longitude,
                        enabled = !state.loading && !state.offline) { selectedName, selectedLat, selectedLon ->
                        location = selectedName; latitude = selectedLat; longitude = selectedLon
                    }
                    TimeZoneInput(zone, { zone = it }, enabled = !state.loading && !state.offline)
                }
                Button(onClick = { onCreate(name.trim(), location.trim(), zone, latitude, longitude) },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                    enabled = !state.loading && !state.offline && name.isNotBlank() &&
                        location.isNotBlank() && zone.isNotBlank() &&
                        (latitude.isBlank() == longitude.isBlank())) { Text("Create farm") }
            }
        }
        if (joining) item(key = "join") {
            FarmSectionCard("Join a farm", subtitle = "Enter the code shared by its owner.") {
                OutlinedTextField(farmCode, { farmCode = it }, label = { Text("Farm code") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("The owner must approve your request before you can enter the farm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { onRequest(farmCode.trim()) },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                    enabled = !state.loading && !state.offline && farmCode.isNotBlank()) {
                    Text("Request access")
                }
            }
        }
        items(state.farms, key = { "farm-${it.id}" }) { farm ->
            OutlinedCard(onClick = { onOpen(farm.id) },
                modifier = Modifier.fillMaxWidth().testTag("record-card")) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FarmPlotPreview()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(farm.name, style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f))
                        FarmStatusChip(farm.role, FarmStatusTone.Planned)
                    }
                    Text(farm.locationText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Open farm →", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item(key = "enrollment-feedback") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.enrollmentMessage?.let {
                    Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (state.myRequests.isNotEmpty())
                    Text("My join requests", style = MaterialTheme.typography.titleMedium)
                if (state.myRequests.isNotEmpty() || state.enrollmentMessage != null)
                    OutlinedButton(onClick = onRetry, enabled = !state.loading) {
                        Text("Refresh join requests")
                    }
            }
        }
        items(state.myRequests.filter { it.status == "Pending" }, key = { "request-${it.id}" }) { request ->
            FarmSectionCard(request.farmName, subtitle = "Awaiting owner approval") {
                Text("${request.farmName}: Pending · Awaiting owner approval",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text("Requested ${request.requestedAtUtc} (UTC)")
                ConfirmActionButton("Cancel request for ${request.farmName}",
                    "Cancel this request? You can apply again with a valid farm code.",
                    enabled = !state.loading && !state.offline,
                    onConfirm = { onCancelRequest(request) })
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
        item(key = "account-shortcut") {
            TextButton(onClick = onBack) { Text("My account") }
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
    onTasks: () -> Unit = {}, onInsights: () -> Unit = {},
    onRetry: () -> Unit = {}, onRegenerateCode: (String) -> Unit = {},
    homeTasks: com.smartfarm.mobile.features.tasks.TaskUiState? = null,
    homeCrops: com.smartfarm.mobile.features.crops.CropUiState? = null,
    actorId: String? = null,
    section: String = "Overview", onWeather: () -> Unit = {},
    onPlots: () -> Unit = {}, onSync: () -> Unit = {},
    onRecord: () -> Unit = {}, onRecordAction: (String) -> Unit = {},
    homeSync: HomeSyncSummary = HomeSyncSummary(0, 0)) {
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
    val visibleSection = when (section) {
        "Weather", "Plots", "Members" -> section
        "Settings" -> if (canManage) section else "Restricted"
        else -> "Overview"
    }
    val sectionState = rememberSaveableStateHolder()

    val hasFarmSwitcher = LocalFarmHeaderNavigation.current != null
    FarmPage(if (visibleSection == "Overview") (farm?.name ?: "Farm") else
        "$visibleSection · ${farm?.name ?: "Farm"}",
        subtitle = if (visibleSection == "Overview") "Your daily farm work in one place."
                   else "Review and manage this farm.",
        onBack = if (visibleSection != "Overview" || !hasFarmSwitcher) onBack else null) {
        Progress(state, onRetry)
        if (farm != null) {
            if (visibleSection != "Overview") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(farm.locationText, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    FarmStatusChip(farm.role, FarmStatusTone.Planned)
                }
            }
            if (visibleSection == "Overview") {
                if (canManage) {
                    FarmSetupGuide(farm.id, state.plots.isNotEmpty(), homeCrops, onPlots, onCrops)
                }
                FarmHomeOverview(state, homeTasks, homeCrops, actorId, onTasks, onCrops, onInventory,
                    onWeather = onWeather, onInsights = onInsights,
                    onRecord = onRecord, onRecordAction = onRecordAction,
                    onPlots = onPlots, onSync = onSync, syncSummary = homeSync)
            }
            if (visibleSection == "Weather") {
                FarmWeatherDetails(state)
            }
            if (visibleSection == "Restricted") {
                FarmEmptyState("Farm settings unavailable",
                    if (state.offline) "Reconnect to edit farm settings." else
                        "Only owners and managers can edit this farm.")
            }
            if (canManage && visibleSection == "Settings") {
                FarmSectionCard("Edit farm", subtitle = "Update this farm's address, location pin and local time zone.") {
                OutlinedTextField(name, { name = it }, label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it; latitude = ""; longitude = "" },
                    label = { Text("Farm address or area") }, modifier = Modifier.fillMaxWidth())
                FarmLocationPicker(location, latitude, longitude, enabled = !state.loading) {
                    selectedName, selectedLat, selectedLon ->
                    location = selectedName
                    latitude = selectedLat
                    longitude = selectedLon
                }
                TimeZoneInput(zone, { zone = it }, enabled = !state.loading)
                Button(onClick = { onSaveFarm(name, location, zone, latitude, longitude) },
                    enabled = !state.loading && zone.isNotBlank() &&
                        (latitude.isBlank() == longitude.isBlank())) { Text("Save farm") }
                if (isOwner) ConfirmActionButton("Archive farm",
                    "Archive ${farm.name}? Its history will be retained, and it will leave your active farms.",
                    enabled = !state.loading, onConfirm = onArchiveFarm)
                }
            }
            HorizontalDivider()
            if (visibleSection == "Plots") {
                sectionState.SaveableStateProvider("plots-${farm.id}") {
                    var openPlotId by rememberSaveable(farm.id) { mutableStateOf<String?>(null) }
                    var addingPlot by rememberSaveable(farm.id) { mutableStateOf(false) }
                    var plotFilter by rememberSaveable(farm.id) { mutableStateOf("") }
                    FarmSectionHeader("Growing plots",
                        "Choose a plot to view its crops. Edit one plot at a time.")
                    Text("Plot previews are illustrations, not maps or real distances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (canManage) {
                        Button(onClick = {
                            addingPlot = !addingPlot
                            if (addingPlot) openPlotId = null
                        }, modifier = Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)) {
                            Text(if (addingPlot) "Close new plot form" else "Add growing plot")
                        }
                        if (addingPlot) {
                            FarmSectionCard("New growing plot") {
                                OutlinedTextField(plotName, { plotName = it },
                                    label = { Text("Plot name") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(plotArea, { plotArea = it },
                                    label = { Text("Area (m²)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                val areaValid = plotArea.toBigDecimalOrNull()?.let {
                                    it > java.math.BigDecimal.ZERO && it.scale() <= 2
                                } == true
                                Button(onClick = { onAddPlot(plotName.trim(), plotArea.trim()) },
                                    enabled = !state.loading && plotName.isNotBlank() && areaValid,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text("Save new plot")
                                }
                            }
                        }
                    }
                    if (state.plots.size > 5) {
                        OutlinedTextField(plotFilter, { plotFilter = it },
                            label = { Text("Find a plot") },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    val visiblePlots = state.plots.filter {
                        it.name.contains(plotFilter.trim(), ignoreCase = true)
                    }
                    if (state.plots.isEmpty()) {
                        FarmEmptyState("No growing plots yet",
                            if (canManage) "Use Add growing plot to create the first growing area."
                            else "Ask an owner or manager to add a growing area.")
                    } else if (visiblePlots.isEmpty()) {
                        FarmEmptyState("No matching plots", "Try a different plot name.")
                    }
                    visiblePlots.forEach { plot ->
                        key(plot.id) {
                            val expanded = openPlotId == plot.id
                            val cropName = homeCrops?.takeIf {
                                it.farmId == farm.id && it.error == null
                            }?.cycles?.firstOrNull {
                                it.plotId == plot.id && it.status == 1
                            }?.cropTypeName ?: homeCrops?.takeIf {
                                it.farmId == farm.id && it.error == null
                            }?.cycles?.firstOrNull {
                                it.plotId == plot.id && it.status == 0
                            }?.cropTypeName
                            OutlinedCard(
                                onClick = { openPlotId = if (expanded) null else plot.id },
                                modifier = Modifier.fillMaxWidth()
                                    .defaultMinSize(minHeight = 64.dp)
                                    .testTag("plot-card-${plot.id}")
                            ) {
                                Row(Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(plot.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${plot.areaSqMeters.toPlainString()} m² · " +
                                            (cropName ?: "No crop yet"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(if (expanded) "Close" else "Details",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (expanded) {
                                OutlinedButton(onClick = onCrops,
                                    modifier = Modifier.fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)) {
                                    Text("View crops")
                                }
                                if (canManage) {
                                    var editName by rememberSaveable(plot.id) {
                                        mutableStateOf(plot.name)
                                    }
                                    var editArea by rememberSaveable(plot.id) {
                                        mutableStateOf(plot.areaSqMeters.toPlainString())
                                    }
                                    FarmSectionCard("Edit ${plot.name}") {
                                        OutlinedTextField(editName, { editName = it },
                                            label = { Text("Plot name") },
                                            singleLine = true, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(editArea, { editArea = it },
                                            label = { Text("Area (m²)") },
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal),
                                            singleLine = true, modifier = Modifier.fillMaxWidth())
                                        val validArea = editArea.toBigDecimalOrNull()?.let {
                                            it > java.math.BigDecimal.ZERO && it.scale() <= 2
                                        } == true
                                        Button(onClick = {
                                            onSavePlot(plot, editName.trim(), editArea.trim())
                                        }, enabled = !state.loading && editName.isNotBlank() &&
                                            validArea, modifier = Modifier.fillMaxWidth()) {
                                            Text("Save plot changes")
                                        }
                                        ConfirmActionButton("Archive this plot",
                                            "Archive ${plot.name}? Its crop history is retained.",
                                            enabled = !state.loading,
                                            onConfirm = { onArchivePlot(plot) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (visibleSection == "Members") {
                FarmSectionHeader("Team & access", "Manage who can work on this farm.")
                if (farm.role == "Owner") {
                    EnrollmentPanel(state, onDecision, onRetry, onRegenerateCode)
                }
                FarmSectionHeader("Farm members", "People who already have access.")
                if (state.members.isEmpty()) {
                    FarmEmptyState("No additional members",
                        if (farm.role == "Owner")
                            "Share the code above to invite workers. Requests require your approval."
                        else "There are no additional team members yet.")
                }
                state.members.forEach { member ->
                    FarmSectionCard(member.fullName,
                        subtitle = if (member.role == "FieldWorker") "Field Worker" else member.role) {
                        if (isOwner && member.role != "Owner") {
                            ConfirmActionButton(
                                if (member.role == "Manager") "Set Field Worker" else "Set Manager",
                                "Change ${member.fullName}'s role? Their available farm actions will change.",
                                enabled = !state.loading,
                                onConfirm = { onChangeRole(member, if (member.role == "Manager") 2 else 1) })
                            ConfirmActionButton("Remove member",
                                "Remove ${member.fullName} from ${farm.name}? They will lose farm access.",
                                enabled = !state.loading, onConfirm = { onRemoveMember(member) })
                        }
                    }
                }
            }        }
    }
}




@Composable
private fun EnrollmentPanel(state: FarmUiState, onDecision: (String, Boolean) -> Unit,
    onRetry: () -> Unit, onRegenerateCode: (String) -> Unit) {
    FarmSectionHeader("Farm access requests",
        "Invite field workers and approve requests before they can enter.")
    if (state.offline) {
        Text("Join requests and farm code are unavailable offline.")
        return
    }
    state.enrollmentMessage?.let {
        Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    val overview = state.enrollment
    if (overview == null) {
        if (state.enrollmentMessage == null)
            Text("Join requests and farm code are unavailable. Refresh to try again.")
        OutlinedButton(onClick = onRetry, enabled = !state.loading) {
            Text("Refresh join requests")
        }
        return
    }
    val context = LocalContext.current
    var copied by rememberSaveable(overview.joinCode) { mutableStateOf(false) }
    FarmSectionCard("Invite field workers",
        subtitle = "Share this code. Joining still requires your approval.") {
        SelectionContainer {
            Text(overview.joinCode, style = MaterialTheme.typography.headlineSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Farm code", overview.joinCode))
                copied = true
            }, modifier = Modifier.weight(1f), enabled = !state.loading) {
                Text("Copy farm code")
            }
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Join ${state.selected?.name ?: "my farm"} in AniGo. " +
                        "Enter this farm code: ${overview.joinCode}. " +
                        "The owner must approve your request.")
                }
                context.startActivity(Intent.createChooser(intent, "Share AniGo farm code"))
            }, modifier = Modifier.weight(1f), enabled = !state.loading) {
                Text("Share code")
            }
        }
        if (copied) Text("Farm code copied.",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        HorizontalDivider()
        Text("If the code was shared with the wrong person, replace it. " +
            "Existing members and requests keep their access and status.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ConfirmActionButton("Regenerate farm code",
            "Replace the farm code? The old code will stop accepting new requests. " +
                "Existing requests and memberships stay unchanged.",
            enabled = !state.loading, onConfirm = { onRegenerateCode(overview.joinCode) })
    }
    val pending = overview.requests.filter {
        it.status == "Pending" && it.farmId == state.selected?.id
    }
    FarmSectionHeader("Pending approvals (${pending.size})",
        "Each approved request grants Field Worker access.")
    OutlinedButton(onClick = onRetry, enabled = !state.loading) {
        Text("Refresh join requests")
    }
    if (pending.isEmpty()) Text("No pending join requests.")
    pending.forEach { request ->
        FarmSectionCard(request.fullName, subtitle = request.email) {
            Text("Requested ${request.requestedAtUtc} (UTC)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ConfirmActionButton("Approve ${request.fullName}",
                "Give ${request.fullName} Field Worker access?",
                enabled = !state.loading, onConfirm = { onDecision(request.id, true) })
            ConfirmActionButton("Reject ${request.fullName}",
                "Reject ${request.fullName}'s request?",
                enabled = !state.loading, onConfirm = { onDecision(request.id, false) })
        }
    }
    val history = overview.history.filter {
        it.farmId == state.selected?.id && it.status != "Pending"
    }
    if (history.isNotEmpty()) {
        FarmSectionHeader("Request history",
            "Previously approved, rejected or canceled requests.")
        history.forEach { request ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${request.fullName}: ${request.status}")
                Text(request.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Requested ${request.requestedAtUtc} (UTC)",
                    style = MaterialTheme.typography.bodySmall)
                request.reviewedAtUtc?.let { Text("Updated $it (UTC)",
                    style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
