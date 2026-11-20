package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

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
private fun FarmPage(title: String, subtitle: String = uiText(R.string.ui_manage_your_farm_and_team_416256a, "Manage your farm and team."),
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
        Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
        OutlinedButton(onClick = onRetry, enabled = !state.loading) { Text(uiText(R.string.ui_try_again_d8b8392, "Try again")) }
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
    var visibleFarmsLimit by rememberSaveable { mutableStateOf(20) }
    var requestHistoryLimit by rememberSaveable { mutableStateOf(20) }
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
        name.isNotBlank() && name.trim().length < 2 -> uiText(R.string.ui_farm_name_must_be_at_least_2_characters_010fc20, "Farm name must be at least 2 characters.")
        name.trim().length > 120 -> uiText(R.string.ui_farm_name_must_be_120_characters_or_fewer_4299bd3, "Farm name must be 120 characters or fewer.")
        else -> null
    }
    val locationError = when {
        location.isNotBlank() && location.trim().length < 2 -> uiText(R.string.ui_location_must_be_at_least_2_characters_35495f7, "Location must be at least 2 characters.")
        location.trim().length > 250 -> uiText(R.string.ui_location_must_be_250_characters_or_fewer_d7a079c, "Location must be 250 characters or fewer.")
        else -> null
    }

    RecordPage {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FarmHero(uiText(R.string.ui_my_farms_0517a09, "My farms"), uiText(R.string.ui_choose_a_farm_to_start_work_or_join_a_team_ada797b, "Choose a farm to start work, or join a team."))
                // Dialog validation is shown inside the active form, not twice
                // (once behind the modal and again in the modal).
                Progress(if (creating || joining) state.copy(error = null) else state, onRetry)
                if (state.farms.isEmpty() && !state.loading && state.error == null)
                    FarmEmptyState(uiText(R.string.ui_no_farms_yet_4ffc6fd, "No farms yet"), uiText(R.string.ui_start_your_own_farm_or_enter_the_code_from_an_owner_41d1807, "Start your own farm or enter the code from an owner."))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { creating = true; joining = false },
                        enabled = !state.loading && !state.offline,
                        modifier = Modifier.weight(1f).widthIn(min = 140.dp).defaultMinSize(minHeight = 52.dp)) {
                        Text(uiText(R.string.ui_add_a_farm_a9cf38d, "Add a farm"))
                    }
                    OutlinedButton(onClick = { joining = true; creating = false },
                        enabled = !state.loading && !state.offline,
                        modifier = Modifier.weight(1f).widthIn(min = 140.dp).defaultMinSize(minHeight = 52.dp)) {
                        Text(uiText(R.string.ui_join_a_farm_0e52b35, "Join a farm"))
                    }
                }
                if (state.farms.isNotEmpty()) {
                    Text(uiText(R.string.ui_your_farms_3633be5, "Your farms"), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                }
            }
        }
        items(state.farms.take(visibleFarmsLimit), key = { "farm-${it.id}" }) { farm ->
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
                    Text(uiText(R.string.ui_open_farm_817e6f7, "Open farm →"), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (state.farms.size > visibleFarmsLimit) item(key = "farms-more") {
            OutlinedButton(onClick = { visibleFarmsLimit += 20 },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_show_20_more_farms_1_s_remaining_bdf4c83, "Show 20 more farms (%1\$s remaining)", (state.farms.size - visibleFarmsLimit)))
            }
        }
        item(key = "enrollment-feedback") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.enrollmentMessage?.let {
                    Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (state.myRequests.isNotEmpty())
                    Text(uiText(R.string.ui_my_join_requests_72b2d81, "My join requests"), style = MaterialTheme.typography.titleMedium)
                if (state.myRequests.isNotEmpty() || state.enrollmentMessage != null)
                    OutlinedButton(onClick = onRetry, enabled = !state.loading) {
                        Text(uiText(R.string.ui_refresh_join_requests_a015710, "Refresh join requests"))
                    }
            }
        }
        items(state.myRequests.filter { it.status == "Pending" }, key = { "request-${it.id}" }) { request ->
            FarmSectionCard(request.farmName, subtitle = uiText(R.string.ui_awaiting_owner_approval_bb8f392, "Awaiting owner approval")) {
                Text(uiText(R.string.ui_1_s_pending_awaiting_owner_approval_a3f7c71, "%1\$s: Pending · Awaiting owner approval", (request.farmName)),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(uiText(R.string.ui_requested_1_s_utc_fe54f71, "Requested %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(request.requestedAtUtc, state.farms.firstOrNull { it.id == request.farmId }?.timeZoneId))))
                ConfirmActionButton(uiText(R.string.ui_cancel_request_for_1_s_bf2325a, "Cancel request for %1\$s", (request.farmName)),
                    uiText(R.string.ui_cancel_this_request_you_can_apply_again_with_a_valid_efbde5c, "Cancel this request? You can apply again with a valid farm code."),
                    enabled = !state.loading && !state.offline,
                    onConfirm = { onCancelRequest(request) })
            }
        }
        val history = state.myRequests.filter { it.status != "Pending" }
        if (history.isNotEmpty()) item(key = "request-history-heading") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(uiText(R.string.ui_request_history_7edce5d, "Request history"), style = MaterialTheme.typography.titleMedium)
                Text(uiText(R.string.ui_enter_the_farm_s_current_code_to_apply_again_after_r_191cf10, "Enter the farm's current code to apply again after rejection or cancellation."))
            }
        }
        items(history.take(requestHistoryLimit), key = { "request-${it.id}" }) { request ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${request.farmName}: ${farmRequestStatusLabel(request.status)}")
                Text(uiText(R.string.ui_requested_1_s_utc_fe54f71, "Requested %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(request.requestedAtUtc, state.farms.firstOrNull { it.id == request.farmId }?.timeZoneId))))
                request.reviewedAtUtc?.let { Text(uiText(R.string.ui_updated_1_s_utc_20c85cb, "Updated %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(it, state.farms.firstOrNull { it.id == request.farmId }?.timeZoneId)))) }
            }
        }
        if (history.size > requestHistoryLimit) item(key = "join-history-more") {
            OutlinedButton(onClick = { requestHistoryLimit += 20 },
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Text(uiText(R.string.ui_show_20_more_requests_1_s_remaining_22cec12, "Show 20 more requests (%1\$s remaining)", (history.size - requestHistoryLimit)))
            }
        }
    }
    if (creating) AlertDialog(
        onDismissRequest = ::closeCreate,
        title = { Text(uiText(R.string.ui_create_farm_2e2e93c, "Create farm")) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 540.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(uiText(R.string.ui_add_a_farm_name_and_nearby_location_you_can_set_up_p_83e1e0f, "Add a farm name and nearby location. You can set up plots and crops afterward."),
                    style = MaterialTheme.typography.bodyMedium)
                state.error?.let {
                    Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                OutlinedTextField(name, { name = it }, label = { Text(uiText(R.string.ui_farm_name_6deef3b, "Farm name")) },
                    singleLine = true, enabled = !state.loading,
                    isError = nameError != null,
                    modifier = Modifier.fillMaxWidth().semantics { nameError?.let { error(it) } })
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(location, { location = it; latitude = ""; longitude = "" },
                    label = { Text(uiText(R.string.ui_city_town_or_farm_address_8c46752, "City, town or farm address")) },
                    singleLine = true, enabled = !state.loading,
                    isError = locationError != null,
                    modifier = Modifier.fillMaxWidth().semantics { locationError?.let { error(it) } })
                locationError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { locationOptions = !locationOptions }) {
                    Text(if (locationOptions) uiText(R.string.ui_hide_location_options_0e48128, "Hide location options") else uiText(R.string.ui_location_pin_time_zone_optional_c5e6d70, "Location pin & time zone (optional)"))
                }
                if (locationOptions) {
                    Text(uiText(R.string.ui_a_map_pin_helps_provide_local_forecasts_add_one_now__6f1239e, "A map pin helps provide local forecasts. Add one now or in Farm settings."),
                        style = MaterialTheme.typography.bodySmall)
                    FarmLocationPicker(location, latitude, longitude,
                        enabled = !state.loading && !state.offline) { selectedName, selectedLat, selectedLon ->
                        location = selectedName; latitude = selectedLat; longitude = selectedLon
                    }
                    TimeZoneInput(zone, { zone = it }, enabled = !state.loading && !state.offline)
                }
                Text(uiText(R.string.ui_philippine_time_asia_manila_is_used_by_default_98e64bf, "Philippine time (Asia/Manila) is used by default."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name.trim(), location.trim(), zone, latitude, longitude) },
                enabled = !state.loading && !state.offline &&
                    name.trim().length in 2..120 && location.trim().length in 2..250 &&
                    zone.isNotBlank() && (latitude.isBlank() == longitude.isBlank())) {
                Text(uiText(R.string.ui_create_farm_2e2e93c, "Create farm"))
            }
        },
        dismissButton = { TextButton(onClick = ::closeCreate) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) } }
    )
    if (joining) AlertDialog(
        onDismissRequest = ::closeJoin,
        title = { Text(uiText(R.string.ui_join_a_farm_0e52b35, "Join a farm")) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 390.dp)
                .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(uiText(R.string.ui_enter_the_12_character_code_shared_by_the_farm_owner_0eebcff, "Enter the 12-character code shared by the farm owner. They must approve access."))
                state.error?.let {
                    Text(com.smartfarm.mobile.localization.systemMessage(it), color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                }
                OutlinedTextField(farmCode, { farmCode = it },
                    label = { Text(uiText(R.string.ui_farm_code_fedbe21, "Farm code")) }, singleLine = true,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onRequest(farmCode.trim()) },
                enabled = !state.loading && !state.offline && farmCode.isNotBlank()) {
                Text(uiText(R.string.ui_request_access_b06f166, "Request access"))
            }
        },
        dismissButton = { TextButton(onClick = ::closeJoin) { Text(uiText(R.string.ui_cancel_19766ed, "Cancel")) } }
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
    aniAlerts: com.smartfarm.mobile.features.tasks.AniAlertState? = null,
    onAcknowledgeAlert: (String) -> Unit = {},
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
    FarmPage(if (visibleSection == "Overview") uiText(R.string.ui_today_1_s_87044f5, "Today · %1\$s", (farm?.name ?: uiText(R.string.ui_farm_3457b16, "Farm"))) else
        "${farmSectionLabel(visibleSection)} · ${farm?.name ?: uiText(R.string.ui_farm_3457b16, "Farm")}",
        subtitle = if (visibleSection == "Overview") uiText(R.string.ui_your_daily_farm_work_in_one_place_fb3e663, "Your daily farm work in one place.")
                   else uiText(R.string.ui_review_and_manage_this_farm_31895e3, "Review and manage this farm."),
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
                    FarmSetupGuide(farm.id, state.plots.isNotEmpty(), homeCrops, onPlots, onCrops, actorId, onRecord)
                }
                if (!state.offline && aniAlerts?.farmId == farm.id) {
                    AniAlertPanel(aniAlerts, actorId, onWeather, onAcknowledgeAlert)
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
                FarmEmptyState(uiText(R.string.ui_farm_settings_unavailable_4f8394a, "Farm settings unavailable"),
                    if (state.offline) uiText(R.string.ui_reconnect_to_edit_farm_settings_55db2b1, "Reconnect to edit farm settings.") else
                        uiText(R.string.ui_only_owners_and_managers_can_edit_this_farm_3221416, "Only owners and managers can edit this farm."))
            }
            if (canManage && visibleSection == "Settings") {
                FarmSectionCard(uiText(R.string.ui_edit_farm_13808ae, "Edit farm"), subtitle = uiText(R.string.ui_update_this_farm_s_address_location_pin_and_local_ti_bbf3221, "Update this farm's address, location pin and local time zone.")) {
                OutlinedTextField(name, { name = it }, label = { Text(uiText(R.string.ui_name_dcd1d52, "Name")) },
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(location, { location = it; latitude = ""; longitude = "" },
                    label = { Text(uiText(R.string.ui_farm_address_or_area_a673ac8, "Farm address or area")) }, modifier = Modifier.fillMaxWidth())
                FarmLocationPicker(location, latitude, longitude, enabled = !state.loading) {
                    selectedName, selectedLat, selectedLon ->
                    location = selectedName
                    latitude = selectedLat
                    longitude = selectedLon
                }
                TimeZoneInput(zone, { zone = it }, enabled = !state.loading)
                Button(onClick = { onSaveFarm(name, location, zone, latitude, longitude) },
                    enabled = !state.loading && zone.isNotBlank() &&
                        (latitude.isBlank() == longitude.isBlank())) { Text(uiText(R.string.ui_save_farm_ad6e82f, "Save farm")) }
                if (isOwner) ConfirmActionButton(uiText(R.string.ui_archive_farm_9310769, "Archive farm"),
                    uiText(R.string.ui_archive_1_s_its_history_will_be_retained_and_it_will_3e415c8, "Archive %1\$s? Its history will be retained, and it will leave your active farms.", (farm.name)),
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
                    FarmSectionHeader(uiText(R.string.ui_growing_plots_f39fd7d, "Growing plots"),
                        uiText(R.string.ui_choose_a_plot_to_view_its_crops_edit_one_plot_at_a_t_5630815, "Choose a plot to view its crops. Edit one plot at a time."))
                    if (canManage) {
                        Button(onClick = {
                            addingPlot = !addingPlot
                            if (addingPlot) openPlotId = null
                        }, modifier = Modifier.fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)) {
                            Text(if (addingPlot) uiText(R.string.ui_close_new_plot_form_d4bbc97, "Close new plot form") else uiText(R.string.ui_add_growing_plot_962da1b, "Add growing plot"))
                        }
                        if (addingPlot) {
                            FarmSectionCard(uiText(R.string.ui_new_growing_plot_7bb7232, "New growing plot")) {
                                OutlinedTextField(plotName, { plotName = it },
                                    label = { Text(uiText(R.string.ui_plot_name_4b4ee9b, "Plot name")) },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(plotArea, { plotArea = it },
                                    label = { Text(uiText(R.string.ui_area_m_f915ae8, "Area (m²)")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                                val areaValid = plotArea.toBigDecimalOrNull()?.let {
                                    it > java.math.BigDecimal.ZERO && it.scale() <= 2
                                } == true
                                Button(onClick = { onAddPlot(plotName.trim(), plotArea.trim()) },
                                    enabled = !state.loading && plotName.isNotBlank() && areaValid,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text(uiText(R.string.ui_save_new_plot_9287048, "Save new plot"))
                                }
                            }
                        }
                    }
                    if (state.plots.size > 5) {
                        OutlinedTextField(plotFilter, { plotFilter = it },
                            label = { Text(uiText(R.string.ui_find_a_plot_ddeed61, "Find a plot")) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    val visiblePlots = state.plots.filter {
                        it.name.contains(plotFilter.trim(), ignoreCase = true)
                    }
                    if (state.plots.isEmpty()) {
                        FarmEmptyState(uiText(R.string.ui_no_growing_plots_yet_48f9e21, "No growing plots yet"),
                            if (canManage) uiText(R.string.ui_use_add_growing_plot_to_create_the_first_growing_are_08f088b, "Use Add growing plot to create the first growing area.")
                            else uiText(R.string.ui_ask_an_owner_or_manager_to_add_a_growing_area_1795b5e, "Ask an owner or manager to add a growing area."))
                    } else if (visiblePlots.isEmpty()) {
                        FarmEmptyState(uiText(R.string.ui_no_matching_plots_12c5301, "No matching plots"), uiText(R.string.ui_try_a_different_plot_name_4a4ca09, "Try a different plot name."))
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
                                onClick = {
                                    val changeSelection: () -> Unit = {
                                        if (expanded) {
                                            exitGuard?.clear("plot-${plot.id}")
                                            sectionState.removeState("plot-edit-${plot.id}")
                                        }
                                        openPlotId = if (expanded) null else plot.id
                                    }
                                    if (expanded && canManage && exitGuard != null) {
                                        exitGuard.requestForm("plot-${plot.id}", changeSelection)
                                    } else changeSelection()
                                },
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
                                            (cropName ?: uiText(R.string.ui_no_crop_yet_cd03bc7, "No crop yet")),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(if (expanded) uiText(R.string.ui_close_7d9eb7a, "Close") else uiText(R.string.ui_details_45989de, "Details"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (expanded) {
                                OutlinedButton(onClick = onCrops,
                                    modifier = Modifier.fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)) {
                                    Text(uiText(R.string.ui_view_crops_81d03d1, "View crops"))
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
                                    FarmSectionCard(uiText(R.string.ui_edit_1_s_261e9c1, "Edit %1\$s", (plot.name))) {
                                        OutlinedTextField(editName, { editName = it },
                                            label = { Text(uiText(R.string.ui_plot_name_4b4ee9b, "Plot name")) },
                                            singleLine = true, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(editArea, { editArea = it },
                                            label = { Text(uiText(R.string.ui_area_m_f915ae8, "Area (m²)")) },
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
                                            Text(uiText(R.string.ui_save_plot_changes_ee4ed17, "Save plot changes"))
                                        }
                                        ConfirmActionButton(uiText(R.string.ui_archive_this_plot_de727f8, "Archive this plot"),
                                            uiText(R.string.ui_archive_1_s_its_crop_history_is_retained_2d84147, "Archive %1\$s? Its crop history is retained.", (plot.name)),
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
                FarmSectionHeader(uiText(R.string.ui_team_access_c3b54bb, "Team & access"), uiText(R.string.ui_manage_who_can_work_on_this_farm_fe91e60, "Manage who can work on this farm."))
                if (farm.role == "Owner") {
                    EnrollmentPanel(state, onDecision, onRetry, onRegenerateCode)
                }
                FarmSectionHeader(uiText(R.string.ui_farm_members_2a82ce8, "Farm members"), uiText(R.string.ui_people_who_already_have_access_497a4c7, "People who already have access."))
                if (state.members.isEmpty()) {
                    FarmEmptyState(uiText(R.string.ui_no_additional_members_95ad835, "No additional members"),
                        if (farm.role == "Owner")
                            uiText(R.string.ui_share_the_code_above_to_invite_workers_requests_requ_64d53fb, "Share the code above to invite workers. Requests require your approval.")
                        else uiText(R.string.ui_there_are_no_additional_team_members_yet_a593a0d, "There are no additional team members yet."))
                }
                state.members.forEach { member ->
                    FarmSectionCard(member.fullName,
                        subtitle = if (member.role == "FieldWorker") uiText(R.string.ui_field_worker_4f4ed6f, "Field Worker") else farmRoleLabel(member.role)) {
                        if (isOwner && member.role != "Owner") {
                            ConfirmActionButton(
                                if (member.role == "Manager") uiText(R.string.ui_set_field_worker_63060ff, "Set Field Worker") else uiText(R.string.ui_set_manager_0f31dd9, "Set Manager"),
                                uiText(R.string.ui_change_1_s_s_role_their_available_farm_actions_will__15739a0, "Change %1\$s's role? Their available farm actions will change.", (member.fullName)),
                                enabled = !state.loading,
                                onConfirm = { onChangeRole(member, if (member.role == "Manager") 2 else 1) })
                            ConfirmActionButton(uiText(R.string.ui_remove_member_9438e0b, "Remove member"),
                                uiText(R.string.ui_remove_1_s_from_2_s_they_will_lose_farm_access_1dd3f56, "Remove %1\$s from %2\$s? They will lose farm access.", (member.fullName), (farm.name)),
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
    FarmSectionHeader(uiText(R.string.ui_farm_access_requests_8a4c1fd, "Farm access requests"),
        uiText(R.string.ui_invite_field_workers_and_approve_requests_before_the_a3cb0f8, "Invite field workers and approve requests before they can enter."))
    if (state.offline) {
        Text(uiText(R.string.ui_join_requests_and_farm_code_are_unavailable_offline_e6bb540, "Join requests and farm code are unavailable offline."))
        return
    }
    state.enrollmentMessage?.let {
        Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
    val overview = state.enrollment
    if (overview == null) {
        if (state.enrollmentMessage == null)
            Text(uiText(R.string.ui_join_requests_and_farm_code_are_unavailable_refresh__2ceaf11, "Join requests and farm code are unavailable. Refresh to try again."))
        OutlinedButton(onClick = onRetry, enabled = !state.loading) {
            Text(uiText(R.string.ui_refresh_join_requests_a015710, "Refresh join requests"))
        }
        return
    }
    val context = LocalContext.current
    var copied by rememberSaveable(overview.joinCode) { mutableStateOf(false) }
    FarmSectionCard(uiText(R.string.ui_invite_field_workers_226f055, "Invite field workers"),
        subtitle = uiText(R.string.ui_share_this_code_joining_still_requires_your_approval_f5e0cb2, "Share this code. Joining still requires your approval.")) {
        SelectionContainer {
            Text(overview.joinCode, style = MaterialTheme.typography.headlineSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText(uiText(R.string.ui_farm_code_fedbe21, "Farm code"), overview.joinCode))
                copied = true
            }, modifier = Modifier.weight(1f), enabled = !state.loading) {
                Text(uiText(R.string.ui_copy_farm_code_a710ae6, "Copy farm code"))
            }
            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, uiText(R.string.ui_join_1_s_in_anigo_055cae9, "Join %1\$s in AniGo. ", (state.selected?.name ?: "my farm")) +
                        uiText(R.string.ui_enter_this_farm_code_1_s_2ae0bfd, "Enter this farm code: %1\$s. ", (overview.joinCode)) +
                        uiText(R.string.ui_the_owner_must_approve_your_request_446e4be, "The owner must approve your request."))
                }
                context.startActivity(Intent.createChooser(intent, uiText(R.string.ui_share_anigo_farm_code_d9e355f, "Share AniGo farm code")))
            }, modifier = Modifier.weight(1f), enabled = !state.loading) {
                Text(uiText(R.string.ui_share_code_4c79ea5, "Share code"))
            }
        }
        if (copied) Text(uiText(R.string.ui_farm_code_copied_d8e7401, "Farm code copied."),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        HorizontalDivider()
        Text(uiText(R.string.ui_if_the_code_was_shared_with_the_wrong_person_replace_c8307e4, "If the code was shared with the wrong person, replace it. ") +
            uiText(R.string.ui_existing_members_and_requests_keep_their_access_and__8a49ea7, "Existing members and requests keep their access and status."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        ConfirmActionButton(uiText(R.string.ui_regenerate_farm_code_396705c, "Regenerate farm code"),
            uiText(R.string.ui_replace_the_farm_code_the_old_code_will_stop_accepti_6e2f946, "Replace the farm code? The old code will stop accepting new requests. ") +
                uiText(R.string.ui_existing_requests_and_memberships_stay_unchanged_e196b12, "Existing requests and memberships stay unchanged."),
            enabled = !state.loading, onConfirm = { onRegenerateCode(overview.joinCode) })
    }
    val pending = overview.requests.filter {
        it.status == "Pending" && it.farmId == state.selected?.id
    }
    FarmSectionHeader(uiText(R.string.ui_pending_approvals_1_s_4aae5f3, "Pending approvals (%1\$s)", (pending.size)),
        uiText(R.string.ui_each_approved_request_grants_field_worker_access_0cd14b5, "Each approved request grants Field Worker access."))
    OutlinedButton(onClick = onRetry, enabled = !state.loading) {
        Text(uiText(R.string.ui_refresh_join_requests_a015710, "Refresh join requests"))
    }
    if (pending.isEmpty()) Text(uiText(R.string.ui_no_pending_join_requests_5975b1e, "No pending join requests."))
    pending.forEach { request ->
        FarmSectionCard(request.fullName, subtitle = request.email) {
            Text(uiText(R.string.ui_requested_1_s_utc_fe54f71, "Requested %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(request.requestedAtUtc, state.selected?.timeZoneId))),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ConfirmActionButton(uiText(R.string.ui_approve_1_s_5f2fd28, "Approve %1\$s", (request.fullName)),
                uiText(R.string.ui_give_1_s_field_worker_access_a5667c4, "Give %1\$s Field Worker access?", (request.fullName)),
                enabled = !state.loading, onConfirm = { onDecision(request.id, true) })
            ConfirmActionButton(uiText(R.string.ui_reject_1_s_247e657, "Reject %1\$s", (request.fullName)),
                uiText(R.string.ui_reject_1_s_s_request_e252316, "Reject %1\$s's request?", (request.fullName)),
                enabled = !state.loading, onConfirm = { onDecision(request.id, false) })
        }
    }
    val history = overview.history.filter {
        it.farmId == state.selected?.id && it.status != "Pending"
    }
    if (history.isNotEmpty()) {
        FarmSectionHeader(uiText(R.string.ui_request_history_7edce5d, "Request history"),
            uiText(R.string.ui_previously_approved_rejected_or_canceled_requests_8c42c63, "Previously approved, rejected or canceled requests."))
        history.forEach { request ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${request.fullName}: ${farmRequestStatusLabel(request.status)}")
                Text(request.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(uiText(R.string.ui_requested_1_s_utc_fe54f71, "Requested %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(request.requestedAtUtc, state.selected?.timeZoneId))),
                    style = MaterialTheme.typography.bodySmall)
                request.reviewedAtUtc?.let { Text(uiText(R.string.ui_updated_1_s_utc_20c85cb, "Updated %1\$s", (com.smartfarm.mobile.ui.components.farmDateTime(it, state.selected?.timeZoneId))),
                    style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
