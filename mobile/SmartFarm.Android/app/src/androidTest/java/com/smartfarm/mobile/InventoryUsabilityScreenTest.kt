package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import com.smartfarm.mobile.data.remote.dto.InventoryActivityOptionDto
import com.smartfarm.mobile.features.inventory.InventoryUiState
import com.smartfarm.mobile.features.inventory.InventoryItemScreen
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InventoryUsabilityScreenTest {
    @get:Rule val compose = createComposeRule()
    private val item = InventoryItemDto(
        id = "supply", farmId = "farm", name = "Fertilizer", category = 2, unit = "kg",
        description = null, quantityOnHand = BigDecimal("5"), reorderLevel = BigDecimal("2"),
        isLowStock = false, archivedAtUtc = null, rowVersion = "v1"
    )
    private val state = InventoryUiState(farmId = "farm", itemId = "supply", item = item,
        activities = listOf(InventoryActivityOptionDto("activity", "Fertilized north plot")))

    @Test fun correctionShowsConfirmedBeforeAfterAndUsesExistingAdjustment() {
        var recordedType: Int? = null
        var recordedQuantity: String? = null
        compose.setContent { SmartFarmTheme {
            InventoryItemScreen(state, true, {}, { _, _, _, _, _ -> }, {},
                { kind, quantity, _, _ -> recordedType = kind; recordedQuantity = quantity },
                { _, _ -> })
        } }
        compose.onNodeWithText("Correct stock count").performScrollTo().performClick()
        compose.onNodeWithText("Correct total stock (kg)").performScrollTo().performTextInput("2")
        compose.onNodeWithText("After sync: 2 kg").performScrollTo().assertExists()
        compose.onNodeWithText("Reason for correction").performScrollTo().performTextInput("Counted bags")
        compose.onNodeWithText("Save corrected count").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(4, recordedType)
            assertEquals("-3", recordedQuantity)
        }
    }

    @Test fun workersCanUseOnlyAndMustLinkAnActualActivity() {
        var saved: Pair<Int, String?>? = null
        compose.setContent { SmartFarmTheme {
            InventoryItemScreen(state, false, {}, { _, _, _, _, _ -> }, {},
                { kind, _, activity, _ -> saved = kind to activity }, { _, _ -> })
        } }
        compose.onNodeWithText("Add stock").assertDoesNotExist()
        compose.onNodeWithText("Correct stock count").assertDoesNotExist()
        compose.onNodeWithText("Quantity used (kg)").performScrollTo().performTextInput("2")
        compose.onNodeWithText("Save supply use").assertIsNotEnabled()
        compose.onNodeWithText("Choose recorded activity").performScrollTo().performClick()
        compose.onNodeWithText("Fertilized north plot").performClick()
        compose.onNodeWithText("Save supply use").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3 to "activity", saved) }
    }
}
