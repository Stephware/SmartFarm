package com.smartfarm.mobile

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.smartfarm.mobile.data.remote.api.TaskCreateInput
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.tasks.TaskScreen
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DailyWorkScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun todayTask() = TaskDto("today-1", "farm", null, "worker", 4,
        TaskWorkCenter.today("Asia/Manila").toString(), 0, "Inspect the rows", "version")

    @Test fun fieldWorkerSeesAssignedWorkButCannotCreateManagerTasks() {
        compose.setContent {
            SmartFarmTheme {
                TaskScreen(
                    TaskUiState(farmId = "farm", tasks = listOf(todayTask()),
                        timeZoneId = "Asia/Manila"),
                    canManage = false, actorId = "worker",
                    onBack = {}, onRetry = {}, onComplete = {}, onRead = {}
                )
            }
        }
        compose.onNodeWithText("Today's farm work").assertExists()
        compose.onNodeWithText("1 due today").assertExists()
        compose.onNodeWithText("Create and assign task").assertDoesNotExist()
    }

    @Test fun managerCanCreateTaskUsingExistingCallback() {
        var created: TaskCreateInput? = null
        compose.setContent {
            SmartFarmTheme {
                TaskScreen(
                    TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
                        choicesReady = true),
                    canManage = true, actorId = "manager",
                    onBack = {}, onRetry = {}, onComplete = {}, onRead = {},
                    onCreate = { created = it }
                )
            }
        }
        compose.onNodeWithText("Create and assign task").performScrollTo().performClick()
        compose.onNodeWithText("Create task").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(4, created?.taskType)
            assertEquals(TaskWorkCenter.today("Asia/Manila").toString(), created?.dueDate)
            assertEquals(null, created?.assignedToUserId)
        }
    }

    @Test fun staleChoicesBlockTaskCreation() {
        compose.setContent {
            SmartFarmTheme {
                TaskScreen(
                    TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
                        choicesReady = false, choicesError = "Reconnect and refresh."),
                    canManage = true, actorId = "manager",
                    onBack = {}, onRetry = {}, onComplete = {}, onRead = {}
                )
            }
        }
        compose.onNodeWithText("Reconnect and refresh.").assertExists()
        compose.onNodeWithText("Create task").assertDoesNotExist()
    }
}
