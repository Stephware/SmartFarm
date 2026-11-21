package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import com.smartfarm.mobile.features.inventory.inventoryMovementQuantity
import com.smartfarm.mobile.features.inventory.inventoryVisibleItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class InventoryPresentationTest {
    private fun item(name: String, category: Int, low: Boolean) = InventoryItemDto(
        id = name, farmId = "farm", name = name, category = category, unit = "kg",
        description = null, quantityOnHand = BigDecimal("3.000"),
        reorderLevel = BigDecimal("5"), isLowStock = low,
        archivedAtUtc = null, rowVersion = "v1"
    )

    private val items = listOf(
        item("Rice seeds", 1, true),
        item("Fertilizer", 2, false),
        item("Tomato seeds", 1, false),
        item("Low fertilizer", 2, true)
    )

    @Test fun lowStockFilterShowsOnlyAlertsInAlphabeticalOrder() {
        assertEquals(listOf("Low fertilizer", "Rice seeds"),
            inventoryVisibleItems(items, "low", "").map { it.name })
    }

    @Test fun categoryAndSearchWorkWithoutChangingSourceBalances() {
        val original = items.map { it.quantityOnHand }
        assertEquals(listOf("Rice seeds", "Tomato seeds"),
            inventoryVisibleItems(items, "seeds", "SEEDS").map { it.name })
        assertTrue(inventoryVisibleItems(items, "pesticides", "").isEmpty())
        assertEquals(original, items.map { it.quantityOnHand })
    }

    @Test fun movementQuantityKeepsConsumptionNegativeAndReceiptPositive() {
        assertEquals(BigDecimal("-2.500"), inventoryMovementQuantity(3, "2.500"))
        assertEquals(BigDecimal("2.500"), inventoryMovementQuantity(2, "2.500"))
        assertEquals(BigDecimal("-1.250"), inventoryMovementQuantity(4, "-1.250"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeConsumption() { inventoryMovementQuantity(3, "-1") }
}
