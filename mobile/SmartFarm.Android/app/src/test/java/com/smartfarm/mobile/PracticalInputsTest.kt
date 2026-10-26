package com.smartfarm.mobile

import com.smartfarm.mobile.features.inventory.inventoryMovementQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class PracticalInputsTest {
    @Test fun positiveConsumptionBecomesNegativeOnlyAtTheServiceBoundary() {
        assertEquals(BigDecimal("-2.5"), inventoryMovementQuantity(3, "2.5"))
        assertEquals(BigDecimal("2.5"), inventoryMovementQuantity(2, "2.5"))
        assertEquals(BigDecimal("-2.5"), inventoryMovementQuantity(4, "-2.5"))
    }

    @Test fun invalidStockInputIsRejectedBeforeItCanBeQueued() {
        for ((type, value) in listOf(3 to "-2", 2 to "0", 4 to "oops", 2 to "1.0001")) {
            assertThrows(IllegalArgumentException::class.java) { inventoryMovementQuantity(type, value) }
        }
    }
}
