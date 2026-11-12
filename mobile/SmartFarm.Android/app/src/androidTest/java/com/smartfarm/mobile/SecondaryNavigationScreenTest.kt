package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.insights.FarmInsightsScreen
import com.smartfarm.mobile.features.insights.FarmInsightsUiState
import com.smartfarm.mobile.navigation.FarmMoreScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SecondaryNavigationScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fieldWorkerMoreHasShortFarmActionsWithoutManagerSettings() {
        var opened: String? = null
        compose.setContent { SmartFarmTheme {
            FarmMoreScreen("North farm", "FieldWorker", onOpen = { opened = it },
                pendingSync = 3, reviewSync = 1)
        } }
        compose.onNodeWithText("Farm settings").assertDoesNotExist()
        compose.onNodeWithText("Harvest & cash reports").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("insights", opened) }
        compose.onNodeWithText("1 need review").performScrollTo().assertExists()
        compose.onNodeWithText("Offline sync").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("sync", opened) }
    }

    @Test fun accountShowsSavedWorkAndConfirmsBeforeSignOut() {
        var loggedOut = false
        compose.setContent { SmartFarmTheme {
            AccountScreen(
                AuthUiState(account = AccountResponse("user", "Farmer User", "farmer@example.com")),
                onLogout = { loggedOut = true }, onFarms = {}, onSync = {},
                pendingSyncCount = 3)
        } }
        compose.onNodeWithText("Work saved on this phone").performScrollTo().assertExists()
        compose.onNodeWithTag("request-sign-out").performScrollTo().performClick()
        compose.onNodeWithText("Sign out of AniGo?").assertExists()
        compose.onNodeWithText("3 unsynced records", substring = true).assertExists()
        compose.runOnIdle { assertEquals(false, loggedOut) }
        compose.onNodeWithText("Keep working").performClick()
        compose.runOnIdle { assertEquals(false, loggedOut) }
        compose.onNodeWithTag("request-sign-out").performScrollTo().performClick()
        compose.onNodeWithTag("confirm-sign-out").performClick()
        compose.runOnIdle { assertEquals(true, loggedOut) }
    }

    @Test fun reportsSeparateHarvestFromCashWithoutChangingPeriodData() {
        compose.setContent { SmartFarmTheme {
            FarmInsightsScreen(
                FarmInsightsUiState(farmId = "farm"), "Asia/Manila",
                onBack = {}, onReload = { _, _ -> })
        } }
        compose.onNodeWithText("Harvest summary").assertExists()
        compose.onNodeWithText("Manual cash records (PHP)").assertDoesNotExist()
        compose.onNodeWithText("Cash").performClick()
        compose.onNodeWithText("Manual cash records (PHP)").assertExists()
        compose.onNodeWithText("Harvest summary").assertDoesNotExist()
        compose.onNodeWithText("Harvest").performClick()
        compose.onNodeWithText("Harvest summary").assertExists()
    }
}
