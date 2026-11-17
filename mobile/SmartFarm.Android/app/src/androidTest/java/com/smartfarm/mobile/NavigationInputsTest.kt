package com.smartfarm.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.monitoring.MonitoringUiState
import com.smartfarm.mobile.ui.components.DateInput
import com.smartfarm.mobile.ui.components.TimeZoneInput
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import com.smartfarm.mobile.features.farms.FarmDetailScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.data.remote.dto.PlotDto
import java.math.BigDecimal
import com.smartfarm.mobile.features.tasks.TaskScreen
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.data.remote.api.TaskDto
import androidx.activity.ComponentActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.assertion.ViewAssertions.matches
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationInputsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun datePickerConfirmsAnIsoDateForTheExistingApi() {
        val initialDate = "2026-10-26"
        val selectedDate = "2027-10-26"
        var selected = ""
        val formatted = java.time.LocalDate.parse(initialDate).format(
            java.time.format.DateTimeFormatter.ofLocalizedDate(
                java.time.format.FormatStyle.MEDIUM))
        compose.setContent { SmartFarmTheme {
            DateInput(initialDate, "Planting date", { selected = it })
        } }
        // Compose injects touches directly into its View, even before window focus.
        // Native dialogs need the host to be foreground before that injected tap.
        try {
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
            compose.onNodeWithContentDescription(
                "Planting date: $formatted. Choose date").assertIsDisplayed().performClick()
            // This is an in-app framework dialog: Espresso synchronizes its focused
            // View root without starting a separate accessibility automation session.
            onView(withText("2026")).inRoot(isDialog())
                .check(matches(isDisplayed())).perform(click())
            onView(withText("2027")).inRoot(isDialog())
                .check(matches(isDisplayed())).perform(click())
            compose.runOnIdle { assertEquals("", selected) }
            onView(withId(android.R.id.button1)).inRoot(isDialog())
                .check(matches(isDisplayed())).perform(click())
            compose.runOnIdle { assertEquals(selectedDate, selected) }
        } catch (failure: Throwable) {
            // Preserve the failed window, rather than guessing why its controls vanished.
            runCatching {
                val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null),
                    "date-picker-failure").apply { mkdirs() }
                val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
                device.takeScreenshot(java.io.File(directory, "screen.png"))
                device.dumpWindowHierarchy(java.io.File(directory, "hierarchy.xml"))
                for (service in listOf("window", "power")) {
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(
                        instrumentation.uiAutomation.executeShellCommand("dumpsys $service")
                    ).use { input -> java.io.File(directory, "$service.txt").outputStream().use(input::copyTo) }
                }
                instrumentation.runOnMainSync {
                    java.io.File(directory, "roots.txt").writeText(
                        android.view.inspector.WindowInspector.getGlobalWindowViews().joinToString("\n") {
                            "$it shown=${it.isShown} focus=${it.hasWindowFocus()} visibility=${it.windowVisibility}"
                        })
                }
            }.exceptionOrNull()?.let(failure::addSuppressed)
            runCatching { androidx.test.espresso.Espresso.pressBackUnconditionally() }
                .exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
    }

    @Test fun timeZonesCanBeFoundByCity() {
        var selected = ""
        compose.setContent {
            SmartFarmTheme {
                var value by remember { mutableStateOf("") }
                androidx.compose.foundation.layout.Column { TimeZoneInput(value, { value = it; selected = it }) }
            }
        }
        compose.onNodeWithText("Choose time zone").performClick()
        compose.onNodeWithText("Search city or region").performTextInput("Manila")
        compose.onNodeWithText("Asia/Manila").performClick()
        compose.runOnIdle { assertEquals("Asia/Manila", selected) }
    }

    @Test fun activityChoicesExposeTheirSelectedState() {
        compose.setContent {
            SmartFarmTheme { MonitoringScreen(MonitoringUiState(canRecord = true), {}, { _, _ -> },
                { _, _, _, _ -> }, { _, _ -> }, {}, { byteArrayOf() }, {}) }
        }
        compose.onNodeWithText("Fertilizing").performScrollTo().performClick().assertIsSelected()
    }

    @Test fun changingDedicatedFarmPagesRetainsUnsavedPlotEdits() {
        val current = mutableStateOf("Plots")
        compose.setContent {
            SmartFarmTheme {
                FarmDetailScreen(FarmUiState(selected = FarmDto("farm", "Farm", "Location", "Asia/Manila",
                    null, null, "Owner", "version"), plots = listOf(PlotDto("plot", "farm", "North plot", BigDecimal.TEN, "version"))),
                    {}, { _, _, _, _, _ -> }, {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {},
                    { _, _ -> }, { _, _ -> }, {}, section = current.value)
            }
        }
        compose.onNodeWithTag("plot-card-plot").performScrollTo().performClick()
        compose.onNodeWithText("Plot name").performScrollTo().performTextReplacement("Draft plot")
        compose.runOnIdle { current.value = "Members" }
        compose.onNodeWithText("Team & access").assertExists()
        compose.runOnIdle { current.value = "Plots" }
        compose.onNodeWithText("Draft plot").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Weather").assertDoesNotExist()
    }

    @Test fun fieldWorkersCanCompleteOnlyTheirAssignedTasks() {
        val task = TaskDto("task", "farm", null, "other", 2, "2026-10-26", 0, "Water north plot", "version")
        val state = mutableStateOf(TaskUiState(farmId = "farm", tasks = listOf(task),
            timeZoneId = "Asia/Manila"))
        var completed: String? = null
        compose.setContent { SmartFarmTheme {
            TaskScreen(state.value, false, "me", {}, {}, { completed = it.id }, {})
        } }
        compose.onNodeWithText("All farm tasks").assertDoesNotExist()
        compose.onNodeWithText("Mark completed").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(tasks = listOf(task.copy(assignedToUserId = "me"))) }
        compose.onNodeWithText("Mark completed").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(null, completed) }
        compose.onNodeWithText("Complete task").performClick()
        compose.runOnIdle { assertEquals("task", completed) }
    }
}
