package com.smartfarm.mobile.navigation

import androidx.navigation.NavController

internal fun NavController.backOrParent() {
    val entry = currentBackStackEntry ?: return
    val parent = navigationParent(entry.destination.route, entry.arguments?.getString("farmId").orEmpty())
    // Do not pop the last destination: Navigation would leave a blank host.
    if (previousBackStackEntry != null && popBackStack()) return
    if (parent != null) navigate(parent) {
        popUpTo(entry.destination.id) { inclusive = true }
        launchSingleTop = true
    }
}

internal fun NavController.openFarms() {
    if (currentDestination?.route == "farms") return
    if (popBackStack("farms", inclusive = false)) return
    navigate("farms") { popUpTo(graph.id) { inclusive = true }; launchSingleTop = true }
}

/** Auth alternatives replace one another; successful login removes the entire auth history. */
internal fun NavController.openAuth(route: String) {
    if (popBackStack(route, inclusive = false)) return
    navigate(route) { popUpTo("welcome"); launchSingleTop = true }
}

internal fun NavController.openWorkspace(farmId: String, destination: String) {
    val home = "farm/$farmId"
    if (runCatching { getBackStackEntry(home) }.isFailure) {
        openFarms()
        navigate(home) { launchSingleTop = true }
    }
    navigate(destination.replace("{farmId}", farmId)) {
        popUpTo(home)
        launchSingleTop = true
    }
}

internal fun NavController.switchFarm(farmId: String) {
    openFarms()
    navigate("farm/$farmId") { launchSingleTop = true }
}

internal fun NavController.authenticationSucceeded() {
    navigate("farms") { popUpTo(graph.id) { inclusive = true }; launchSingleTop = true }
}

internal fun NavController.openSecondary(route: String) {
    if (currentDestination?.route == route) return
    if (popBackStack(route, inclusive = false)) return
    navigate(route) { launchSingleTop = true }
}
