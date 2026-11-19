package com.smartfarm.mobile.navigation

import com.smartfarm.mobile.ui.components.ExitGuard
import com.smartfarm.mobile.ui.components.LocalExitGuard
import com.smartfarm.mobile.ui.components.rememberExitGuard
import com.smartfarm.mobile.ui.components.DiscardChangesDialog
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.smartfarm.mobile.ui.components.LocalFarmHeaderNavigation
import com.smartfarm.mobile.ui.components.LocalSyncProgress
import com.smartfarm.mobile.data.sync.SyncProgress
import com.smartfarm.mobile.ui.components.FarmHeaderNavigation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NamedNavArgument
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import com.smartfarm.mobile.ui.components.LocalPageNavigation
import com.smartfarm.mobile.ui.components.PageNavigation
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
import com.smartfarm.mobile.features.auth.ForgotPasswordScreen
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingScreen
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingStore
import com.smartfarm.mobile.ui.components.onboarding.anigoInitialRoute
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
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmDetailScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel, farms: FarmViewModel, crops: CropViewModel, monitoring: MonitoringViewModel, inventory: InventoryViewModel, harvests: HarvestViewModel, status: StatusViewModel, sync: SyncStatusViewModel, tasks: com.smartfarm.mobile.features.tasks.TaskViewModel, insights: FarmInsightsViewModel) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val currentFarmId = currentEntry?.arguments?.getString("farmId").orEmpty()
    val showFarmTabs = currentRoute in setOf(
        "farm/{farmId}", "myfarm/{farmId}"
    ) && currentFarmId.isNotBlank()
    val appContext = LocalContext.current
    // Stable for this NavHost instance: completing the tour navigates explicitly
    // to Welcome. On subsequent app launches the persisted flag selects Welcome.
    val onboardingStore = remember(appContext) { AniGoOnboardingStore.create(appContext) }
    val initialRoute = remember(onboardingStore) {
        anigoInitialRoute(onboardingStore.isCompleted())
    }
    val resolver = appContext.contentResolver
    val state by auth.state.collectAsStateWithLifecycle()
    val pending by sync.operations.collectAsStateWithLifecycle()
    val pendingPhotos by sync.photos.collectAsStateWithLifecycle()
    val progress by sync.progress.collectAsStateWithLifecycle()
    val scopedProgress = progress.takeIf { it.userId == state.account?.id } ?: SyncProgress()
    val confirmationVersion = scopedProgress.confirmedFarms[currentFarmId] ?: 0
    val headerFarm by farms.state.collectAsStateWithLifecycle()
    val cropState by crops.state.collectAsStateWithLifecycle()
    val accountQueue = syncQueueSummary(pending, pendingPhotos, userId = state.account?.id)
    var showFarmSwitcher by rememberSaveable { mutableStateOf(false) }
    var recordForFarm by rememberSaveable { mutableStateOf<String?>(null) }
    var recordAction by rememberSaveable { mutableStateOf<String?>(null) }
    val drafts = rememberNavigationDrafts()
    val exitGuards = drafts.guards
    val guardOwners = remember { mutableMapOf<String, NavBackStackEntry>() }
    val rootExit = rememberExitGuard()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    var farmScopeUserId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.account?.id) {
        showFarmSwitcher = false
        recordForFarm = null
        recordAction = null
        // Keep the same user's farm context through Activity recreation.
        if (farmScopeUserId != state.account?.id) {
            farms.clear()
            farmScopeUserId = state.account?.id
        }
        if (state.account != null) sync.load() else sync.clear()
    }
    val expandedWorkspace = LocalConfiguration.current.screenWidthDp >= 600
    val navigateWorkspace: (String) -> Unit = { destination ->
        if (destination == "record") {
            recordAction = null
            recordForFarm = currentFarmId
            if (crops.state.value.farmId != currentFarmId) crops.load(currentFarmId)
        } else {
            navController.openWorkspace(currentFarmId, destination)
        }
    }

    fun completeFarmSwitch(target: String) {
        showFarmSwitcher = false
        if (target == "__all__") {
            navController.openFarms()
        } else if (target != currentFarmId) {
            navController.switchFarm(target)
        }
    }
    fun acceptOnSuccess(form: String): () -> Unit {
        return exitGuards[currentEntry?.id]?.acceptAfterSave(form) ?: {}
    }
    fun requestRootChange(action: () -> Unit) {
        keyboard?.hide()
        focus.clearFocus()
        // Farms itself is retained. Check every destination that a root pop can remove,
        // including a draft parked below a global Sync/Account/Notifications page.
        rootExit.update("leaving-stack", exitGuards.any { (id, guard) ->
            drafts.routes[id] != "farms" && guard.dirty
        })
        rootExit.request { action(); drafts.clearLeavingFarms() }
    }
    fun requestFarmSwitch(target: String) {
        showFarmSwitcher = false
        requestRootChange { completeFarmSwitch(target) }
    }

    val openTaskRecord: (String, com.smartfarm.mobile.data.remote.api.TaskDto) -> Unit = { farmId, task ->
        val action = when (task.taskType) {
            2 -> "watering"; 3 -> "fertilizing"; 4 -> "inspection"; 5 -> "harvest"; else -> null
        }
        val route = action?.let { quickRecordRoute(farmId, it, task.cropCycleId, task.id) }
        navController.navigate(route ?: "tasks/$farmId")
    }

    // Both home and dedicated farm subpages use the existing data/actions.
    val farmDestination: @Composable (String, String) -> Unit = { farmId, section ->
        val farmState by farms.state.collectAsStateWithLifecycle()
        val homeTasks by tasks.state.collectAsStateWithLifecycle()
        val homeCrops by crops.state.collectAsStateWithLifecycle()
        LaunchedEffect(farmId, confirmationVersion) {
            if (farms.state.value.selected?.id != farmId) farms.loadDetail(farmId)
            if (confirmationVersion > 0 || tasks.state.value.farmId != farmId) tasks.load(farmId)
            if (confirmationVersion > 0 || crops.state.value.farmId != farmId) crops.load(farmId)
        }
        FarmDetailScreen(farmState,
            onBack = {
                if (section == "Overview") {
                    navController.openFarms()
                } else navController.backOrParent()
            },
            onSaveFarm = { name, location, zone, latitude, longitude ->
                farms.saveFarm(name, location, zone, latitude, longitude) {} },
            onArchiveFarm = { farms.archiveFarm { navController.openFarms() } },
            onAddPlot = { name, area -> farms.addPlot(name, area, acceptOnSuccess("new-plot")) },
            onSavePlot = { plot, name, area -> farms.savePlot(plot, name, area) },
            onArchivePlot = { plot -> farms.archivePlot(plot) },
            onCrops = { navController.navigate("crops/$farmId") },
            onInventory = { navController.navigate("inventory/$farmId") },
            onDecision = farms::decideJoinRequest,
            onChangeRole = { member, role -> farms.changeRole(member, role) },
            onRemoveMember = { farms.remove(it) },
            onTasks = { navController.navigate("tasks/$farmId") },
            onTask = { openTaskRecord(farmId, it) },
            onInsights = { navController.navigate("insights/$farmId") },
            onRetry = { farms.loadDetail(farmId) },
            onRegenerateCode = farms::regenerateJoinCode,
            homeTasks = homeTasks, homeCrops = homeCrops, actorId = state.account?.id,
            section = section,
            onWeather = { navController.navigate("farm/$farmId/weather") },
            onPlots = { navController.navigate("farm/$farmId/plots") },
            onSync = { navController.openSecondary("sync") },
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

    // Each destination owns its header and system Back, including during transitions.
    fun NavGraphBuilder.page(route: String, arguments: List<NamedNavArgument> = emptyList(),
        content: @Composable (NavBackStackEntry) -> Unit) {
        composable(route, arguments = arguments) { entry ->
            val root = route in setOf("welcome", "onboarding", "onboarding-preview", "farms",
                "farm/{farmId}", "myfarm/{farmId}", "more/{farmId}")
            val previous = navController.previousBackStackEntry
            val title = navigationTitle(route, entry.arguments?.getString("section"), entry.arguments?.getString("quick"))
            val parent = previous?.let { navigationTitle(it.destination.route,
                it.arguments?.getString("section"), it.arguments?.getString("quick")) }
                ?: navigationTitle(navigationParent(route, entry.arguments?.getString("farmId").orEmpty()))
            val guard = rememberExitGuard(exitGuards[entry.id])
            DisposableEffect(entry.id, guard) {
                exitGuards[entry.id] = guard
                drafts.routes[entry.id] = route
                if (guardOwners.put(entry.id, entry) == null) {
                    entry.lifecycle.addObserver(object : androidx.lifecycle.LifecycleEventObserver {
                        override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: androidx.lifecycle.Lifecycle.Event) {
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY) {
                                drafts.remove(entry.id)
                                guardOwners.remove(entry.id)
                                source.lifecycle.removeObserver(this)
                            }
                        }
                    })
                }
                // A stopped back-stack entry still owns its draft; retain its guard.
                onDispose { }
            }
            val back = {
                keyboard?.hide()
                focus.clearFocus()
                guard.request {
                    if (navController.previousBackStackEntry == null && navigationParent(route,
                        entry.arguments?.getString("farmId").orEmpty()) == null) activity?.finish()
                    else navController.backOrParent()
                }
            }
            BackHandler(enabled = route !in setOf("welcome", "onboarding", "onboarding-preview") &&
                (guard.dirty || previous != null || navigationParent(route, entry.arguments?.getString("farmId").orEmpty()) != null)) { back() }
            CompositionLocalProvider(LocalExitGuard provides guard,
                LocalPageNavigation provides PageNavigation(title, "Back to $parent", if (root) null else back)) {
                content(entry)
                DiscardChangesDialog(guard)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (state.account != null && showFarmTabs && !expandedWorkspace) {
                FarmWorkspaceBar(selectedRoute = currentRoute.orEmpty(), onNavigate = navigateWorkspace)
            }
        }
    ) { workspacePadding ->
    CompositionLocalProvider(LocalSyncProgress provides scopedProgress,
        LocalFarmHeaderNavigation provides if (state.account == null) null else
        FarmHeaderNavigation(headerFarm.selected?.takeIf { it.id == currentFarmId }?.name,
            onFarms = {
                if (currentFarmId.isBlank()) {
                    requestRootChange { navController.openFarms() }
                } else showFarmSwitcher = true
            },
            onNotifications = { navController.openSecondary("notifications") },
            queuedCount = accountQueue.total,
            reviewCount = accountQueue.needsReview,
            onSync = { navController.openSecondary("sync") })) {
    Row(Modifier.fillMaxSize().padding(workspacePadding)) {
    if (state.account != null && showFarmTabs && expandedWorkspace) {
        FarmWorkspaceBar(selectedRoute = currentRoute.orEmpty(), onNavigate = navigateWorkspace, expanded = true)
    }
    NavHost(
        navController = navController,
        startDestination = initialRoute,
        modifier = Modifier.weight(1f)
    ) {
        page("onboarding") {
            AniGoOnboardingScreen(onExit = {
                onboardingStore.markCompleted()
                navController.navigate("welcome") {
                    popUpTo("onboarding") { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        page("welcome") {
            val statusState by status.state.collectAsStateWithLifecycle()
            WelcomeScreen(
                status = statusState,
                onCheckBackend = { status.check() },
                onLogin = { navController.navigate("login") },
                onRegister = { navController.navigate("register") },
                onPreviewGuide = { navController.navigate("onboarding-preview") { launchSingleTop = true } })
        }
        page("onboarding-preview") {
            AniGoOnboardingScreen(onExit = {
                if (navController.previousBackStackEntry == null && state.account != null) navController.openFarms()
                else navController.backOrParent()
            })
        }
        page("login") {
            LoginScreen(state,
                onSubmit = { email, password ->
                    auth.login(email, password) {
                        drafts.clear()
                        navController.authenticationSucceeded()
                    }
                },
                onRegister = { exitGuards[currentEntry?.id]?.request { navController.openAuth("register") } },
                onForgotPassword = { navController.navigate("forgot-password") })
        }
        page("forgot-password") {
            ForgotPasswordScreen(onLogin = {
                if (!navController.popBackStack("login", inclusive = false)) {
                    navController.navigate("login") { launchSingleTop = true }
                }
            })
        }
        page("register") {
            RegisterScreen(state,
                onSubmit = { name, email, password ->
                    auth.register(name, email, password) {
                        navController.openAuth("login")
                    }
                },
                onLogin = { exitGuards[currentEntry?.id]?.request { navController.openAuth("login") } })
        }
        page("account") {
            AccountScreen(state, onLogout = {
                auth.logout()
                drafts.clear()
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
                navController.navigate("welcome") { popUpTo(navController.graph.id) { inclusive = true }; launchSingleTop = true }
            }, onFarms = { requestRootChange { navController.openFarms() } },
                onSync = { navController.openSecondary("sync") },
                onNotifications = { navController.openSecondary("notifications") },
                pendingSyncCount = accountQueue.total,
                onReplayGuide = {
                    navController.navigate("onboarding-preview") { launchSingleTop = true }
                })
        }
        page("sync") {
            val pending by sync.operations.collectAsStateWithLifecycle()
            val photos by sync.photos.collectAsStateWithLifecycle()
            val preview by sync.stockPreview.collectAsStateWithLifecycle()
            val syncError by sync.error.collectAsStateWithLifecycle()
            val syncNotice by sync.notice.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { sync.load() }
            SyncStatusScreen(pending, photos, preview, syncError,
                onBack = { navController.backOrParent() },
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
                farmTimeZone = headerFarm.selected?.timeZoneId,
                onAccount = { navController.openSecondary("account") })
        }
        page("farms") {
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { farms.load() }
            FarmListScreen(farmState,
                onOpen = { navController.navigate("farm/$it") },
                onCreate = { name, location, zone, latitude, longitude ->
                    val saved = acceptOnSuccess("farm-create")
                    farms.create(name, location, zone, latitude, longitude) {
                    saved()
                    navController.navigate("farm/$it")
                } },
                onRequest = { farms.requestToJoin(it, acceptOnSuccess("farm-join")) },
                onBack = { navController.openSecondary("account") }, onRetry = { farms.load() },
                onCancelRequest = farms::cancelJoinRequest)
        }
        page("farm/{farmId}") { entry ->
            farmDestination(entry.arguments?.getString("farmId").orEmpty(), "Overview")
        }
        page("farm/{farmId}/{section}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val section = safeFarmSection(entry.arguments?.getString("section"))
                .replaceFirstChar { it.uppercase() }
            farmDestination(farmId, section)
        }
        page("myfarm/{farmId}") { entry ->
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
                    "crops" -> navController.navigate("crops/$farmId")
                    "inventory" -> navController.navigate("inventory/$farmId")
                    "insights" -> navController.navigate("insights/$farmId")
                    "members", "plots", "settings", "weather" ->
                        navController.navigate("farm/$farmId/$choice")
                    "notifications", "sync", "account" -> navController.openSecondary(choice)
                    "switch" -> showFarmSwitcher = true
                }
            }, pendingSync = currentQueue.total, reviewSync = currentQueue.needsReview)
        }
        page("more/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            LaunchedEffect(farmId) {
                navController.navigate("myfarm/$farmId") {
                    popUpTo("more/{farmId}") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        page("tasks/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, confirmationVersion) { tasks.load(farmId) }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState.takeIf { it.farmId == farmId }
                ?: com.smartfarm.mobile.features.tasks.TaskUiState(farmId = farmId, busy = true),
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                actorId = state.account?.id,
                isOwner = farmState.selected?.id == farmId && farmState.selected?.role == "Owner",
                onBack = { navController.backOrParent() },
                onRetry = { tasks.load(farmId) }, onComplete = tasks::complete, onRead = tasks::read,
                onCreate = tasks::create, onUpdate = tasks::update,
                onOpenCrop = { task ->
                    val cycleId = task.cropCycleId
                    if (cycleId != null && taskState.cycles.any { it.id == cycleId && it.status == 1 }) {
                        openTaskRecord(farmId, task)
                    }
                })
        }
        page("notifications") {
            val taskState by tasks.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { tasks.load() }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState.takeIf { it.farmId == null }
                ?: com.smartfarm.mobile.features.tasks.TaskUiState(busy = true), false, state.account?.id,
                onBack = { navController.backOrParent() }, onRetry = { tasks.load() },
                onComplete = tasks::complete, onRead = tasks::read)
        }
        page("crops/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cropState by crops.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { crops.load(farmId) }
            CropScreen(cropState,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.backOrParent() },
                onCreateType = { name, unit, growth, watering ->
                    crops.createType(name, unit, growth, watering, acceptOnSuccess("crop-type"))
                },
                onPlan = { plotId, typeId, date, expected ->
                    crops.plan(plotId, typeId, date, expected, acceptOnSuccess("crop-plan"))
                },
                onUpdatePlan = { cycle, date, expected -> crops.updatePlan(cycle, date, expected) },
                onMonitoring = { cycle -> navController.navigate("monitor/$farmId/${cycle.id}") },
                onQuickMonitoring = { cycle, mode ->
                    navController.navigate("monitor/$farmId/${cycle.id}?quick=$mode")
                },
                onHarvests = { cycle -> navController.navigate("harvests/$farmId/${cycle.id}") },
                onTransition = { cycle, status, planted, reason ->
                    crops.transition(cycle, status, planted, reason, acceptOnSuccess("cycle-${cycle.id}"))
                }, onRetry = { crops.load(farmId) },
                onPlots = { navController.navigate("farm/$farmId/plots") })
        }
        page("monitor/{farmId}/{cycleId}?quick={quick}&taskId={taskId}",
            arguments = listOf(navArgument("quick") {
                type = NavType.StringType
                defaultValue = ""
            }, navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null })) { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val taskId = entry.arguments?.getString("taskId")
            val monitoringState by monitoring.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val manager = farmState.selected?.id == farmId &&
                farmState.selected?.role in listOf("Owner", "Manager")
            LaunchedEffect(farmId, cycleId, confirmationVersion) {
                monitoring.state.first { !it.busy || it.farmId != farmId || it.cycleId != cycleId }
                monitoring.load(farmId, cycleId)
                if (crops.state.value.farmId != farmId) crops.load(farmId)
                if (manager) tasks.load(farmId)
            }
            MonitoringScreen(monitoringState.takeIf { it.farmId == farmId && it.cycleId == cycleId }
                ?: com.smartfarm.mobile.features.monitoring.MonitoringUiState(farmId = farmId, cycleId = cycleId, busy = true),
                onBack = { navController.backOrParent() },
                onActivity = { kind, notes -> monitoring.logActivity(kind, notes, taskId) },
                onObservation = { health, growth, notes, photo ->
                    monitoring.observe(health, growth, notes, photo, resolver)
                },
                onUpload = { observationId, uri ->
                    monitoring.uploadPhoto(observationId, uri, resolver)
                },
                onPreview = { monitoring.preview(it) },
                onThumbnail = { monitoring.thumbnail(it) },
                onDismissPreview = { monitoring.dismissPreview() }, pending = pending,
                onSync = { navController.openSecondary("sync") },
                onRetry = { monitoring.load(farmId, cycleId) },
                quick = entry.arguments?.getString("quick").orEmpty(), linkedTask = !taskId.isNullOrBlank(),
                cropLabel = cropState.takeIf { it.farmId == farmId }?.cycles
                    ?.firstOrNull { it.id == cycleId }?.let { "${it.cropTypeName} · ${it.plotName}" },
                onIssueObservation = { health, growth, notes, category, severity, photo ->
                    monitoring.observeIssue(health, growth, notes,
                        category, severity, photo, resolver)
                },
                canManageIssues = manager,
                taskOptions = taskState.takeIf { it.farmId == farmId && !it.busy && it.error == null }?.tasks.orEmpty(),
                onFollowUp = { observation, status, taskId ->
                    monitoring.updateFollowUp(observation, status, taskId)
                },
                onActivityWithStock = { type, notes, item, quantity ->
                    monitoring.logActivityWithStock(type, notes, item, quantity, taskId)
                })
        }
        page("inventory/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val state by inventory.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, confirmationVersion) {
                inventory.state.first { !it.busy || it.farmId != farmId }
                inventory.load(farmId)
            }
            InventoryListScreen(state,
                canManage = !state.accessDenied && farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.backOrParent() },
                onOpen = { navController.navigate("inventory/$farmId/$it") },
                onCreate = { name, category, unit, opening, reorder ->
                    inventory.create(name, category, unit, opening, reorder, acceptOnSuccess("new-supply"))
                }, onRetry = { inventory.load(farmId) })
        }
        page("inventory/{farmId}/{itemId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val itemId = entry.arguments?.getString("itemId").orEmpty()
            val state by inventory.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, itemId, confirmationVersion) {
                inventory.state.first { !it.busy || it.farmId != farmId || it.itemId != itemId }
                inventory.loadItem(farmId, itemId)
            }
            InventoryItemScreen(state,
                canManage = !state.accessDenied && farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.backOrParent() },
                onSave = { name, category, unit, description, reorder ->
                    inventory.update(name, category, unit, description, reorder)
                },
                onArchive = { inventory.archive { navController.backOrParent() } },
                onMovement = { kind, quantity, activityId, reason ->
                    inventory.movement(kind, quantity, activityId, reason)
                },
                onReverse = { transactionId, reason -> inventory.reverse(transactionId, reason) },
                pending = pending, onSync = { navController.openSecondary("sync") },
                onRetry = { inventory.loadItem(farmId, itemId) },
                farmTimeZone = farmState.selected?.takeIf { it.id == farmId }?.timeZoneId)
        }
        page("insights/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val insightState by insights.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, confirmationVersion) {
                if (farms.state.value.selected?.id != farmId) farms.loadDetail(farmId)
                val filters = insights.state.value.takeIf { it.farmId == farmId }
                insights.load(farmId, filters?.from, filters?.to)
            }
            FarmInsightsScreen(
                state = insightState,
                farmTimeZone = farmState.selected?.takeIf { it.id == farmId }?.timeZoneId,
                onBack = { navController.backOrParent() },
                onReload = { from, to -> insights.load(farmId, from, to) }
            )
        }
        page("harvests/{farmId}/{cycleId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val harvestState by harvests.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, cycleId, confirmationVersion) {
                harvests.state.first { !it.busy || it.farmId != farmId || it.cycleId != cycleId }
                harvests.load(farmId, cycleId)
            }
            HarvestScreen(
                state = harvestState,
                onBack = { navController.backOrParent() },
                onRecord = { quantity, notes -> harvests.record(quantity, notes) },
                onVoid = { harvestId, reason -> harvests.void(harvestId, reason) },
                pending = pending, onSync = { navController.openSecondary("sync") },
                onRetry = { harvests.load(farmId, cycleId) },
                farmTimeZone = farms.state.collectAsStateWithLifecycle().value.selected
                    ?.takeIf { it.id == farmId }?.timeZoneId
            )
        }
    }
    } // Row
    DiscardChangesDialog(rootExit)
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
