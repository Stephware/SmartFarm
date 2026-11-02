package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.runtime.mutableStateOf
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.StateRestorationTester
import android.net.Uri
import androidx.compose.ui.test.isDisplayed
import androidx.compose.foundation.layout.Column
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.monitoring.MonitoringUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Rule
import org.junit.Test

class DraftFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draftRemainsOnFailureAndClearsOnlyAfterLocalSave() {
        var saved: (() -> Unit)? = null
        val state = mutableStateOf(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true))
        compose.setContent {
            SmartFarmTheme {
                MonitoringScreen(state.value, onBack = {},
                    onActivity = { _, _ -> saved = {
                        state.value = state.value.copy(saveReceipts = mapOf("farm/crop/activity" to "saved"))
                    } },
                    onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {})
            }
        }
        compose.onNodeWithText("Notes (optional)").performScrollTo().performTextInput("Watered north field")
        compose.onNodeWithText("Save activity").performScrollTo().performClick()
        compose.onNodeWithText("Notes (optional)").assertTextContains("Watered north field")
        compose.runOnIdle { saved!!() }
        compose.onNodeWithText("Watered north field").assertDoesNotExist()
    }

    @Test fun selectedPhotoSurvivesRecreationAndFailedSave() {
        var saved: (() -> Unit)? = null
        val state = mutableStateOf(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true))
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>,
                    input: I, options: ActivityOptionsCompat?) {
                    dispatchResult(requestCode, Uri.parse("content://test/selected-photo"))
                }
            }
        }
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                SmartFarmTheme {
                    MonitoringScreen(state.value, onBack = {},
                        onActivity = { _, _ -> },
                        onObservation = { _, _, _, _ -> saved = {
                            state.value = state.value.copy(saveReceipts = mapOf("farm/crop/observation" to "saved"))
                        } },
                        onUpload = { _, _ -> }, onPreview = {})
                }
            }
        }
        compose.onNodeWithText("Select photo").performScrollTo().performClick()
        compose.onNodeWithText("Save observation").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Photo selected for offline upload").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { saved!!() }
        compose.onNodeWithText("Photo selected for offline upload").assertDoesNotExist()
    }

    @Test fun savingDisablesDraftEditing() {
        val state = mutableStateOf(MonitoringUiState(canRecord = true))
        compose.setContent {
            SmartFarmTheme {
                MonitoringScreen(state.value, onBack = {},
                    onActivity = { _, _ -> state.value = state.value.copy(busy = true) },
                    onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {})
            }
        }
        compose.onNodeWithText("Save activity").performScrollTo().performClick()
        compose.onNodeWithText("Notes (optional)").assertIsNotEnabled()
    }

    @Test fun pendingHarvestsShowOnlyForTheCurrentFarmAndCrop() {
        val operation = PendingOperation("op", "user", "farm", "harvest",
            """{"targetId":"crop","harvest":{"quantity":3}}""", 1)
        compose.setContent {
            SmartFarmTheme {
                Column { PendingRecords(listOf(operation, operation.copy(operationId = "other", farmId = "other")),
                    "farm", "crop", setOf("harvest")) {} }
            }
        }
        compose.onNodeWithText("Harvest quantity: 3 · Waiting to sync").assertIsDisplayed()
        compose.onNodeWithText("Review sync status").assertIsDisplayed()
    }

    @Test fun registrationSubmitCanBeScrolledAboveTheKeyboard() {
        compose.setContent { SmartFarmTheme { RegisterScreen(AuthUiState(), { _, _, _ -> }, {}) } }
        compose.onNodeWithText("Password").performScrollTo().performTextInput("Password2026")
        compose.onNodeWithText("Register").performScrollTo()
        // The system keyboard animates outside Compose's idling clock.
        compose.waitUntil(5000) { compose.onNodeWithText("Register").isDisplayed() }
        compose.onNodeWithText("Register").assertIsDisplayed()
    }
}
