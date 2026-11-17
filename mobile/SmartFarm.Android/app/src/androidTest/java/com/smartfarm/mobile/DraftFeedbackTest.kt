package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import android.os.SystemClock
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
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
import org.junit.Assert.assertEquals

class DraftFeedbackTest {
    @get:Rule val compose = createComposeRule()

    /**
     * The Android IME animates independently from Compose's idling clock.
     * Wait for actual root-inset visibility and a stable inset height before
     * scrolling or tapping coordinates in a form.
     */
    private fun awaitStableIme(view: () -> android.view.View?, visible: Boolean) {
        var previousHeight = -1
        var unchangedSince = SystemClock.uptimeMillis()
        compose.waitUntil(7_000) {
            val insets = view()?.let(ViewCompat::getRootWindowInsets)
            val height = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: -1
            if (height != previousHeight) {
                previousHeight = height
                unchangedSince = SystemClock.uptimeMillis()
            }
            val matchesVisibility = insets?.isVisible(WindowInsetsCompat.Type.ime()) == visible
            matchesVisibility && height >= 0 &&
                SystemClock.uptimeMillis() - unchangedSince >= 200L
        }
    }

    @Test fun draftRemainsOnFailureAndClearsOnlyAfterLocalSave() {
        var hideKeyboard: () -> Unit = {}
        var view: android.view.View? = null
        var saved: (() -> Unit)? = null
        var calls = 0
        val state = mutableStateOf(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true))
        compose.setContent {
            val keyboard = LocalSoftwareKeyboardController.current
            val focus = LocalFocusManager.current
            val currentView = LocalView.current
            SideEffect {
                hideKeyboard = {
                    // Remove focus so the IME cannot reopen during the Save tap.
                    focus.clearFocus(force = true)
                    keyboard?.hide()
                }
                view = currentView
            }
            SmartFarmTheme {
                MonitoringScreen(state.value, onBack = {},
                    onActivity = { kind, notes ->
                        assertEquals(1, kind)
                        assertEquals("Watered north field", notes)
                        calls++
                        if (calls == 1) state.value = state.value.copy(error = "Local save failed.")
                        else saved = {
                            state.value = state.value.copy(error = null,
                                saveReceipts = mapOf("farm/crop/activity" to "saved"))
                        }
                    },
                    onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() }, onDismissPreview = {})
            }
        }
        compose.onNodeWithText("Notes (optional)").performScrollTo().performTextInput("Watered north field")
        // Dismiss the IME through Compose, without Espresso's window-focus dependency.
        compose.runOnIdle { hideKeyboard() }
        // Native keyboard animation can move the lazy form after Compose is idle.
        awaitStableIme({ view }, visible = false)
        compose.onNodeWithText("Save activity").performScrollTo()
        compose.waitUntil(5_000) { compose.onNodeWithText("Save activity").isDisplayed() }
        compose.onNodeWithText("Save activity").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
        compose.onNodeWithText("Local save failed.").assertIsDisplayed()
        compose.onNodeWithText("Notes (optional)").performScrollTo().assertTextContains("Watered north field")
        compose.onNodeWithText("Save activity").performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, calls) }
        compose.onNodeWithText("Notes (optional)").performScrollTo().assertTextContains("Watered north field")
        compose.runOnIdle {
            val acceptSave = requireNotNull(saved) {
                "Save activity must call onActivity before a local receipt can clear the draft"
            }
            acceptSave()
        }
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
                        onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() }, onDismissPreview = {})
                }
            }
        }
        compose.onNodeWithText("Inspection").performScrollTo().performClick()
        compose.onNodeWithText("Choose photo").performScrollTo().performClick()
        compose.onNodeWithText("Save inspection").performScrollTo().performClick()
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
                    onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() }, onDismissPreview = {})
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
        compose.onNodeWithText("Harvest quantity: 3").assertIsDisplayed()
        compose.onAllNodesWithText("Saved on phone").assertCountEquals(2)
        compose.onNodeWithText("Uploads automatically when a connection is available.").assertIsDisplayed()
    }

    @Test fun registrationSubmitCanBeScrolledAboveTheKeyboard() {
        var view: android.view.View? = null
        compose.setContent {
            val currentView = LocalView.current
            SideEffect { view = currentView }
            SmartFarmTheme { RegisterScreen(AuthUiState(), { _, _, _ -> }, {}) }
        }
        compose.onNodeWithText("Password").performScrollTo().performTextInput("Password2026")
        // Do not scroll while the native keyboard is still moving the viewport.
        // The button must remain reachable with the IME OPEN, not only after hiding it.
        awaitStableIme({ view }, visible = true)
        compose.onNodeWithText("Create account").performScrollTo()
        compose.waitUntil(5_000) { compose.onNodeWithText("Create account").isDisplayed() }
        compose.onNodeWithText("Create account").assertIsDisplayed()
    }
}
