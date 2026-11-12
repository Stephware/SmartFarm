package com.smartfarm.mobile

import com.smartfarm.mobile.features.inventory.inventoryCorrectionDelta
import com.smartfarm.mobile.features.inventory.inventoryBalancePreview
import com.smartfarm.mobile.ui.components.farmDateTime
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InventoryWorkflowTest {
    @Test fun targetBalanceConvertsToSignedExistingApiAdjustment() {
        assertEquals(BigDecimal("-3"), inventoryCorrectionDelta(BigDecimal("5"), "2"))
        assertEquals(BigDecimal("5.5"), inventoryCorrectionDelta(BigDecimal("5"), "10.5"))
        assertEquals(BigDecimal("-5"), inventoryCorrectionDelta(BigDecimal("5"), "0"))
        assertNull(inventoryCorrectionDelta(BigDecimal("5"), "5"))
        assertNull(inventoryCorrectionDelta(BigDecimal("5"), "-1"))
        assertNull(inventoryCorrectionDelta(BigDecimal("5"), "2.1234"))
    }
    @Test fun noPreviewForInvalidOrInsufficientStock() {
        assertEquals(BigDecimal("7"), inventoryBalancePreview(BigDecimal("5"), 2, "2"))
        assertEquals(BigDecimal("3"), inventoryBalancePreview(BigDecimal("5"), 3, "2"))
        assertEquals(BigDecimal("2"), inventoryBalancePreview(BigDecimal("5"), 4, "2"))
        assertNull(inventoryBalancePreview(BigDecimal("5"), 3, "6"))
        assertNull(inventoryBalancePreview(BigDecimal("5"), 4, "-2"))
        assertNull(inventoryBalancePreview(BigDecimal("5"), 4, "5"))
    }
    @Test fun datesAreFarmLocalAndSafeIfMissing() {
        assertEquals("Nov 12, 2026 · 8:30 AM (Asia/Manila)",
            farmDateTime("2026-11-12T00:30:00Z", "Asia/Manila"))
        assertEquals("Nov 12, 2026 · 12:30 AM (UTC)",
            farmDateTime("2026-11-12T00:30:00Z", "Not/A/Timezone"))
        assertEquals("unknown", farmDateTime("unknown", "Asia/Manila"))
    }
}
