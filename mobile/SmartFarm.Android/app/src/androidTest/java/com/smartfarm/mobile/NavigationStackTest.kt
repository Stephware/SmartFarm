package com.smartfarm.mobile

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.smartfarm.mobile.navigation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real Navigation Compose stack with the same production transition helpers. */
class NavigationStackTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private fun start() {
        compose.setContent {
            val controller = rememberNavController()
            SideEffect { nav = controller }
            NavHost(controller, startDestination = "welcome") {
                listOf("welcome", "login", "register", "forgot-password", "farms", "account", "sync", "notifications",
                    "onboarding-preview", "farm/{farmId}", "myfarm/{farmId}", "crops/{farmId}", "tasks/{farmId}",
                    "monitor/{farmId}/{cycleId}", "harvests/{farmId}/{cycleId}", "inventory/{farmId}",
                    "inventory/{farmId}/{itemId}", "farm/{farmId}/{section}").forEach { route -> composable(route) {} }
            }
        }
    }
    @Test fun authenticationAndFarmTabsNeverAccumulateOldHistory() {
        start()
        compose.runOnIdle {
            nav.navigate("register"); nav.openAuth("login"); nav.navigate("forgot-password"); nav.backOrParent()
            assertEquals("login", nav.currentDestination?.route)
            nav.authenticationSucceeded()
            assertNull(nav.previousBackStackEntry)
            nav.navigate("farm/first")
            repeat(12) { nav.openWorkspace("first", "myfarm/{farmId}"); nav.openWorkspace("first", "farm/{farmId}") }
            nav.backOrParent()
            assertEquals("farms", nav.currentDestination?.route)
            assertNull(nav.previousBackStackEntry)
            assertFalse(nav.popBackStack("login", false))
            assertFalse(nav.popBackStack("welcome", false))
        }
    }
    @Test fun nestedPagesAndGlobalDestinationsReturnToTheirActualCaller() {
        start()
        compose.runOnIdle {
            nav.authenticationSucceeded(); nav.navigate("farm/first"); nav.openWorkspace("first", "myfarm/{farmId}")
            for (route in listOf("inventory/first", "farm/first/settings", "farm/first/members", "crops/first", "tasks/first")) {
                nav.navigate(route)
                for (global in listOf("notifications", "sync", "account")) {
                    val original = nav.currentBackStackEntry
                    nav.openSecondary(global); nav.backOrParent()
                    assertSame(original, nav.currentBackStackEntry)
                }
                nav.backOrParent()
                assertEquals("myfarm/{farmId}", nav.currentDestination?.route)
            }
            nav.navigate("crops/first")
            for (detail in listOf("monitor/first/crop", "harvests/first/crop")) {
                nav.navigate(detail); nav.backOrParent(); assertEquals("crops/{farmId}", nav.currentDestination?.route)
            }
            nav.navigate("inventory/first"); nav.navigate("inventory/first/item"); nav.backOrParent()
            assertEquals("inventory/{farmId}", nav.currentDestination?.route)
            nav.switchFarm("second")
            assertEquals("second", nav.currentBackStackEntry?.arguments?.getString("farmId"))
            nav.backOrParent(); assertEquals("farms", nav.currentDestination?.route)
            assertFalse(nav.popBackStack("farm/first", false))
        }
    }
    @Test fun secondaryWithoutCallerGetsAUsableParentAndAccountDoesNotLoop() {
        start()
        compose.runOnIdle {
            nav.navigate("inventory/first/item") { popUpTo(nav.graph.id) { inclusive = true } }
            nav.backOrParent(); assertEquals("inventory/{farmId}", nav.currentDestination?.route)
            nav.backOrParent(); assertEquals("farm/{farmId}", nav.currentDestination?.route)
            nav.backOrParent(); assertEquals("farms", nav.currentDestination?.route)
            nav.openSecondary("account"); nav.openSecondary("sync"); nav.openSecondary("account")
            nav.backOrParent(); assertEquals("farms", nav.currentDestination?.route)
            nav.openSecondary("account"); nav.navigate("onboarding-preview"); nav.backOrParent()
            assertEquals("account", nav.currentDestination?.route)
            nav.openFarms(); assertEquals("farms", nav.currentDestination?.route)
        }
    }
}
