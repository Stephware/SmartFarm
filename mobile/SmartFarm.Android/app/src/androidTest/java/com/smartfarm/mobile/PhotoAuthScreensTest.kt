package com.smartfarm.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.auth.ForgotPasswordScreen
import com.smartfarm.mobile.features.auth.LoginScreen
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhotoAuthScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun welcomePhotoAuthKeepsBothEntryPaths() {
        var login = 0
        var registration = 0
        compose.setContent {
            SmartFarmTheme {
                WelcomeScreen(onLogin = { login++ }, onRegister = { registration++ })
            }
        }
        compose.onNodeWithText("Welcome to AniGo").assertExists()
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, login); assertEquals(1, registration) }
    }

    @Test fun loginPhotoAuthPreservesValidationAndNavigation() {
        var reset = 0
        var registration = 0
        var submission: Pair<String, String>? = null
        compose.setContent {
            SmartFarmTheme {
                LoginScreen(AuthUiState(),
                    onSubmit = { email, password -> submission = email to password },
                    onRegister = { registration++ },
                    onForgotPassword = { reset++ })
            }
        }
        compose.onNodeWithText("Good to have you back.").assertExists()
        compose.onNodeWithText("Log in").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Forgot password?").performScrollTo().performClick()
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.onNodeWithText("Email address").performScrollTo().performTextInput("farmer@example.com")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("Password2026")
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, reset)
            assertEquals(1, registration)
            assertEquals("farmer@example.com" to "Password2026", submission)
        }
    }

    @Test fun registrationKeepsExistingThreeFieldApiContract() {
        var captured: List<String>? = null
        compose.setContent {
            SmartFarmTheme {
                RegisterScreen(AuthUiState(), { name, email, password ->
                    captured = listOf(name, email, password)
                }, {})
            }
        }
        compose.onNodeWithText("Create your account").assertExists()
        compose.onNodeWithText("Confirm password").assertDoesNotExist()
        compose.onNodeWithText("Full name").performScrollTo().performTextInput("Sam Rivera")
        compose.onNodeWithText("Email address").performScrollTo().performTextInput("sam@example.com")
        compose.onNodeWithText("Password").performScrollTo().performTextInput("Password2026")
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("Sam Rivera", "sam@example.com", "Password2026"), captured)
        }
    }

    @Test fun recoveryDoesNotPretendToSendAnUnsupportedEmail() {
        var back = 0
        compose.setContent {
            SmartFarmTheme { ForgotPasswordScreen(onLogin = { back++ }) }
        }
        compose.onNodeWithText("Forgot your password?").assertExists()
        compose.onNodeWithText("Email password reset is not available in this AniGo version.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Send reset link").assertDoesNotExist()
        compose.onNodeWithText("Back to log in").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(back == 1) }
    }
}
