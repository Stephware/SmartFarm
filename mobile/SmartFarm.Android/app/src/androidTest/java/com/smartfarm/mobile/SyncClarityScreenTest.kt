package com.smartfarm.mobile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.smartfarm.mobile.data.sync.SyncProgress
import com.smartfarm.mobile.ui.components.LocalSyncProgress
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import com.smartfarm.mobile.ui.components.FarmAppBar
import com.smartfarm.mobile.ui.components.FarmHeaderNavigation
import com.smartfarm.mobile.ui.components.LocalFarmHeaderNavigation
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SyncClarityScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun operation(id: String, farm: String, status: String = "pending") =
        PendingOperation(id, "person", farm, "harvest",
            """{"targetId":"cycle","harvest":{"quantity":"2"}}""",
            1000L, status, if (status == "review") "Check the rejected record" else null)

    @Test fun sendingAndSentRequireActualScopedTransferEvidence() {
        val progress = mutableStateOf(SyncProgress("person", "queued", "farm-a"))
        val rows = mutableStateOf(listOf(operation("queued", "farm-a")))
        compose.setContent { SmartFarmTheme {
            CompositionLocalProvider(LocalSyncProgress provides progress.value) {
                SyncStatusScreen(rows.value, emptyList(), null, null,
                    onBack = {}, onRetry = {}, onDiscard = {}, onRetryPhoto = {}, onDiscardPhoto = {},
                    onReviewStock = {}, onApproveStock = {}, onCancelStock = {}, onSyncNow = {},
                    currentFarmId = "farm-a")
            }
        } }
        compose.onNodeWithText("Sending").performScrollTo().assertExists()
        compose.onNodeWithText("Sent").assertDoesNotExist()
        compose.runOnIdle { progress.value = SyncProgress("person", confirmedFarms = mapOf("farm-b" to 1L)) }
        compose.onNodeWithText("Sent").assertDoesNotExist()
        compose.runOnIdle {
            rows.value = emptyList()
            progress.value = SyncProgress("person", confirmedFarms = mapOf("farm-a" to 1L))
        }
        compose.onNodeWithText("Sent").performScrollTo().assertExists()
        compose.onNodeWithText("Server-confirmed work is available in farm history.").assertExists()
        compose.runOnIdle { progress.value = SyncProgress("person", signInRequired = true) }
        compose.onNodeWithText("Sign in again to send your saved work.").performScrollTo().assertExists()
        compose.onNodeWithText("Sent").assertDoesNotExist()
    }

    @Test fun globalHeaderBadgeOpensSyncAndFlagsReviewWithoutHidingNotifications() {
        var opened = 0
        var notifications = 0
        compose.setContent { SmartFarmTheme {
            CompositionLocalProvider(LocalFarmHeaderNavigation provides
                FarmHeaderNavigation("My Farm", onFarms = {}, onNotifications = { notifications++ },
                    queuedCount = 3, reviewCount = 1, onSync = { opened++ })) {
                FarmAppBar()
            }
        } }
        compose.onNodeWithContentDescription(
            "1 offline records need review; 3 waiting in total").performClick()
        compose.onNodeWithContentDescription("Notifications").performClick()
        compose.runOnIdle {
            assertEquals(1, opened)
            assertEquals(1, notifications)
        }
    }

    @Test fun syncFiltersCurrentFarmAndShowsReviewBeforeRetry() {
        var requestedRetry: String? = null
        compose.setContent { SmartFarmTheme {
            SyncStatusScreen(
                operations = listOf(
                    operation("need", "farm-a", "review"),
                    operation("queued", "farm-a"),
                    operation("other", "farm-b")),
                photos = emptyList(), stockPreview = null, error = null,
                onBack = {}, onRetry = { requestedRetry = it }, onDiscard = {},
                onRetryPhoto = {}, onDiscardPhoto = {},
                onReviewStock = {}, onApproveStock = {},
                onCancelStock = {}, onSyncNow = {},
                currentFarmId = "farm-a",
                farmNames = mapOf("farm-a" to "North Farm", "farm-b" to "South Farm"))
        } }
        compose.onNodeWithText("Needs review: 1").assertExists()
        compose.onNodeWithText("South Farm", substring = true).assertDoesNotExist()
        compose.onNode(hasText("Needs review") and hasClickAction()).performClick()
        compose.onNodeWithText("Retry this change").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("need", requestedRetry) }
        compose.onNodeWithText("All my farms").performClick()
        compose.onNodeWithText("All").performClick()
        // The third operation belongs to another farm and is initially offscreen.
        compose.onNodeWithTag("record-list").performScrollToIndex(4)
        compose.onNodeWithText("South Farm", substring = true).assertExists()
    }

    @Test fun dependentPhotoNeverSuggestsUnsafeRetryAndRequiresDiscardConfirmation() {
        var discarded = false
        val dependent = PendingPhoto("photo", "person", "farm", "crop",
            null, "inspection", "image/jpeg", 1000L,
            status = "review", errorMessage = "Inspection not confirmed")
        compose.setContent { SmartFarmTheme {
            SyncStatusScreen(emptyList(), listOf(dependent), null, null,
                onBack = {}, onRetry = {}, onDiscard = {},
                onRetryPhoto = {}, onDiscardPhoto = { discarded = true },
                onReviewStock = {}, onApproveStock = {},
                onCancelStock = {}, onSyncNow = {})
        } }
        compose.onNodeWithText("Retry photo").assertDoesNotExist()
        compose.onNodeWithText("Review the inspection entry above first.", substring = true)
            .performScrollTo().assertExists()
        compose.onNodeWithText("Discard local photo").performScrollTo().performClick()
        compose.onNodeWithText("Discard offline photo?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(false, discarded) }
    }
}
