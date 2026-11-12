package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.smartfarm.mobile.ui.components.LocalFarmHeaderNavigation
import com.smartfarm.mobile.ui.components.FarmHeaderNavigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthViewModel
import com.smartfarm.mobile.features.status.StatusViewModel
import com.smartfarm.mobile.features.auth.LoginScreen
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.features.farms.FarmViewModel
import com.smartfarm.mobile.features.crops.CropViewModel
import com.smartfarm.mobile.features.crops.CropScreen
import com.smartfarm.mobile.features.monitoring.MonitoringViewModel
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.inventory.InventoryViewModel
import com.smartfarm.mobile.features.inventory.InventoryListScreen
import com.smartfarm.mobile.features.inventory.InventoryItemScreen
import com.smartfarm.mobile.features.harvests.HarvestViewModel
import com.smartfarm.mobile.features.harvests.HarvestScreen
import com.smartfarm.mobile.features.insights.FarmInsightsViewModel
import com.smartfarm.mobile.features.insights.FarmInsightsScreen
import com.smartfarm.mobile.features.syncstatus.SyncStatusViewModel
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import com.smartfarm.mobile.features.syncstatus.syncQueueSummary
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmDetailScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel, farms: FarmViewModel, crops: CropViewModel, monitoring: MonitoringViewModel, inventory: InventoryViewModel, harvests: HarvestViewModel, status: StatusViewModel, sync: SyncStatusViewModel, tasks: com.smartfarm.mobile.features.tasks.TaskViewModel, insights: FarmInsightsViewModel) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val currentFarmId = currentEntry?.arguments?.getString("farmId").orEmpty()
    val showFarmTabs = currentRoute in setOf(
        "farm/{farmId}", "tasks/{farmId}", "crops/{farmId}", "more/{farmId}"
    ) && currentFarmId.isNotBlank()
    val appContext = LocalContext.current
    val resolver = appContext.contentResolver
    val state by auth.state.collectAsStateWithLifecycle()
    val pending by sync.operations.collectAsStateWithLifecycle()
    val pendingPhotos by sync.photos.collectAsStateWithLifecycle()
    val headerFarm by farms.state.collectAsStateWithLifecycle()
    val cropState by crops.state.collectAsStateWithLifecycle()
    val accountQueue = syncQueueSummary(pending, pendingPhotos, userId = state.account?.id)
    var showFarmSwitcher by rememberSaveable { mutableStateOf(false) }
    var recordForFarm by rememberSaveable { mutableStateOf<String?>(null) }
    var recordAction by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFarmSwitch by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.account?.id) {
        showFarmSwitcher = false
        recordForFarm = null
        recordAction = null
        pendingFarmSwitch = null
        farms.clear()
        if (state.account != null) sync.load()
    }
    val expandedWorkspace = LocalConfiguration.current.screenWidthDp >= 600
    val navigateWorkspace: (String) -> Unit = { destination ->
        if (destination == "record") {
            recordAction = null
            recordForFarm = currentFarmId
            if (crops.state.value.farmId != currentFarmId) crops.load(currentFarmId)
        } else {
            val route = destination.replace("{farmId}", currentFarmId)
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                popUpTo("farm/$currentFarmId") { saveState = true }
            }
        }
    }

    fun completeFarmSwitch(target: String) {
        showFarmSwitcher = false
        pendingFarmSwitch = null
        if (target == "__all__") {
            navController.navigate("farms") { launchSingleTop = true }
        } else if (target != currentFarmId) {
            navController.navigate("farm/$target") {
                popUpTo("farms") { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    fun requestFarmSwitch(target: String) {
        showFarmSwitcher = false
        if (requiresExitConfirmation(currentRoute,
                currentEntry?.arguments?.getString("section"))) {
            pendingFarmSwitch = target
        } else completeFarmSwitch(target)
    }

    // Both home and dedicated farm subpages use the existing data/actions.
    val farmDestination: @Composable (String, String) -> Unit = { farmId, section ->
        val farmState by farms.state.collectAsStateWithLifecycle()
        val homeTasks by tasks.state.collectAsStateWithLifecycle()
        val homeCrops by crops.state.collectAsStateWithLifecycle()
        LaunchedEffect(farmId) {
            if (farms.state.value.selected?.id != farmId) farms.loadDetail(farmId)
            if (tasks.state.value.farmId != farmId) tasks.load(farmId)
            if (crops.state.value.farmId != farmId) crops.load(farmId)
        }
        FarmDetailScreen(farmState,
            onBack = {
                if (section == "Overview") {
                    navController.navigate("farms") { launchSingleTop = true }
                } else navController.popBackStack()
            },
            onSaveFarm = { name, location, zone, latitude, longitude ->
                farms.saveFarm(name, location, zone, latitude, longitude) {} },
            onArchiveFarm = { farms.archiveFarm { navController.navigate("farms") } },
            onAddPlot = { name, area -> farms.addPlot(name, area) },
            onSavePlot = { plot, name, area -> farms.savePlot(plot, name, area) },
            onArchivePlot = { plot -> farms.archivePlot(plot) },
            onCrops = { navController.navigate("crops/$farmId") },
            onInventory = { navController.navigate("inventory/$farmId") },
            onDecision = farms::decideJoinRequest,
            onChangeRole = { member, role -> farms.changeRole(member, role) },
            onRemoveMember = { farms.remove(it) },
            onTasks = { navController.navigate("tasks/$farmId") },
            onInsights = { navController.navigate("insights/$farmId") },
            onRetry = { farms.loadDetail(farmId) },
            onRegenerateCode = farms::regenerateJoinCode,
            homeTasks = homeTasks, homeCrops = homeCrops, actorId = state.account?.id,
            section = section,
            onWeather = { navController.navigate("farm/$farmId/weather") },
            onPlots = { navController.navigate("farm/$farmId/plots") },
            onSync = { navController.navigate("sync") },
            onRecord = {
                recordAction = null
                recordForFarm = farmId
                if (crops.state.value.farmId != farmId) crops.load(farmId)
            },
            onRecordAction = { action ->
                recordAction = action
                recordForFarm = farmId
                if (crops.state.value.farmId != farmId) crops.load(farmId)
            },
            homeSync = com.smartfarm.mobile.features.farms.homeSyncSummary(
                pending, pendingPhotos, farmId, state.account?.id))
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (state.account != null && showFarmTabs && !expandedWorkspace) {
                FarmWorkspaceBar(selectedRoute = currentRoute.orEmpty(), onNavigate = navigateWorkspace)
            }
        }
    ) { workspacePadding ->
    CompositionLocalProvider(LocalFarmHeaderNavigation provides if (state.account == null) null else
        FarmHeaderNavigation(headerFarm.selected?.takeIf { it.id == currentFarmId }?.name,
            onFarms = {
                if (currentFarmId.isBlank()) {
                    navController.navigate("farms") { launchSingleTop = true }
                } else showFarmSwitcher = true
            },
            onNotifications = { navController.navigate("notifications") { launchSingleTop = true } },
            queuedCount = accountQueue.total,
            reviewCount = accountQueue.needsReview,
            onSync = { navController.navigate("sync") { launchSingleTop = true } })) {
    Row(Modifier.fillMaxSize().padding(workspacePadding)) {
    if (state.account != null && showFarmTabs && expandedWorkspace) {
        FarmWorkspaceBar(selectedRoute = currentRoute.orEmpty(), onNavigate = navigateWorkspace, expanded = true)
    }
    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = Modifier.weight(1f)
    ) {
        composable("welcome") {
            val statusState by status.state.collectAsStateWithLifecycle()
            WelcomeScreen(
                status = statusState,
                onCheckBackend = { status.check() },
                onLogin = { navController.navigate("login") },
                onRegister = { navController.navigate("register") })
        }
        composable("login") {
            LoginScreen(state,
                onSubmit = { email, password ->
                    auth.login(email, password) {
                        navController.navigate("farms") { popUpTo("welcome") }
                    }
                },
                onRegister = { navController.navigate("register") })
        }
        composable("register") {
            RegisterScreen(state,
                onSubmit = { name, email, password ->
                    auth.register(name, email, password) {
                        navController.navigate("login") { popUpTo("welcome") }
                    }
                },
                onLogin = { navController.navigate("login") })
        }
        composable("account") {
            AccountScreen(state, onLogout = {
                auth.logout()
                farms.clear()
                crops.clear()
                monitoring.clear()
                // Remove any decoded private photos after signing out.
                Glide.get(appContext).clearMemory()
                inventory.clear()
                harvests.clear()
                insights.clear()
                sync.clear()
                tasks.clear()
                navController.navigate("welcome") { popUpTo("welcome") { inclusive = true } }
            }, onFarms = { navController.navigate("farms") },
                onSync = { navController.navigate("sync") },
                onNotifications = { navController.navigate("notifications") },
                pendingSyncCount = accountQueue.total)
        }
        composable("sync") {
            val pending by sync.operations.collectAsStateWithLifecycle()
            val photos by sync.photos.collectAsStateWithLifecycle()
            val preview by sync.stockPreview.collectAsStateWithLifecycle()
            val syncError by sync.error.collectAsStateWithLifecycle()
            val syncNotice by sync.notice.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { sync.load() }
            SyncStatusScreen(pending, photos, preview, syncError,
                onBack = { navController.popBackStack() },
                onRetry = { sync.retry(it) },
                onDiscard = { sync.discard(it) },
                onRetryPhoto = { sync.retryPhoto(it) },
                onDiscardPhoto = { sync.discardPhoto(it) },
                onReviewStock = { sync.reviewStock(it) },
                onApproveStock = { sync.approveStockRetry() },
                onCancelStock = { sync.cancelReview() },
                onSyncNow = { sync.syncNow() },
                currentFarmId = headerFarm.selected?.id,
                farmNames = headerFarm.farms.associate { it.id to it.name },
                farmTimeZones = headerFarm.farms.associate { it.id to it.timeZoneId },
                notice = syncNotice,
                farmTimeZone = headerFarm.selected?.timeZoneId)
        }
        composable("farms") {
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { farms.load() }
            FarmListScreen(farmState,
                onOpen = { navController.navigate("farm/$it") },
                onCreate = { name, location, zone, latitude, longitude ->
                    farms.create(name, location, zone, latitude, longitude) {
                    navController.navigate("farm/$it")
                } },
                onRequest = farms::requestToJoin,
                onBack = { navController.navigate("account") }, onRetry = { farms.load() },
                onCancelRequest = farms::cancelJoinRequest)
        }
        composable("farm/{farmId}") { entry ->
            farmDestination(entry.arguments?.getString("farmId").orEmpty(), "Overview")
        }
        composable("farm/{farmId}/{section}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val section = safeFarmSection(entry.arguments?.getString("section"))
                .replaceFirstChar { it.uppercase() }
            farmDestination(farmId, section)
        }
        composable("more/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) {
                if (farms.state.value.selected?.id != farmId) farms.loadDetail(farmId)
            }
            val farm = farmState.selected?.takeIf { it.id == farmId }
            val currentQueue = com.smartfarm.mobile.features.syncstatus.syncQueueSummary(
                pending, pendingPhotos, farmId, state.account?.id)
            FarmMoreScreen(farm?.name ?: "Farm", farm?.role, onOpen = { choice ->
                when (choice) {
                    "inventory" -> navController.navigate("inventory/$farmId")
                    "insights" -> navController.navigate("insights/$farmId")
                    "members", "plots", "settings", "weather" ->
                        navController.navigate("farm/$farmId/$choice")
                    "notifications", "sync", "account" -> navController.navigate(choice)
                    "switch" -> showFarmSwitcher = true
                }
            }, pendingSync = currentQueue.total, reviewSync = currentQueue.needsReview)
        }
        composable("tasks/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { tasks.load(farmId) }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                actorId = state.account?.id,
                isOwner = farmState.selected?.id == farmId && farmState.selected?.role == "Owner",
                onBack = { navController.popBackStack() },
                onRetry = { tasks.load(farmId) }, onComplete = tasks::complete, onRead = tasks::read,
                onCreate = tasks::create, onUpdate = tasks::update,
                onOpenCrop = { task ->
                    val cycleId = task.cropCycleId
                    if (cycleId != null && taskState.cycles.any { it.id == cycleId && it.status == 1 }) {
                        when (task.taskType) {
                            5 -> navController.navigate("harvests/$farmId/$cycleId")
                            2 -> navController.navigate("monitor/$farmId/$cycleId?quick=watering")
                            3 -> navController.navigate("monitor/$farmId/$cycleId?quick=fertilizing")
                            4 -> navController.navigate("monitor/$farmId/$cycleId?quick=inspection")
                            else -> navController.navigate("monitor/$farmId/$cycleId")
                        }
                    }
                })
        }
        composable("notifications") {
            val taskState by tasks.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { tasks.load() }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState, false, state.account?.id,
                onBack = { navController.popBackStack() }, onRetry = { tasks.load() },
                onComplete = tasks::complete, onRead = tasks::read)
        }
        composable("crops/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cropState by crops.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { crops.load(farmId) }
            CropScreen(cropState,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.popBackStack() },
                onCreateType = { name, unit, growth, watering ->
                    crops.createType(name, unit, growth, watering)
                },
                onPlan = { plotId, typeId, date, expected ->
                    crops.plan(plotId, typeId, date, expected)
                },
                onUpdatePlan = { cycle, date, expected -> crops.updatePlan(cycle, date, expected) },
                onMonitoring = { cycle -> navController.navigate("monitor/$farmId/${cycle.id}") },
                onQuickMonitoring = { cycle, mode ->
                    navController.navigate("monitor/$farmId/${cycle.id}?quick=$mode")
                },
                onHarvests = { cycle -> navController.navigate("harvests/$farmId/${cycle.id}") },
                onTransition = { cycle, status, planted, reason ->
                    crops.transition(cycle, status, planted, reason)
                }, onRetry = { crops.load(farmId) },
                onPlots = { navController.navigate("farm/$farmId/plots") })
        }
        composable("monitor/{farmId}/{cycleId}?quick={quick}",
            arguments = listOf(navArgument("quick") {
                type = NavType.StringType
                defaultValue = ""
            })) { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val monitoringState by monitoring.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val manager = farmState.selected?.id == farmId &&
                farmState.selected?.role in listOf("Owner", "Manager")
            LaunchedEffect(farmId, cycleId) {
                monitoring.load(farmId, cycleId)
                if (manager) tasks.load(farmId)
            }
            MonitoringScreen(monitoringState,
                onBack = { navController.popBackStack() },
                onActivity = { kind, notes -> monitoring.logActivity(kind, notes) },
                onObservation = { health, growth, notes, photo ->
                    monitoring.observe(health, growth, notes, photo, resolver)
                },
                onUpload = { observationId, uri ->
                    monitoring.uploadPhoto(observationId, uri, resolver)
                },
                onPreview = { monitoring.preview(it) },
                onThumbnail = { monitoring.thumbnail(it) },
                onDismissPreview = { monitoring.dismissPreview() }, pending = pending,
                onSync = { navController.navigate("sync") },
                onRetry = { monitoring.load(farmId, cycleId) },
                quick = entry.arguments?.getString("quick").orEmpty(),
                onIssueObservation = { health, growth, notes, category, severity, photo ->
                    monitoring.observeIssue(health, growth, notes,
                        category, severity, photo, resolver)
                },
                canManageIssues = manager,
                taskOptions = taskState.tasks,
                onFollowUp = { observation, status, taskId ->
                    monitoring.updateFollowUp(observation, status, taskId)
                },
                onActivityWithStock = { type, notes, item, quantity ->
                    monitoring.logActivityWithStock(type, notes, item, quantity)
                })
        }
        composable("inventory/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val state by inventory.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { inventory.load(farmId) }
            InventoryListScreen(state,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate("inventory/$farmId/$it") },
                onCreate = { name, category, unit, opening, reorder ->
                    inventory.create(name, category, unit, opening, reorder)
                }, onRetry = { inventory.load(farmId) })
        }
        composable("inventory/{farmId}/{itemId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val itemId = entry.arguments?.getString("itemId").orEmpty()
            val state by inventory.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, itemId) { inventory.loadItem(farmId, itemId) }
            InventoryItemScreen(state,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.popBackStack() },
                onSave = { name, category, unit, description, reorder ->
                    inventory.update(name, category, unit, description, reorder)
                },
                onArchive = { inventory.archive { navController.popBackStack() } },
                onMovement = { kind, quantity, activityId, reason ->
                    inventory.movement(kind, quantity, activityId, reason)
                },
                onReverse = { transactionId, reason -> inventory.reverse(transactionId, reason) },
                pending = pending, onSync = { navController.navigate("sync") },
                onRetry = { inventory.loadItem(farmId, itemId) },
                farmTimeZone = farmState.selected?.takeIf { it.id == farmId }?.timeZoneId)
        }
        composable("insights/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val insightState by insights.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { insights.load(farmId) }
            FarmInsightsScreen(
                state = insightState,
                farmTimeZone = farmState.selected?.takeIf { it.id == farmId }?.timeZoneId,
                onBack = { navController.popBackStack() },
                onReload = { from, to -> insights.load(farmId, from, to) }
            )
        }
        composable("harvests/{farmId}/{cycleId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val harvestState by harvests.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, cycleId) { harvests.load(farmId, cycleId) }
            HarvestScreen(
                state = harvestState,
                onBack = { navController.popBackStack() },
                onRecord = { quantity, notes -> harvests.record(quantity, notes) },
                onVoid = { harvestId, reason -> harvests.void(harvestId, reason) },
                pending = pending, onSync = { navController.navigate("sync") },
                onRetry = { harvests.load(farmId, cycleId) },
                farmTimeZone = farms.state.collectAsStateWithLifecycle().value.selected
                    ?.takeIf { it.id == farmId }?.timeZoneId
            )
        }
    }
    } // Row
    if (state.account != null && showFarmSwitcher) {
        FarmSwitcherSheet(
            farms = headerFarm.farms, currentFarmId = currentFarmId,
            onSelect = { farmId ->
                if (farmId == currentFarmId) showFarmSwitcher = false
                else requestFarmSwitch(farmId)
            },
            onAllFarms = { requestFarmSwitch("__all__") },
            onDismiss = { showFarmSwitcher = false }
        )
    }
    if (state.account != null && pendingFarmSwitch != null) {
        AlertDialog(
            onDismissRequest = { pendingFarmSwitch = null },
            title = { Text("Leave this screen?") },
            text = { Text("Save any unfinished form details before switching. Saved offline records will remain on this device.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = pendingFarmSwitch
                    if (target != null) completeFarmSwitch(target)
                }) { Text("Leave screen") }
            },
            dismissButton = {
                TextButton(onClick = { pendingFarmSwitch = null }) { Text("Keep working") }
            }
        )
    }
    if (state.account != null && !recordForFarm.isNullOrBlank()) {
        val farmId = requireNotNull(recordForFarm)
        FarmQuickRecordSheet(
            farmId = farmId,
            farmName = headerFarm.selected?.takeIf { it.id == farmId }?.name ?: "Selected farm",
            cropState = cropState,
            onDismiss = { recordForFarm = null; recordAction = null },
            onSelect = { action, cycleId ->
                recordForFarm = null
                recordAction = null
                quickRecordRoute(farmId, action, cycleId)?.let { navController.navigate(it) }
            },
            onBrowseCrops = {
                recordForFarm = null
                recordAction = null
                navController.navigate("crops/$farmId")
            },
            onSupplies = {
                recordForFarm = null
                recordAction = null
                navController.navigate("inventory/$farmId")
            },
            initialAction = recordAction
        )
    }
    } // CompositionLocalProvider
    } // Scaffold
}
