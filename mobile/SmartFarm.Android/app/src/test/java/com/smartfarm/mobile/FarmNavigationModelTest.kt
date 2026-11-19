package com.smartfarm.mobile

import com.smartfarm.mobile.navigation.safeFarmSection
import com.smartfarm.mobile.navigation.quickRecordRoute
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
    @Test fun shortcutsUseExistingRecordsAndRespectActiveCycleSelection() {
        assertEquals("monitor/farm/cycle?quick=watering",quickRecordRoute("farm","watering","cycle"))
        assertEquals("monitor/farm/cycle?quick=fertilizing",quickRecordRoute("farm","fertilizing","cycle"))
        assertEquals("monitor/farm/cycle?quick=inspection",quickRecordRoute("farm","inspection","cycle"))
        assertEquals("harvests/farm/cycle",quickRecordRoute("farm","harvest","cycle"))
        assertEquals("inventory/farm",quickRecordRoute("farm","supplies",null))
        assertNull(quickRecordRoute("farm","harvest",null))
        assertNull(quickRecordRoute("","supplies",null))
        assertNull(quickRecordRoute("farm","unsupported","cycle"))
        assertEquals("monitor/farm/cycle?quick=other", quickRecordRoute("farm","other","cycle"))
    }
    @Test fun explicitTaskLinksOnlyUseSupportedActivityContracts() {
        for (kind in listOf("watering", "fertilizing")) {
            assertEquals("monitor/farm/cycle?quick=$kind&taskId=task-id",
                quickRecordRoute("farm", kind, "cycle", "task-id"))
        }
        assertEquals("monitor/farm/cycle?quick=inspection", quickRecordRoute("farm", "inspection", "cycle", "task-id"))
        assertEquals("harvests/farm/cycle", quickRecordRoute("farm", "harvest", "cycle", "task-id"))
        assertEquals("monitor/farm/cycle?quick=watering&taskId=task%26other%3D1",
            quickRecordRoute("farm", "watering", "cycle", "task&other=1"))
    }

}
