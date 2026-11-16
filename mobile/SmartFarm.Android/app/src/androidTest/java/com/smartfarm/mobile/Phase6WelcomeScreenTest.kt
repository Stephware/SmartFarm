package com.smartfarm.mobile

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class Phase6WelcomeScreenTest {
    @get:Rule val compose = createComposeRule()
    private val preferences = InstrumentationRegistry.getInstrumentation().targetContext
        .getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE)
    private val previousDark = preferences.getBoolean("dark", false)

    @After fun restoreAppearance() { preferences.edit().putBoolean("dark", previousDark).commit() }

    @Test fun lightWelcomeKeepsAllCopyAndActionsAtLargeText() = verifyWelcome(false)
    @Test fun darkWelcomeKeepsAllCopyAndActionsAtLargeText() = verifyWelcome(true)

    private fun verifyWelcome(dark: Boolean) {
        preferences.edit().putBoolean("dark", dark).commit()
        var logins = 0
        var registrations = 0
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme { WelcomeScreen(onLogin = { logins++ }, onRegister = { registrations++ }) }
                }
            }
        }
        for (text in listOf("ONE CONNECTED WORKSPACE", "Welcome to AniGo",
            "Plan crops, record fieldwork, and track harvests in one place.")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertFalse("Welcome hero must show its complete text: $text", layouts.single().hasVisualOverflow)
        }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, logins); assertEquals(1, registrations) }
    }
}
