package com.smartfarm.mobile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.auth.LoginScreen
import com.smartfarm.mobile.localization.LanguageChoices
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AccountPreferencesTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun welcomeAndLoginHaveNoLanguageSelector() {
        compose.setContent {
            SmartFarmTheme {
                WelcomeScreen()
            }
        }
        compose.onNodeWithText("Get started").assertIsDisplayed()
        compose.onNodeWithTag("account-language-settings").assertDoesNotExist()
        compose.onNodeWithText("English").assertDoesNotExist()
        compose.onNodeWithText("Filipino").assertDoesNotExist()
    }

    @Test
    fun loginScreenDoesNotOfferASeparateLanguagePreference() {
        compose.setContent {
            SmartFarmTheme {
                LoginScreen(AuthUiState(), onSubmit = { _, _ -> }, onRegister = {})
            }
        }
        compose.onNodeWithTag("account-language-settings").assertDoesNotExist()
        compose.onNodeWithText("English").assertDoesNotExist()
        compose.onNodeWithText("Filipino").assertDoesNotExist()
    }

    @Test
    fun accountGroupsDarkThemeAndBothLanguagesWithinSettings() {
        compose.setContent {
            SmartFarmTheme {
                AccountScreen(AuthUiState(), onLogout = {}, onFarms = {}, onSync = {})
            }
        }
        compose.onNodeWithTag("account-dark-theme").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("account-language-settings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("language-option-en").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("language-option-fil").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun languageSelectionHasExactlyOneSelectedOptionAndWholeRowIsClickable() {
        var changes = 0
        var selected = ""
        compose.setContent {
            var language by remember { mutableStateOf("en") }
            SmartFarmTheme {
                LanguageChoices(
                    selectedLanguage = language,
                    onSelect = {
                        language = it
                        selected = it
                        changes++
                    }
                )
            }
        }
        compose.onNodeWithTag("language-option-en").assertIsSelected()
        compose.onNodeWithTag("language-option-fil").assertIsNotSelected()
        compose.onNodeWithTag("language-option-fil").performClick()
        compose.onNodeWithTag("language-option-fil").assertIsSelected()
        compose.onNodeWithTag("language-option-en").assertIsNotSelected()
        compose.onNodeWithTag("language-option-fil").performClick()
        compose.runOnIdle {
            assertEquals("fil", selected)
            assertEquals("Already selected language should not trigger another write", 1, changes)
        }
    }

    @Test
    fun compactLargeTextStillShowsBothLanguageChoices() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme {
                        AccountScreen(AuthUiState(), onLogout = {}, onFarms = {}, onSync = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("account-dark-theme").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("language-option-en").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("language-option-fil").performScrollTo().assertIsDisplayed()
    }
}
