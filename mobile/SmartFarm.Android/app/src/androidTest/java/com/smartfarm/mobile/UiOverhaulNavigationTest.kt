package com.smartfarm.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfarm.mobile.navigation.FarmWorkspaceBar
import com.smartfarm.mobile.ui.components.OfflineDataNotice
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class UiOverhaulNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bottomTabsReuseExistingRoutesWithoutReopeningSelectedTab() {
        var destination: String? = null
        compose.setContent {
            SmartFarmTheme {
                FarmWorkspaceBar(selectedRoute = "farm/{farmId}") {
                    destination = it
                }
            }
        }
        listOf("Farm", "Tasks", "Crops", "Stock", "Account").forEach { label ->
            compose.onNodeWithText(label).assertExists()
        }
        compose.onNodeWithText("Farm").performClick()
        compose.runOnIdle { assertNull(destination) }

        compose.onNodeWithText("Crops").performClick()
        compose.runOnIdle { assertEquals("crops/{farmId}", destination) }
    }

    @Test fun offlineNoticeShowsLocalOnlyStateAndDisappearsWhenConnected() {
        val offline = mutableStateOf(true)
        compose.setContent {
            SmartFarmTheme {
                OfflineDataNotice(offline = offline.value, savedAtUtcMillis = null)
            }
        }
        compose.onNodeWithText("Viewing saved offline data").assertExists()
        compose.onNodeWithText("Offline copy from a previous visit", substring = true)
            .assertExists()
        compose.runOnIdle { offline.value = false }
        compose.onNodeWithText("Viewing saved offline data").assertDoesNotExist()
    }
}
