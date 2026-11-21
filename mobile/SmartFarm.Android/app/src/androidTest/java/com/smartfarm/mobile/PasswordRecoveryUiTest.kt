package com.smartfarm.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.auth.ForgotPasswordScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PasswordRecoveryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resetRequestIsNeutralAndDoesNotRevealAccountExistence() {
        val state = mutableStateOf(AuthUiState())
        var requestedAddress: String? = null
        compose.setContent {
            SmartFarmTheme {
                ForgotPasswordScreen(state = state.value,
                    onRequest = { address ->
                        requestedAddress = address
                        state.value = state.value.copy(recoveryRequested = true)
                    },
                    onLogin = {})
            }
        }
        compose.onNodeWithTag("request-password-reset").assertIsNotEnabled()
        compose.onNodeWithTag("recovery-email").performTextInput("farmer@example.com")
        compose.onNodeWithTag("recovery-email").assertTextContains("farmer@example.com")
        compose.onNodeWithTag("request-password-reset").assertIsEnabled()
        compose.onNodeWithTag("recovery-email").performImeAction()
        compose.onNodeWithTag("request-password-reset").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("farmer@example.com", requestedAddress) }
        compose.onNodeWithText("Check your email").assertExists()
        compose.onNodeWithTag("request-password-reset").assertDoesNotExist()
        compose.onNodeWithText(
            "If an active account exists and email delivery is configured, reset instructions will be sent. We cannot confirm whether an account exists for that address."
        ).assertExists()
    }

    @Test fun busyStatePreventsAnotherRecoveryRequest() {
        compose.setContent {
            SmartFarmTheme {
                ForgotPasswordScreen(
                    state = AuthUiState(recoveryBusy = true),
                    onRequest = { throw AssertionError("Cannot submit while request is pending") },
                    onLogin = {})
            }
        }
        compose.onNodeWithTag("request-password-reset").assertIsNotEnabled()
    }
}
