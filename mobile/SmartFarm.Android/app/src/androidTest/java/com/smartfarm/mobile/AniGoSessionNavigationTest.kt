package com.smartfarm.mobile

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

    private fun render(start: String, onController: (NavHostController) -> Unit) {
        compose.setContent {
            SmartFarmTheme {
                val nav = rememberNavController()
                SideEffect { onController(nav) }
                NavHost(navController = nav, startDestination = start) {
                    composable("welcome") {
                        Button(onClick = { nav.navigate("login") }) { Text("Open login") }
                    }
                    composable("login") {
                        Button(onClick = { nav.navigateToSignedInFarms() }) { Text("Sign in") }
                    }
                    composable("farms") {
                        Button(onClick = { nav.navigate("account") }) { Text("Open account") }
                    }
                    composable("account") {
                        Button(onClick = { nav.navigateToSignedOutWelcome() }) { Text("Sign out") }
                    }
                }
            }
        }
    }
}
