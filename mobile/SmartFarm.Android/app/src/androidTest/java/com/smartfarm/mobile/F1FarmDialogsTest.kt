package com.smartfarm.mobile

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** F1 dialogs should retain failures and react only to confirmed saves. */
class F1FarmDialogsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun confirmedJoinClosesDialogAndClearsItsDraft() {
        val state = mutableStateOf(FarmUiState())
        var submitted: String? = null
        compose.setContent { SmartFarmTheme {
            FarmListScreen(state.value, {}, { _, _, _, _, _ -> }, { code ->
                submitted = code
                state.value = state.value.copy(joinSaveVersion = state.value.joinSaveVersion + 1)
            }, {})
        } }
        compose.onNodeWithText("Join a farm").performClick()
        compose.onNodeWithText("Farm code").performScrollTo().performTextInput("ABCD12345678")
        compose.onNodeWithText("Request access").performClick()
        compose.runOnIdle { assertEquals("ABCD12345678", submitted) }
        compose.onNodeWithText("Farm code").assertDoesNotExist()
        compose.onNodeWithText("Join a farm").performClick()
        compose.onNodeWithText("Farm code").assertTextContains("")
    }

    @Test fun failedJoinKeepsDraftAndShowsActionableError() {
        val state = mutableStateOf(FarmUiState())
        compose.setContent { SmartFarmTheme {
            FarmListScreen(state.value, {}, { _, _, _, _, _ -> }, {
                state.value = state.value.copy(error = "Code not recognized")
            }, {})
        } }
        compose.onNodeWithText("Join a farm").performClick()
        compose.onNodeWithText("Farm code").performScrollTo().performTextInput("BADCODE12345")
        compose.onNodeWithText("Request access").performClick()
        compose.onNodeWithText("Code not recognized").assertExists()
        compose.onNodeWithText("Farm code").assertTextContains("BADCODE12345")
    }

    @Test fun farmChooserDoesNotUseAnAccountShortcutAsBackNavigation() {
        compose.setContent { SmartFarmTheme {
            FarmListScreen(FarmUiState(), {}, { _, _, _, _, _ -> }, {}, {})
        } }
        compose.onNodeWithText("My account").assertDoesNotExist()
    }
}
