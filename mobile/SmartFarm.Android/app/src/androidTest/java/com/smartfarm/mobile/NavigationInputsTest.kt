package com.smartfarm.mobile

import android.widget.DatePicker
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
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

    @Test fun datePickerReturnsAnIsoDateForTheExistingApi() {
        var selected = ""
        compose.setContent { SmartFarmTheme { DateInput("", "Planting date", { selected = it }) } }
        compose.onNodeWithText("Choose date").performClick()
        onView(isAssignableFrom(DatePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints() = isAssignableFrom(DatePicker::class.java)
            override fun getDescription() = "Choose October 26, 2026"
            override fun perform(controller: UiController, view: View) { (view as DatePicker).updateDate(2026, 9, 26) }
        })
        onView(withId(android.R.id.button1)).perform(click())
        compose.runOnIdle { assertEquals("2026-10-26", selected) }
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
                { _, _, _, _ -> }, { _, _ -> }, {}) }
        }
        compose.onNodeWithText("Fertilizing").performScrollTo().performClick().assertIsSelected()
    }

    @Test fun switchingFarmSectionsRetainsUnsavedPlotEdits() {
        compose.setContent {
            SmartFarmTheme {
                FarmDetailScreen(FarmUiState(selected = FarmDto("farm", "Farm", "Location", "Asia/Manila",
                    null, null, "Owner", "version"), plots = listOf(PlotDto("plot", "farm", "North plot", BigDecimal.TEN, "version"))),
                    {}, { _, _, _, _, _ -> }, {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {},
                    { _, _ -> }, { _, _ -> }, {})
            }
        }
        compose.onNodeWithText("Plots").performScrollTo().performClick()
        compose.onAllNodesWithText("Plot name")[0].performScrollTo().performTextReplacement("Draft plot")
        compose.onNodeWithText("Members").performScrollTo().performClick()
        compose.onNodeWithText("Plots").performScrollTo().performClick()
        compose.onNodeWithText("Draft plot").performScrollTo().assertIsDisplayed()
    }

    @Test fun fieldWorkersCanCompleteOnlyTheirAssignedTasks() {
        val task = TaskDto("task", "farm", null, "other", 2, "2026-10-26", 0, "Water north plot", "version")
        val state = mutableStateOf(TaskUiState(farmId = "farm", tasks = listOf(task)))
        var completed: String? = null
        compose.setContent { SmartFarmTheme {
            TaskScreen(state.value, false, "me", {}, {}, { completed = it.id }, {})
        } }
        compose.onNodeWithText("Mark completed").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(tasks = listOf(task.copy(assignedToUserId = "me"))) }
        compose.onNodeWithText("Mark completed").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("task", completed) }
    }
}
