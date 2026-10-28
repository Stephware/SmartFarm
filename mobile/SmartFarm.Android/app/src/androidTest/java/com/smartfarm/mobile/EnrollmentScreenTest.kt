package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import androidx.compose.runtime.mutableStateOf
import com.smartfarm.mobile.features.farms.*
import com.smartfarm.mobile.data.remote.dto.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EnrollmentScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun registrationOnlyAsksForNameEmailAndPassword() {
        compose.setContent { SmartFarmTheme { RegisterScreen(AuthUiState(), { _, _, _ -> }, {}) } }
        compose.onNodeWithText("Full name").assertExists()
        compose.onNodeWithText("Email").assertExists()
        compose.onNodeWithText("Password").assertExists()
        compose.onNodeWithText("Confirm password").assertDoesNotExist()
        compose.onNodeWithText("Farm code").assertDoesNotExist()
        compose.onNodeWithText("Join an existing farm").assertDoesNotExist()
        compose.onNodeWithText("Create/manage my own farm").assertDoesNotExist()
    }

    @Test fun registrationSubmitsWithoutAFarmCodeOrConfirmation() {
        var submitted: List<String>? = null
        compose.setContent { SmartFarmTheme { RegisterScreen(AuthUiState(), { name, email, password ->
            submitted = listOf(name, email, password)
        }, {}) } }
        compose.onNodeWithText("Register").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Full name").performScrollTo().performTextInput("Sam")
        compose.onNodeWithText("Email").performScrollTo().performTextInput("sam@example.com")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("Password2026")
        compose.onNodeWithText("Register").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("Sam", "sam@example.com", "Password2026"), submitted) }
    }

    @Test fun requestStatusDoesNotGrantFarmAccessAndFailedFormRetainsCode() {
        val state = mutableStateOf(FarmUiState(myRequests = listOf(request())))
        var sent = ""
        compose.setContent { SmartFarmTheme { FarmListScreen(state.value, {}, { _, _, _, _, _ -> },
            { sent = it; state.value = state.value.copy(error = "Code not recognized") }, {}) } }
        compose.onNodeWithText("North farm: Pending · Awaiting owner approval").assertExists()
        compose.onNodeWithText("Join a farm").performClick()
        compose.onNodeWithText("Farm code").performTextInput("ABCD12345678")
        compose.onNodeWithText("Request access").performClick()
        compose.runOnIdle { assertEquals("ABCD12345678", sent) }
        compose.onNodeWithText("Farm code").assertTextContains("ABCD12345678")
        compose.onNodeWithText("Code not recognized").assertExists()
    }

    @Test fun ownerCanApproveOrRejectAndOtherRolesCannotSeeCodesOrApplicants() {
        val state = mutableStateOf(FarmUiState(selected = farm("Owner"), enrollment =
            FarmEnrollmentOverviewDto("ABCD12345678", listOf(request()))))
        var decision: Pair<String, Boolean>? = null
        compose.setContent { SmartFarmTheme { detail(state.value) { id, approve -> decision = id to approve } } }
        compose.onNodeWithText("ABCD12345678").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("sam@example.com").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Approve Sam").performScrollTo().performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals("req" to true, decision) }
        compose.onNodeWithText("Reject Sam").performScrollTo().performClick()
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals("req" to false, decision); state.value = state.value.copy(selected = farm("Manager")) }
        compose.onNodeWithText("ABCD12345678").assertDoesNotExist()
        compose.onNodeWithText("sam@example.com").assertDoesNotExist()
        compose.onNodeWithText("Approve Sam").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(selected = farm("FieldWorker")) }
        compose.onNodeWithText("ABCD12345678").assertDoesNotExist()
        compose.onNodeWithText("sam@example.com").assertDoesNotExist()
    }

    @Test fun unavailableEnrollmentCanBeRetriedWithoutLosingFarmAccess() {
        var retries = 0
        compose.setContent { SmartFarmTheme {
            FarmListScreen(FarmUiState(enrollmentMessage = "Join request status unavailable."), {},
                { _, _, _, _, _ -> }, {}, {}, onRetry = { retries++ })
        } }
        compose.onNodeWithText("Refresh join requests").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun offlineOwnerCannotUseStaleApprovalData() {
        compose.setContent { SmartFarmTheme { detail(FarmUiState(selected = farm("Owner"), offline = true,
            enrollment = FarmEnrollmentOverviewDto("ABCD12345678", listOf(request())))) { _, _ -> } } }
        compose.onNodeWithText("Join requests and farm code are unavailable offline.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ABCD12345678").assertDoesNotExist()
        compose.onNodeWithText("Approve Sam").assertDoesNotExist()
    }

    @Test fun onlyPendingRequestsCanBeCanceledAndHistoryExplainsReapplying() {
        var canceled: FarmJoinRequestDto? = null
        compose.setContent { SmartFarmTheme {
            FarmListScreen(FarmUiState(myRequests = listOf(request(), request().copy(id = "old", status = "Canceled"))),
                {}, { _, _, _, _, _ -> }, {}, {}, onCancelRequest = { canceled = it })
        } }
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Cancel request for North farm"))
        compose.onNodeWithText("Cancel request for North farm").performScrollTo().performClick()
        compose.runOnIdle { assertNull(canceled) }
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals("req", canceled?.id) }
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("North farm: Canceled"))
        compose.onNodeWithText("North farm: Canceled").assertExists()
        compose.onNodeWithText("Request history").assertExists()
        compose.onNodeWithText("Enter the farm's current code to apply again after rejection or cancellation.").assertExists()
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Cancel request for North farm"))
        compose.onAllNodesWithText("Cancel request for North farm").assertCountEquals(1)
    }

    @Test fun ownerCanCopyAndRegenerateTheDisplayedCodeAndSeeTerminalHistory() {
        var regenerated: String? = null
        val state = FarmUiState(selected = farm("Owner"), enrollment = FarmEnrollmentOverviewDto(
            "ABCD12345678", emptyList(), listOf(request().copy(status = "Rejected"))))
        compose.setContent { SmartFarmTheme {
            FarmDetailScreen(state, {}, { _, _, _, _, _ -> }, {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {},
                { _, _ -> }, { _, _ -> }, {}, onRegenerateCode = { regenerated = it })
        } }
        compose.onNodeWithText("Copy farm code").performScrollTo().performClick()
        compose.onNodeWithText("Farm code copied.").assertExists()
        compose.runOnIdle {
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("ABCD12345678", context.getSystemService(android.content.ClipboardManager::class.java)
                .primaryClip?.getItemAt(0)?.text?.toString())
        }
        compose.onNodeWithText("Regenerate farm code").performScrollTo().performClick()
        compose.runOnIdle { assertNull(regenerated) }
        compose.onNodeWithText("Confirm").performClick()
        compose.runOnIdle { assertEquals("ABCD12345678", regenerated) }
        compose.onNodeWithText("Request history").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sam: Rejected").assertExists()
        compose.onNodeWithText("Approve Sam").assertDoesNotExist()
    }

    @Test fun enrollmentNotificationsUseTheExistingNotificationList() {
        val notification = com.google.gson.Gson().fromJson(
            """{"id":"notice","type":4,"title":"Farm request approved","message":"You now have Field Worker access.","createdAtUtc":"2026-10-28T01:00:00Z"}""",
            com.smartfarm.mobile.data.remote.api.NotificationDto::class.java)
        var read: String? = null
        compose.setContent { SmartFarmTheme {
            com.smartfarm.mobile.features.tasks.TaskScreen(
                com.smartfarm.mobile.features.tasks.TaskUiState(notifications = listOf(notification)),
                false, "farmer", {}, {}, {}, { read = it })
        } }
        compose.onNodeWithText("Farm request approved").assertIsDisplayed()
        compose.onNodeWithText("You now have Field Worker access.").assertIsDisplayed()
        compose.onNodeWithText("Mark as read").performClick()
        compose.runOnIdle { assertEquals("notice", read) }
    }

    @androidx.compose.runtime.Composable
    private fun detail(state: FarmUiState, decision: (String, Boolean) -> Unit) {
        FarmDetailScreen(state, {}, { _, _, _, _, _ -> }, {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {},
            decision, { _, _ -> }, {})
    }

    private fun farm(role: String) = FarmDto("farm", "North farm", "Location", "Asia/Manila", role = role)
    private fun request() = FarmJoinRequestDto("req", "farm", "North farm", "farmer", "Sam", "sam@example.com",
        "Pending", "2026-10-27T01:00:00Z")
}
