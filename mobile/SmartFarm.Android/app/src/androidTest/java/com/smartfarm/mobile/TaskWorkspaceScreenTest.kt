package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.tasks.TaskScreen
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TaskWorkspaceScreenTest {
    @get:Rule val compose = createComposeRule()
    private val today = TaskWorkCenter.today("Asia/Manila").toString()

    private fun task(id: String, assignee: String = "worker", status: Int = 0) =
        TaskDto(id, "farm", null, assignee, 2, today, status, "Water $id", "version")

    @Test fun workersSeeOnlyTheirAssignedWorkAndHistory() {
        val mine = task("mine")
        val other = task("other", "other")
        compose.setContent { SmartFarmTheme {
            TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
                tasks = listOf(mine, other, task("closed-mine", status = 1),
                    task("closed-other", "other", 1))),
                canManage = false, actorId = "worker",
                onBack = {}, onRetry = {}, onComplete = {}, onRead = {})
        } }
        compose.onNodeWithText("My tasks").assertExists()
        compose.onNodeWithText("Water mine").performScrollTo().assertExists()
        compose.onNodeWithText("Water other").assertDoesNotExist()
        compose.onNodeWithText("All farm tasks").assertDoesNotExist()
        compose.onNodeWithText("Completed").performClick()
        compose.onNodeWithText("Water closed-mine").performScrollTo().assertExists()
        compose.onNodeWithText("Water closed-other").assertDoesNotExist()
    }

    @Test fun completionRequiresConfirmationAndDoesNotCreateFieldworkLog() {
        var completed: String? = null
        compose.setContent { SmartFarmTheme {
            TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
                tasks = listOf(task("mine"))),
                false, "worker", {}, {}, { completed = it.id }, {})
        } }
        compose.onNodeWithText("Mark completed").performScrollTo().performClick()
        compose.onNodeWithText("Complete this task?").assertExists()
        compose.runOnIdle { assertEquals(null, completed) }
        compose.onNodeWithText("Keep open").performClick()
        compose.runOnIdle { assertEquals(null, completed) }
        compose.onNodeWithText("Mark completed").performScrollTo().performClick()
        compose.onNodeWithText("Complete task").performClick()
        compose.runOnIdle { assertEquals("mine", completed) }
    }

    @Test fun managersCanFindFarmWideEditingWithoutClutteringToday() {
        compose.setContent { SmartFarmTheme {
            TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
                tasks = listOf(task("member-task", "other")), choicesReady = true),
                canManage = true, actorId = "manager",
                onBack = {}, onRetry = {}, onComplete = {}, onRead = {})
        } }
        compose.onNodeWithText("Water member-task").assertDoesNotExist()
        compose.onNodeWithText("All farm tasks").performClick()
        compose.onNodeWithText("Water member-task").performScrollTo().assertExists()
        compose.onNodeWithText("Edit assignment").performScrollTo().assertExists()
    }
    @Test fun taskHelpIsOptionalAndNeverHidesAssignments() {
        compose.setContent { SmartFarmTheme {
            TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
                tasks = listOf(task("mine"))),
                canManage = false, actorId = "worker",
                onBack = {}, onRetry = {}, onComplete = {}, onRead = {})
        } }
        compose.onNodeWithText("About task updates").performClick()
        compose.onNodeWithText("Task updates need internet. Offline field logs are available from an active crop.")
            .assertExists()
        compose.onNodeWithText("Hide task help").performClick()
        compose.onNodeWithText("Water mine").performScrollTo().assertExists()
    }

    @Test fun refreshAfterASuccessfulSaveKeepsTheNextUnfinishedTask() {
        val receipt = com.smartfarm.mobile.features.tasks.TaskFormSave("new-task", "confirmed-first-save")
        val state = androidx.compose.runtime.mutableStateOf(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila",
            choicesReady = true, savedCount = 1, lastFormSave = receipt))
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { SmartFarmTheme {
            TaskScreen(state.value, canManage = true, actorId = "manager", onBack = {}, onRetry = {}, onComplete = {}, onRead = {})
        } }
        compose.onNodeWithText("Create and assign task").performScrollTo().performClick()
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Instructions (optional)"))
        compose.onNodeWithText("Instructions (optional)").performTextInput("Check the east field")
        // Refresh and notification reads are not form save receipts.
        compose.runOnIdle { state.value = state.value.copy(savedCount = 0) }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Instructions (optional)"))
        compose.onNodeWithText("Instructions (optional)").assertTextContains("Check the east field")
        compose.runOnIdle { state.value = state.value.copy(savedCount = 1) }
        compose.onNodeWithText("Instructions (optional)").assertTextContains("Check the east field")
    }

}
