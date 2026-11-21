package com.smartfarm.mobile.navigation

import androidx.navigation.NavHostController

/**
 * Auth boundaries must clear the entire previous session, regardless of
 * whether Welcome was ever placed on the back stack.
 *
 * Unlike popUpTo("welcome"), the NavHost graph remains on the stack even
 * when the first-run Onboarding or Welcome destination has been removed.
 */
internal fun NavHostController.navigateToSignedInFarms() {
    navigate("farms") {
        popUpTo(graph.id) { inclusive = false }
        launchSingleTop = true
    }
}

internal fun NavHostController.navigateToSignedOutWelcome() {
    navigate("welcome") {
        popUpTo(graph.id) { inclusive = false }
        launchSingleTop = true
    }
}
