package com.smartfarm.mobile.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.ui.components.DateInput
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class PhaseSevenAppearanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferences = context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE)
    private val previous = preferences.getBoolean("dark", false)
    @After fun restoreAppearance() { preferences.edit().putBoolean("dark", previous).commit() }

    @Test fun accountSwitchUpdatesAndPersistsTheActualTheme() {
        preferences.edit().putBoolean("dark", false).commit()
        var background = Color.Unspecified
        compose.setContent { SmartFarmTheme {
            val current = androidx.compose.material3.MaterialTheme.colorScheme.background
            SideEffect { background = current }
            AccountScreen(AuthUiState(), {}, {}, {})
        } }
        compose.onNodeWithContentDescription("Dark theme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(DarkColors.background, background)
            assertEquals(true, preferences.getBoolean("dark", false))
        }
        compose.onNodeWithContentDescription("Dark theme").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(LightColors.background, background)
            assertEquals(false, preferences.getBoolean("dark", true))
        }
    }

    @Test fun nativeDatePickerUsesDarkSurfaceAndStillConfirmsIsoDate() {
        preferences.edit().putBoolean("dark", true).commit()
        var selected = ""
        compose.setContent { SmartFarmTheme { DateInput("2026-11-16", "Due date", { selected = it }) } }
        // A resumed host can still be behind a system panel or launcher window.
        androidx.test.uiautomator.UiDevice.getInstance(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()).pressHome()
        compose.activityRule.scenario.onActivity { host ->
            // persistentId also supports the app's API 26 test hosts.
            host.getSystemService(android.app.ActivityManager::class.java).appTasks
                .single { it.taskInfo.persistentId == host.taskId }.moveToFront()
        }
        compose.waitUntil(5_000) {
            var focused = false
            compose.activityRule.scenario.onActivity { focused = it.hasWindowFocus() }
            focused
        }
        compose.onNodeWithContentDescription("Due date:", substring = true).performClick()
        var open = true
        try {
            Espresso.onView(withClassName(equalTo("android.widget.DatePicker"))).inRoot(isDialog())
                .check { view, error ->
                    if (error != null) throw error
                    val root = view.rootView
                    val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                    root.draw(Canvas(bitmap))
                    val output = File(context.filesDir, "phase7-review").apply { mkdirs() }
                    File(output, "native-date-picker.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                    val colors = view.context.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
                    try { assertEquals("App-owned picker must follow the selected dark mode",
                        DarkColors.surface.toArgb(), colors.getColor(0, 0)) }
                    finally { colors.recycle() }
                }
            Espresso.onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click())
            open = false
            compose.runOnIdle { assertEquals("2026-11-16", selected) }
        } finally {
            if (open) Espresso.pressBackUnconditionally()
        }
    }
}
