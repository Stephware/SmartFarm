package com.smartfarm.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.features.crops.CropScreen
import com.smartfarm.mobile.features.farms.*
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.monitoring.MonitoringUiState
import com.smartfarm.mobile.features.tasks.TaskScreen
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.navigation.FarmMoreScreen
import com.smartfarm.mobile.navigation.FarmQuickRecordSheet
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Phase3RefinementTest {
    @get:Rule val compose = createComposeRule()

    @Composable private fun Narrow(content: @Composable () -> Unit) {
        DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))) {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                SmartFarmTheme { content() }
            }
        }
    }

    @Test fun setupActionsStayInsideSmallScreenWithLargeText() {
        compose.setContent { Narrow {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                FarmSetupGuide("farm", false, null, {}, {})
            }
        } }
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        for (label in listOf("See all setup steps", "Not now")) {
            val button = compose.onNode(hasText(label) and hasClickAction())
            button.assertIsDisplayed()
            val bounds = button.getUnclippedBoundsInRoot()
            assertTrue("$label must fit the phone", bounds.left >= root.left && bounds.right <= root.right)
            assertTrue("$label needs a 48dp touch target", bounds.bottom - bounds.top >= 48.dp)
        }
        compose.onNodeWithText("See all setup steps").performClick()
        compose.onNodeWithText("3. Record fieldwork from the Record button").assertExists()
    }

    @Test fun savedWorkHintIsReadableWithLargeTextAndReviewCount() {
        var opened = ""
        compose.setContent { Narrow {
            FarmMoreScreen("Long farm name in San Fernando", "FieldWorker", { opened = it },
                pendingSync = 120, reviewSync = 110)
        } }
        compose.onNodeWithText("Field worker").assertExists()
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Offline sync"))
        val hint = compose.onNodeWithText("Work saved on this phone", useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        hint.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("Saved-work guidance must not be clipped", layouts.single().hasVisualOverflow)
        compose.onNode(hasText("Offline sync") and hasClickAction()).performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals("sync", opened) }
    }

    @Test fun recordOptionsExposeNativeButtonSemantics() {
        compose.setContent { SmartFarmTheme {
            FarmQuickRecordSheet("farm", "Test farm", CropUiState(farmId = "farm"), {}, { _, _ -> }, {}, {})
        } }
        compose.onNode(hasText("Water") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test fun collapsedForecastKeepsSourceAndFreshnessVisible() {
        val day = WeatherDayDto(date = LocalDate.now(ZoneId.of("Asia/Manila")).toString(), minTemperatureC = null,
            maxTemperatureC = null, rainfallMm = null, retrievedAtUtc = Instant.now().toString(),
            provider = "Test provider")
        compose.setContent { SmartFarmTheme {
            androidx.compose.foundation.layout.Column {
                FarmWeatherDetails(FarmUiState(selected = FarmDto("farm", "Farm", "Location", "Asia/Manila", role = "Owner"),
                    weather = WeatherOverviewDto("farm", true, "Asia/Manila", listOf(day), emptyList())))
            }
        } }
        compose.onNodeWithText("Source: Test provider").assertExists()
        compose.onNodeWithText("Last checked:", substring = true).assertExists()
        compose.onNodeWithText("Show daily forecast").assertExists()
    }

    private fun assertSingleLine(label: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("$label must stay readable without splitting a word", layouts.single().lineCount == 1)
    }

    @Test fun cropFiltersKeepWholeWordsAtLargeText() {
        compose.setContent { Narrow {
            CropScreen(CropUiState(farmId = "farm"), true, {}, { _, _, _, _ -> }, { _, _, _, _ -> },
                { _, _, _ -> }, {}, {}, { _, _, _, _ -> })
        } }
        listOf("Growing", "Planned", "History").forEach(::assertSingleLine)
    }

    @Test fun taskFiltersKeepWholeWordsAtLargeText() {
        compose.setContent { Narrow {
            TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila"), true,
                "owner", {}, {}, {}, {})
        } }
        listOf("Today", "Upcoming", "Completed").forEach(::assertSingleLine)
    }

    @Test fun fieldworkTabsKeepWholeWordsAtLargeText() {
        compose.setContent { Narrow {
            MonitoringScreen(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                {}, { _, _ -> }, { _, _, _, _ -> }, { _, _ -> }, {}, { byteArrayOf() }, {}, quick = "watering")
        } }
        listOf("Activity", "Inspection", "History").forEach(::assertSingleLine)
    }
}
