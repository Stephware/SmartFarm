package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.ui.components.UnsavedChanges
import com.smartfarm.mobile.ui.components.LocalExitGuard
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import com.smartfarm.mobile.ui.components.TimeZoneInput
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
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
import com.smartfarm.mobile.ui.components.farmRoleLabel
import com.smartfarm.mobile.ui.components.FarmStatusTone
import com.smartfarm.mobile.ui.components.FarmEmptyState
import com.smartfarm.mobile.ui.components.RecordPage
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    Scaffold(topBar = { FarmAppBar(title = title, onBack = onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 840.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
    val exitGuard = LocalExitGuard.current
    UnsavedChanges("farm-create", creating && (name.isNotEmpty() || location.isNotEmpty() ||
        zone != "Asia/Manila" || latitude.isNotEmpty() || longitude.isNotEmpty()),
        name, location, zone, latitude, longitude)
    UnsavedChanges("farm-join", joining && farmCode.isNotEmpty(), farmCode)
    var handledJoinSave by rememberSaveable { mutableStateOf(state.joinSaveVersion) }
    LaunchedEffect(state.joinSaveVersion) {
        if (state.joinSaveVersion > handledJoinSave) {
            handledJoinSave = state.joinSaveVersion
            joining = false
            farmCode = ""
            exitGuard?.clear("farm-join")
        }
    }
    fun closeCreate() {
        val discard: () -> Unit = {
            creating = false; name = ""; location = ""; zone = "Asia/Manila"
            latitude = ""; longitude = ""; locationOptions = false
            exitGuard?.clear("farm-create")
        }
        if (exitGuard != null) exitGuard.requestForm("farm-create", discard) else discard()
    }
    fun closeJoin() {
        val discard: () -> Unit = { joining = false; farmCode = ""; exitGuard?.clear("farm-join") }
        if (exitGuard != null) exitGuard.requestForm("farm-join", discard) else discard()
    }
    val nameError = when {
        name.isNotBlank() && name.trim().length < 2 -> "Farm name must be at least 2 characters."
        name.trim().length > 120 -> "Farm name must be 120 characters or fewer."
        else -> null
    }
    val locationError = when {
        location.isNotBlank() && location.trim().length < 2 -> "Location must be at least 2 characters."
        location.trim().length > 250 -> "Location must be 250 characters or fewer."
        else -> null
    }

    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero("My farms", "Choose a farm to start work, or join a team.")
                Progress(state, onRetry)
                if (state.farms.isEmpty() && !state.loading && state.error == null)
                    FarmEmptyState("No farms yet", "Start your own farm or enter the code from an owner.")
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { creating = true; joining = false },
                        enabled = !state.loading && !state.offline,
                        modifier = Modifier.weight(1f).widthIn(min = 140.dp).defaultMinSize(minHeight = 52.dp)) {
                        Text("Add a farm")
                    }
                    OutlinedButton(onClick = { joining = true; creating = false },
                        enabled = !state.loading && !state.offline,
                        modifier = Modifier.weight(1f).widthIn(min = 140.dp).defaultMinSize(minHeight = 52.dp)) {
                        Text("Join a farm")
                    }
                }
                if (state.farms.isNotEmpty()) {
                    Text("Your farms", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                }
            }
        }
        items(state.farms, key = { "farm-${it.id}" }) { farm ->
            OutlinedCard(onClick = { onOpen(farm.id) },
                modifier = Modifier.fillMaxWidth().testTag("record-card")) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f).widthIn(min = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(farm.name, style = MaterialTheme.typography.titleMedium)
                            Text(farm.locationText, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FarmStatusChip(farmRoleLabel(farm.role), FarmStatusTone.Planned)
                    }
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
    }
    if (creating) AlertDialog(
        onDismissRequest = ::closeCreate,
        title = { Text("Create farm") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 540.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add a farm name and nearby location. You can set up plots and crops afterward.",
                    style = MaterialTheme.typography.bodyMedium)
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                OutlinedTextField(name, { name = it }, label = { Text("Farm name") },
                    singleLine = true, enabled = !state.loading,
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth().semantics { nameError?.let { error(it) } })
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(location, { location = it; latitude = ""; longitude = "" },
                    label = { Text("City, town or farm address") },
                    singleLine = true, enabled = !state.loading,
                    isError = locationError != null,
                    modifier = Modifier.fillMaxWidth().semantics { locationError?.let { error(it) } })
                locationError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { locationOptions = !locationOptions }) {
                    Text(if (locationOptions) "Hide location options" else "Location pin & time zone (optional)")
                }
                if (locationOptions) {
                    Text("A map pin helps provide local forecasts. Add one now or in Farm settings.",
                        style = MaterialTheme.typography.bodySmall)
                    FarmLocationPicker(location, latitude, longitude,
                        enabled = !state.loading && !state.offline) { selectedName, selectedLat, selectedLon ->
                        location = selectedName; latitude = selectedLat; longitude = selectedLon
                    }
                    TimeZoneInput(zone, { zone = it }, enabled = !state.loading && !state.offline)
                }
                Text("Philippine time (Asia/Manila) is used by default.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name.trim(), location.trim(), zone, latitude, longitude) },
                enabled = !state.loading && !state.offline &&
                    name.trim().length in 2..120 && location.trim().length in 2..250 &&
                    zone.isNotBlank() && (latitude.isBlank() == longitude.isBlank())) {
                Text("Create farm")
            }
        },
        dismissButton = { TextButton(onClick = ::closeCreate) { Text("Cancel") } }
    )
    if (joining) AlertDialog(
        onDismissRequest = ::closeJoin,
        title = { Text("Join a farm") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 390.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter the 12-character code shared by the farm owner. They must approve access.")
                state.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                OutlinedTextField(farmCode, { farmCode = it },
                    label = { Text("Farm code") }, singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onRequest(farmCode.trim()) },
                enabled = !state.loading && !state.offline && farmCode.isNotBlank()) {
                Text("Request access")
            }
        },
        dismissButton = { TextButton(onClick = ::closeJoin) { Text("Cancel") } }
    )
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
    onTask: (com.smartfarm.mobile.data.remote.api.TaskDto) -> Unit = {},
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
    UnsavedChanges("farm-settings", visibleSection == "Settings" && farm != null &&
        (name != farm.name || location != farm.locationText || zone != farm.timeZoneId ||
            latitude != farm.latitude?.toPlainString().orEmpty() || longitude != farm.longitude?.toPlainString().orEmpty()))
    val sectionState = rememberSaveableStateHolder()
    val exitGuard = LocalExitGuard.current

    val hasFarmSwitcher = LocalFarmHeaderNavigation.current != null
    FarmPage(if (visibleSection == "Overview") "Today · ${farm?.name ?: "Farm"}" else
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
                    FarmStatusChip(farmRoleLabel(farm.role), FarmStatusTone.Planned)
                }
            }
            if (visibleSection == "Overview") {
                if (canManage) {
                    FarmSetupGuide(farm.id, state.plots.isNotEmpty(), homeCrops, onPlots, onCrops)
                }
                FarmHomeOverview(state, homeTasks, homeCrops, actorId, onTasks,
                    onWeather = onWeather, onTask = onTask,
                    onRecord = onRecord, onRecordAction = onRecordAction,
                    onSync = onSync, syncSummary = homeSync)
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
                    UnsavedChanges("new-plot", addingPlot && (plotName.isNotEmpty() || plotArea.isNotEmpty()),
                        plotName, plotArea)
                    var handledPlotSave by rememberSaveable(farm.id) { mutableStateOf(state.plotSaveVersion) }
                    var handledPlotEdit by rememberSaveable(farm.id) { mutableStateOf(state.plotEditSaveVersion) }
                    LaunchedEffect(state.plotSaveVersion) {
                        if (state.plotSaveVersion > handledPlotSave) {
                            handledPlotSave = state.plotSaveVersion
                            addingPlot = false
                            plotName = ""; plotArea = ""
                            exitGuard?.clear("new-plot")
                        }
                    }
                    LaunchedEffect(state.plotEditSaveVersion) {
                        if (state.plotEditSaveVersion > handledPlotEdit) {
                            handledPlotEdit = state.plotEditSaveVersion
                            state.savedPlotId?.let { saved ->
                                if (openPlotId == saved) openPlotId = null
                                exitGuard?.clear("plot-$saved")
                                sectionState.removeState("plot-edit-$saved")
                            }
                        }
                    }
                    FarmSectionHeader("Growing plots",
                        "Choose a plot to view its crops. Edit one plot at a time.")
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
                                    sectionState.SaveableStateProvider("plot-edit-${plot.id}") {
                                    var editName by rememberSaveable(plot.id) {
                                        mutableStateOf(plot.name)
                                    }
                                    var editArea by rememberSaveable(plot.id) {
                                        mutableStateOf(plot.areaSqMeters.toPlainString())
                                    }
                                    UnsavedChanges("plot-${plot.id}", editName != plot.name ||
                                        editArea.toBigDecimalOrNull()?.compareTo(plot.areaSqMeters) != 0)
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
