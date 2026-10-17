package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfarm.mobile.ui.components.WelcomeScreen

@Composable
fun SmartFarmNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") { WelcomeScreen() }
    }
}
