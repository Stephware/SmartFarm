package com.smartfarm.mobile

import com.smartfarm.mobile.navigation.safeFarmSection
import com.smartfarm.mobile.navigation.quickRecordRoute
import com.smartfarm.mobile.navigation.requiresExitConfirmation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FarmNavigationModelTest {
    @Test fun onlyKnownSectionsAreRoutable() {
        assertEquals("weather", safeFarmSection("weather"))
        assertEquals("members", safeFarmSection("members"))
        assertEquals("overview", safeFarmSection("unknown"))
        assertEquals("overview", safeFarmSection("../inventory"))
    }
    @Test fun changingFarmsWarnsBeforeLeavingAForm() {
        assertEquals(false, requiresExitConfirmation("farm/{farmId}"))
        assertEquals(false, requiresExitConfirmation("more/{farmId}"))
        assertEquals(true, requiresExitConfirmation("monitor/{farmId}/{cycleId}?quick={quick}"))
        assertEquals(true, requiresExitConfirmation("crops/{farmId}"))
        assertEquals(true, requiresExitConfirmation("farm/{farmId}/{section}"))
        assertEquals(false, requiresExitConfirmation("farm/{farmId}/{section}", "weather"))
        assertEquals(false, requiresExitConfirmation("farm/{farmId}/{section}", "members"))
        assertEquals(true, requiresExitConfirmation("farm/{farmId}/{section}", "plots"))
        assertEquals(true, requiresExitConfirmation("farm/{farmId}/{section}", "settings"))
        assertEquals(false, requiresExitConfirmation("insights/{farmId}"))
        assertEquals(false, requiresExitConfirmation("sync"))
        assertEquals(false, requiresExitConfirmation("notifications"))
        assertEquals(true, requiresExitConfirmation("inventory/{farmId}/{itemId}"))
        assertEquals(true, requiresExitConfirmation("tasks/{farmId}"))
    }
    @Test fun shortcutsUseExistingRecordsAndRespectActiveCycleSelection() {
        assertEquals("monitor/farm/cycle?quick=watering",quickRecordRoute("farm","watering","cycle"))
        assertEquals("monitor/farm/cycle?quick=fertilizing",quickRecordRoute("farm","fertilizing","cycle"))
        assertEquals("monitor/farm/cycle?quick=inspection",quickRecordRoute("farm","inspection","cycle"))
        assertEquals("harvests/farm/cycle",quickRecordRoute("farm","harvest","cycle"))
        assertEquals("inventory/farm",quickRecordRoute("farm","supplies",null))
        assertNull(quickRecordRoute("farm","harvest",null))
        assertNull(quickRecordRoute("","supplies",null))
        assertNull(quickRecordRoute("farm","unsupported","cycle"))
    }
}
