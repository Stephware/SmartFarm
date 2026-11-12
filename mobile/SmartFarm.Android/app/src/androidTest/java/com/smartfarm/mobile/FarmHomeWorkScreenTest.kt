package com.smartfarm.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.features.crops.CropUiState
import com.smartfarm.mobile.features.farms.FarmHomeOverview
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.features.farms.HomeSyncSummary
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FarmHomeWorkScreenTest {
    @get:Rule val compose = createComposeRule()
    private val farm = FarmDto("farm", "Green Valley", "Angeles", "Asia/Manila",
        role = "FieldWorker")

    @Test fun fieldWorkerCanOpenQuickWateringWithoutScrollingThroughStatistics() {
        var selected: String? = null
        compose.setContent { SmartFarmTheme {
            Home(farm = FarmUiState(selected = farm),
                onRecordAction = { selected = it })
        } }
        compose.onNodeWithText("Your work today").assertExists()
        compose.onNodeWithText("Water").performClick()
        compose.runOnIdle { assertEquals("watering", selected) }
        compose.onNodeWithText("Active plots").assertDoesNotExist()
    }

    @Test fun overdueWorkIsVisibleAndOpensRealTaskList() {
        var taskOpens = 0
        val date = TaskWorkCenter.today(farm.timeZoneId).minusDays(1).toString()
        val task = TaskDto("task", farm.id, null, "worker", 2, date, 0,
            "Water tomatoes", "version")
        compose.setContent { SmartFarmTheme {
            Home(farm = FarmUiState(selected = farm),
                tasks = TaskUiState(farmId = farm.id, tasks = listOf(task)),
                onTasks = { taskOpens++ })
        } }
        compose.onNodeWithText("Needs attention").assertExists()
        compose.onNodeWithText("Review overdue tasks").performClick()
        compose.runOnIdle { assertEquals(1, taskOpens) }
    }

    @Test fun unsyncedRecordsAreNotShownAsCompletedFarmWork() {
        var syncOpens = 0
        compose.setContent { SmartFarmTheme {
            Home(farm = FarmUiState(selected = farm),
                sync = HomeSyncSummary(pending = 3, needsReview = 1),
                onSync = { syncOpens++ })
        } }
        compose.onNodeWithText("Saved on this phone").performScrollTo().assertExists()
        compose.onNodeWithText("View sync status").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, syncOpens) }
    }

    @Composable
    private fun Home(
        farm: FarmUiState,
        tasks: TaskUiState = TaskUiState(farmId = "farm"),
        sync: HomeSyncSummary = HomeSyncSummary(0, 0),
        onTasks: () -> Unit = {},
        onRecordAction: (String) -> Unit = {},
        onSync: () -> Unit = {}
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            FarmHomeOverview(farm, tasks, CropUiState(farmId = "farm"),
                actorId = "worker", onTasks = onTasks, onCrops = {},
                onInventory = {}, onWeather = {}, onInsights = {},
                onRecordAction = onRecordAction, onSync = onSync, syncSummary = sync)
        }
    }
}
