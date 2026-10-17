package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthViewModel
import com.smartfarm.mobile.features.auth.LoginScreen
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.features.farms.FarmViewModel
import com.smartfarm.mobile.features.crops.CropViewModel
import com.smartfarm.mobile.features.crops.CropScreen
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmDetailScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel, farms: FarmViewModel, crops: CropViewModel) {
    val navController = rememberNavController()
    val state by auth.state.collectAsStateWithLifecycle()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onLogin = { navController.navigate("login") },
                onRegister = { navController.navigate("register") })
        }
        composable("login") {
            LoginScreen(state,
                onSubmit = { email, password ->
                    auth.login(email, password) {
                        navController.navigate("account") { popUpTo("welcome") }
                    }
                },
                onRegister = { navController.navigate("register") })
        }
        composable("register") {
            RegisterScreen(state,
                onSubmit = { name, email, password, confirm ->
                    auth.register(name, email, password, confirm) {
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
                navController.navigate("welcome") { popUpTo("welcome") { inclusive = true } }
            }, onFarms = { navController.navigate("farms") })
        }
        composable("farms") {
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { farms.load() }
            FarmListScreen(farmState,
                onOpen = { navController.navigate("farm/$it") },
                onCreate = { name, location -> farms.create(name, location) {
                    navController.navigate("farm/$it")
                } },
                onAccept = { token -> farms.acceptInvitation(token) {
                    navController.navigate("farm/$it")
                } },
                onBack = { navController.popBackStack() })
        }
        composable("farm/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { farms.loadDetail(farmId) }
            FarmDetailScreen(farmState,
                onBack = { navController.popBackStack() },
                onSaveFarm = { name, location -> farms.saveFarm(name, location) {} },
                onArchiveFarm = { farms.archiveFarm { navController.popBackStack() } },
                onAddPlot = { name, area -> farms.addPlot(name, area) },
                onSavePlot = { plot, name, area -> farms.savePlot(plot, name, area) },
                onArchivePlot = { plot -> farms.archivePlot(plot) },
                onCrops = { navController.navigate("crops/$farmId") },
                onInvite = { email, role -> farms.invite(email, role) },
                onChangeRole = { member, role -> farms.changeRole(member, role) },
                onRemoveMember = { farms.remove(it) })
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
                onTransition = { cycle, status, planted, reason ->
                    crops.transition(cycle, status, planted, reason)
                })
        }
    }
}



