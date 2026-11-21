package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.monitoring.MonitoringUiState
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CombinedFieldworkScreenTest {
    @get:Rule val compose = createComposeRule()

    private val fertilizer = InventoryItemDto(
        id = "item-1", farmId = "farm", name = "Organic fertilizer",
        category = 2, unit = "kg", description = null,
        quantityOnHand = BigDecimal("5.000"), reorderLevel = null,
        isLowStock = false, archivedAtUtc = null, rowVersion = "row")

    @Test fun combinedEntryUsesOneCallbackNotIndependentActivitySave() {
        var saved: String? = null
        var separateActivitySaved = false
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(
                MonitoringUiState(farmId = "farm", cycleId = "crop",
                    canRecord = true, supplies = listOf(fertilizer)),
                onBack = {}, onActivity = { _, _ -> separateActivitySaved = true },
                onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> },
                onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {},
                onActivityWithStock = { kind, _, item, quantity ->
                    saved = "$kind/${item.id}/$quantity"
                }
            )
        } }
        compose.onNodeWithText("Fertilizing").performScrollTo().performClick()
        compose.onNodeWithText("Deduct supply from farm inventory")
            .performScrollTo().performClick()
        compose.onNodeWithText("Choose supply").performScrollTo().performClick()
        compose.onNodeWithText("Organic fertilizer", substring = true)
            .performClick()
        compose.onNodeWithText("Quantity used (kg)")
            .performScrollTo().performTextInput("1.500")
        compose.onNodeWithText("Save activity and supply use")
            .performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("2/item-1/1.500", saved)
            assertEquals(false, separateActivitySaved)
        }
    }

    @Test fun insufficientSupplyBlocksCombinedSubmission() {
        var saved: String? = null
        compose.setContent { SmartFarmTheme {
            MonitoringScreen(
                MonitoringUiState(farmId = "farm", cycleId = "crop",
                    canRecord = true, supplies = listOf(fertilizer)),
                onBack = {}, onActivity = { _, _ -> },
                onObservation = { _, _, _, _ -> }, onUpload = { _, _ -> },
                onPreview = {}, onThumbnail = { byteArrayOf() },
                onDismissPreview = {},
                onActivityWithStock = { _, _, _, qty -> saved = qty }
            )
        } }
        compose.onNodeWithText("Fertilizing").performScrollTo().performClick()
        compose.onNodeWithText("Deduct supply from farm inventory")
            .performScrollTo().performClick()
        compose.onNodeWithText("Choose supply").performScrollTo().performClick()
        compose.onNodeWithText("Organic fertilizer", substring = true).performClick()
        compose.onNodeWithText("Quantity used (kg)")
            .performScrollTo().performTextInput("8")
        compose.onNodeWithText("Save activity and supply use").assertIsNotEnabled()
        compose.runOnIdle { assertNull(saved) }
    }
}
