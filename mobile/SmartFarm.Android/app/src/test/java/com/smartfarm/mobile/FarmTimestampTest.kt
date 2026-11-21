package com.smartfarm.mobile
import com.smartfarm.mobile.ui.components.farmDateTime
import org.junit.Assert.*
import org.junit.Test
class FarmTimestampTest {
    @Test fun utcAndHistoricalUtcCrossManilaMidnightWithoutChangingDateOnly() {
        assertTrue(farmDateTime("2026-11-20T16:05:00Z",null).startsWith("Nov 21, 2026"))
        assertTrue(farmDateTime("2026-11-20T16:05:00","Asia/Manila").startsWith("Nov 21, 2026"))
        assertTrue(farmDateTime("2026-11-20T16:05:00Z","America/New_York").startsWith("Nov 20, 2026"))
        assertTrue(farmDateTime("2026-11-21T00:05:00+08:00", "Asia/Manila").startsWith("Nov 21, 2026"))
        assertTrue(farmDateTime("2026-11-20T16:05:00+00:00", "Asia/Manila").startsWith("Nov 21, 2026"))
        assertEquals("2026-11-20", farmDateTime("2026-11-20","Asia/Manila"))
    }
}
