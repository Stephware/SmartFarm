package com.smartfarm.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationInputsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun datePickerConfirmsAnIsoDateForTheExistingApi() {
        // UI Automator is required for the framework DatePickerDialog. Espresso
        // sometimes selects an unfocused window on headless Android emulators.
        val selectedDate = "2026-10-26"
        var selected = ""
        val formatted = java.time.LocalDate.parse(selectedDate).format(
            java.time.format.DateTimeFormatter.ofLocalizedDate(
                java.time.format.FormatStyle.MEDIUM))
        compose.setContent { SmartFarmTheme {
            DateInput(selectedDate, "Planting date", { selected = it })
        } }
        val device = androidx.test.uiautomator.UiDevice.getInstance(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        val ok = androidx.test.uiautomator.By.res("android:id/button1")
        try {
            compose.onNodeWithContentDescription(
                "Planting date: $formatted. Choose date").performClick()
            // Commit the framework window before querying its accessibility tree.
            compose.waitForIdle()
            device.waitForIdle()
            // Headless emulators can expose the native positive button by text before
            // its framework resource ID is discoverable by UI Automator.
            // Android 35 emulators may not expose framework dialog buttons to
            // UI Automator immediately. Look up the native positive button by
            // resource and accessible text, then fall back to Espresso's dialog root.
            fun nativeConfirm() = device.wait(
                androidx.test.uiautomator.Until.findObject(ok), 5_000L)
                ?: device.wait(androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.res("android", "button1")), 2_000L)
                ?: device.wait(androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.text("OK")), 2_000L)
                ?: device.wait(androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.text("Set")), 2_000L)
            var confirm = nativeConfirm()
            if (confirm == null) {
                // A framework dialog can be created but fail to acquire window
                // focus on a headless emulator. Espresso's isDialog fallback
                // cannot click that unfocused root. Dismiss and reopen it once,
                // then require a real accessible native confirmation control.
                device.pressBack()
                compose.waitForIdle()
                compose.onNodeWithContentDescription(
                    "Planting date: $formatted. Choose date").performClick()
                compose.waitForIdle()
                device.waitForIdle()
                confirm = nativeConfirm()
            }
            org.junit.Assert.assertNotNull(
                "Native DatePickerDialog must expose a focused confirmation button", confirm)
            confirm!!.click()
            compose.waitUntil(5_000) { selected == selectedDate }
            compose.runOnIdle { assertEquals(selectedDate, selected) }
        } catch (failure: Throwable) {
            // Preserve the failed window, rather than guessing why its controls vanished.
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val directory = java.io.File(instrumentation.targetContext.getExternalFilesDir(null),
                "date-picker-failure").apply { mkdirs() }
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
            throw failure
        } finally {
            // Never leave a native dialog open for the next Compose test.
            if (device.hasObject(ok)) device.pressBack()
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
