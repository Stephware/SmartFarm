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
import org.junit.Assert.assertTrue
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
        for (text in listOf("YOUR FARM, ORGANIZED", "Your farm starts here.",
            "Plan crops, record fieldwork and track harvests in one place.",
            "Get started", "Manage your own farm or join an existing team with one account.")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("Welcome must show every line: $text", layout.didOverflowHeight)
            assertEquals("Welcome must show the complete copy: $text", text.length,
                layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
            // A short label's paragraph can retain the wider measurement constraint.
            // Check the rendered lines against the actual text box, not paragraph width.
            for (line in 0 until layout.lineCount) {
                assertTrue("Welcome text must fit horizontally: $text (line $line)",
                    layout.getLineLeft(line) >= 0f && layout.getLineRight(line) <= layout.size.width)
            }
        }
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, logins); assertEquals(1, registrations) }
    }
}
