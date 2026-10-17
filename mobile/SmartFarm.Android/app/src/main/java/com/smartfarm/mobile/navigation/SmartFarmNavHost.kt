package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthViewModel
import com.smartfarm.mobile.features.auth.LoginScreen
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.components.WelcomeScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel) {
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
                navController.navigate("welcome") { popUpTo("welcome") { inclusive = true } }
            })
        }
    }
}

