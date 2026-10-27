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

    @Test fun registrationOffersJoiningBeforeOwnerOnboarding() {
        compose.setContent { SmartFarmTheme { RegisterScreen(AuthUiState(), { _, _, _, _, _, _ -> }, {}) } }
        compose.onNodeWithText("Join an existing farm").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Farm code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Create/manage my own farm").performScrollTo().performClick()
        compose.onNodeWithText("Farm code").assertDoesNotExist()
    }

    @Test fun joiningNeedsCodeAndSubmitsTheChosenOnboarding() {
        var submitted: Pair<Boolean, String?>? = null
        compose.setContent { SmartFarmTheme { RegisterScreen(AuthUiState(), { _, _, _, _, join, code ->
            submitted = join to code
        }, {}) } }
        compose.onNodeWithText("Full name").performScrollTo().performTextInput("Sam")
        compose.onNodeWithText("Email").performScrollTo().performTextInput("sam@example.com")
        compose.onNodeWithText("Password", substring = false).performScrollTo().performTextInput("Password2026")
        compose.onNodeWithText("Confirm password").performScrollTo().performTextInput("Password2026")
        compose.onNodeWithText("Register").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Farm code").performScrollTo().performTextInput("ABCD12345678")
        compose.onNodeWithText("Register").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true to "ABCD12345678", submitted) }
        compose.onNodeWithText("Create/manage my own farm").performScrollTo().performClick()
        compose.onNodeWithText("Register").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(false to null, submitted) }
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

    @androidx.compose.runtime.Composable
    private fun detail(state: FarmUiState, decision: (String, Boolean) -> Unit) {
        FarmDetailScreen(state, {}, { _, _, _, _, _ -> }, {}, { _, _ -> }, { _, _, _ -> }, {}, {}, {},
            decision, { _, _ -> }, {})
    }

    private fun farm(role: String) = FarmDto("farm", "North farm", "Location", "Asia/Manila", role = role)
    private fun request() = FarmJoinRequestDto("req", "farm", "North farm", "farmer", "Sam", "sam@example.com",
        "Pending", "2026-10-27T01:00:00Z")
}
