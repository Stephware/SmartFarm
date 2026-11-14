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
            // Headless emulators can expose the native positive button by text before
            // its framework resource ID is discoverable by UI Automator.
            val confirm = device.wait(
                androidx.test.uiautomator.Until.findObject(ok), 15_000L)
                ?: device.wait(androidx.test.uiautomator.Until.findObject(
                    androidx.test.uiautomator.By.text("OK")), 5_000L)
            org.junit.Assert.assertNotNull("Native date picker should show its OK button", confirm)
            confirm!!.click()
            compose.waitUntil(5_000) { selected == selectedDate }
            compose.runOnIdle { assertEquals(selectedDate, selected) }
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
