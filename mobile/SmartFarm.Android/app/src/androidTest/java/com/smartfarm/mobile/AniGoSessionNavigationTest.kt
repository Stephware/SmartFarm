package com.smartfarm.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfarm.mobile.navigation.navigateToSignedInFarms
import com.smartfarm.mobile.navigation.navigateToSignedOutWelcome
import com.smartfarm.mobile.ui.components.onboarding.AniGoOnboardingScreen
import com.smartfarm.mobile.ui.components.onboarding.anigoReplayExitLabel
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Exercises the real AniGo auth-boundary navigation helpers against a Compose
 * NavHost, including the important case where Welcome was never in the stack.
 */
class AniGoSessionNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loginClearsGuestHistoryAndSignOutClearsAuthenticatedHistory() {
        lateinit var controller: NavHostController
        render(start = "welcome", onController = { controller = it })

        compose.onNodeWithText("Open login").performClick()
        compose.onNodeWithText("Sign in").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("farms", controller.currentBackStackEntry?.destination?.route)
            assertNull("Guest login should not remain behind Farms", controller.previousBackStackEntry)
        }

        compose.onNodeWithText("Open account").performClick()
        compose.onNodeWithText("Sign out").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Open login").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("welcome", controller.currentBackStackEntry?.destination?.route)
            assertNull("Authenticated routes must be removed on sign-out", controller.previousBackStackEntry)
        }
    }

    @Test
    fun directAuthenticatedLaunchCanSignOutWithoutWelcomeInOriginalStack() {
        lateinit var controller: NavHostController
        render(start = "farms", onController = { controller = it })

        compose.onNodeWithText("Open account").performClick()
        compose.onNodeWithText("Sign out").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Open login").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("welcome", controller.currentBackStackEntry?.destination?.route)
            assertNull("A direct Farms session must not retain Account or Farms", controller.previousBackStackEntry)
        }
    }

    @Test
    fun optionalGuideReplayClosesBackToWelcomeOrAccountWithoutAuthTransition() {
        lateinit var controller: NavHostController
        render(start = "welcome", onController = { controller = it })

        compose.onNodeWithText("Explore guide").performClick()
        compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
        compose.onNodeWithText("Close guide").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("welcome", controller.currentBackStackEntry?.destination?.route)
            assertNull("Guest replay should not add a destination after closing", controller.previousBackStackEntry)
        }

        compose.onNodeWithText("Open login").performClick()
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Open account").performClick()
        compose.onNodeWithText("Replay guide").performClick()
        compose.onNodeWithText("Welcome to AniGo!").assertIsDisplayed()
        repeat(4) {
            compose.onNodeWithText("Next →").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Back to My Account").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Replay guide").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals("account", controller.currentBackStackEntry?.destination?.route)
            assertEquals("farms", controller.previousBackStackEntry?.destination?.route)
        }
    }

    private fun render(start: String, onController: (NavHostController) -> Unit) {
        compose.setContent {
            SmartFarmTheme {
                val nav = rememberNavController()
                SideEffect { onController(nav) }
                NavHost(navController = nav, startDestination = start) {
                    composable("welcome") {
                        Column {
                            Button(onClick = { nav.navigate("login") }) { Text("Open login") }
                            Button(onClick = { nav.navigate("onboarding-preview") }) { Text("Explore guide") }
                        }
                    }
                    composable("login") {
                        Button(onClick = { nav.navigateToSignedInFarms() }) { Text("Sign in") }
                    }
                    composable("farms") {
                        Button(onClick = { nav.navigate("account") }) { Text("Open account") }
                    }
                    composable("account") {
                        Column {
                            Button(onClick = { nav.navigateToSignedOutWelcome() }) { Text("Sign out") }
                            Button(onClick = { nav.navigate("onboarding-preview") }) { Text("Replay guide") }
                        }
                    }
                    composable("onboarding-preview") {
                        val authenticated = nav.previousBackStackEntry?.destination?.route == "account"
                        AniGoOnboardingScreen(
                            onExit = { nav.popBackStack() },
                            motionAllowed = false,
                            completionLabel = anigoReplayExitLabel(authenticated),
                            skipLabel = "Close guide"
                        )
                    }
                }
            }
        }
    }
}
