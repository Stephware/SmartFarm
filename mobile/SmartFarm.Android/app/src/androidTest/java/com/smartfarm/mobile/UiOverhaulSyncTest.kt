package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import com.smartfarm.mobile.ui.components.PendingRecords
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiOverhaulSyncTest {
    @get:Rule val compose = createComposeRule()

    private fun queued(status: String = "pending") = PendingOperation(
        operationId = "stock-op",
        userId = "person",
        farmId = "farm",
        operationType = "inventory",
        payloadJson = """{"targetId":"item","movement":{"quantityChange":"-2"}}""",
        createdAtUtc = 0L,
        status = status
    )

    @Test fun queuedRecordsAreLabeledAsLocalNotServerConfirmed() {
        var openedSync = false
        compose.setContent {
            SmartFarmTheme {
                PendingRecords(
                    operations = listOf(queued()),
                    farmId = "farm",
                    targetId = "item",
                    types = setOf("inventory"),
                    onSync = { openedSync = true }
                )
            }
        }
        compose.onAllNodesWithText("Saved on phone").assertCountEquals(2)
        compose.onNodeWithText("Stock quantity change: -2").assertExists()
        compose.onNodeWithText("Uploads automatically when a connection is available.").assertExists()
        compose.onNodeWithText("Review saved work").assertDoesNotExist()
        compose.runOnIdle { assertEquals(false, openedSync) }
    }

    @Test fun stockConflictsMustBeReviewedBeforeRetryOrDiscard() {
        var reviewed: String? = null
        var discarded: String? = null
        compose.setContent {
            SmartFarmTheme {
                SyncStatusScreen(
                    operations = listOf(queued("review")),
                    photos = emptyList(),
                    stockPreview = null,
                    error = null,
                    onBack = {}, onRetry = {}, onDiscard = { discarded = it },
                    onRetryPhoto = {}, onDiscardPhoto = {},
                    onReviewStock = { reviewed = it }, onApproveStock = {},
                    onCancelStock = {}, onSyncNow = {}
                )
            }
        }

        compose.onNodeWithText("Needs review: 1").assertExists()
        compose.onNodeWithText("Check latest stock").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("stock-op", reviewed) }

        compose.onNodeWithText("Discard and re-enter").performScrollTo().performClick()
        compose.onNodeWithText("Discard unsynced change?").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Discard unsynced change?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(null, discarded) }
    }
}
