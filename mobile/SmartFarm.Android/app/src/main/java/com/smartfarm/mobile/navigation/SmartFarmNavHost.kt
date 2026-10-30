package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.smartfarm.mobile.features.syncstatus.SyncStatusViewModel
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmDetailScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel, farms: FarmViewModel, crops: CropViewModel, monitoring: MonitoringViewModel, inventory: InventoryViewModel, harvests: HarvestViewModel, status: StatusViewModel, sync: SyncStatusViewModel, tasks: com.smartfarm.mobile.features.tasks.TaskViewModel) {
    val navController = rememberNavController()
    val appContext = LocalContext.current
    val resolver = appContext.contentResolver
    val state by auth.state.collectAsStateWithLifecycle()
    val pending by sync.operations.collectAsStateWithLifecycle()
    LaunchedEffect(state.account?.id) {
        farms.clear()
        if (state.account != null) sync.load()
    }
    NavHost(navController = navController, startDestination = "welcome") {
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
                sync.clear()
                tasks.clear()
                navController.navigate("welcome") { popUpTo("welcome") { inclusive = true } }
            }, onFarms = { navController.navigate("farms") },
                onSync = { navController.navigate("sync") },
                onNotifications = { navController.navigate("notifications") })
        }
        composable("sync") {
            val pending by sync.operations.collectAsStateWithLifecycle()
            val photos by sync.photos.collectAsStateWithLifecycle()
            val preview by sync.stockPreview.collectAsStateWithLifecycle()
            val syncError by sync.error.collectAsStateWithLifecycle()
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
                onSyncNow = { sync.syncNow() })
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
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { farms.loadDetail(farmId) }
            FarmDetailScreen(farmState,
                onBack = { navController.popBackStack() },
                onSaveFarm = { name, location, zone, latitude, longitude ->
                    farms.saveFarm(name, location, zone, latitude, longitude) {} },
                onArchiveFarm = { farms.archiveFarm { navController.popBackStack() } },
                onAddPlot = { name, area -> farms.addPlot(name, area) },
                onSavePlot = { plot, name, area -> farms.savePlot(plot, name, area) },
                onArchivePlot = { plot -> farms.archivePlot(plot) },
                onCrops = { navController.navigate("crops/$farmId") },
                onInventory = { navController.navigate("inventory/$farmId") },
                onDecision = farms::decideJoinRequest,
                onChangeRole = { member, role -> farms.changeRole(member, role) },
                onRemoveMember = { farms.remove(it) },
                onTasks = { navController.navigate("tasks/$farmId") }, onRetry = { farms.loadDetail(farmId) },
                onRegenerateCode = farms::regenerateJoinCode)
        }
        composable("tasks/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { tasks.load(farmId) }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState,
                canManage = farmState.selected?.id == farmId && farmState.selected?.role in listOf("Owner", "Manager"),
                actorId = state.account?.id, onBack = { navController.popBackStack() },
                onRetry = { tasks.load(farmId) }, onComplete = tasks::complete, onRead = tasks::read)
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
                }, onRetry = { crops.load(farmId) })
        }
        composable("monitor/{farmId}/{cycleId}?quick={quick}",
            arguments = listOf(navArgument("quick") {
                type = NavType.StringType
                defaultValue = ""
            })) { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val monitoringState by monitoring.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, cycleId) { monitoring.load(farmId, cycleId) }
            MonitoringScreen(monitoringState,
                onBack = { navController.popBackStack() },
                onActivity = { kind, notes -> monitoring.logActivity(kind, notes) },
                onObservation = { health, growth, notes, photo ->
                    monitoring.observe(health, growth, notes, photo, resolver)
                },
                onUpload = { observationId, uri ->
                    monitoring.uploadPhoto(observationId, uri, resolver)
                },
                onPreview = { monitoring.preview(it) }, pending = pending,
                onSync = { navController.navigate("sync") },
                onRetry = { monitoring.load(farmId, cycleId) },
                quick = entry.arguments?.getString("quick").orEmpty())
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
                onRetry = { inventory.loadItem(farmId, itemId) })
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
                onRetry = { harvests.load(farmId, cycleId) }
            )
        }
    }
}






