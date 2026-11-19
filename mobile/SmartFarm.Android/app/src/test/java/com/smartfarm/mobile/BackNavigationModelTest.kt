package com.smartfarm.mobile

import com.smartfarm.mobile.navigation.navigationParent
import com.smartfarm.mobile.navigation.navigationTitle
import org.junit.Assert.*
import org.junit.Test

class BackNavigationModelTest {
    @Test fun secondaryEntryHasSafeContextualParentWithoutHistory() {
        assertEquals("crops/f", navigationParent("monitor/{farmId}/{cycleId}?quick={quick}&taskId={taskId}", "f"))
        assertEquals("crops/f", navigationParent("harvests/{farmId}/{cycleId}", "f"))
        assertEquals("inventory/f", navigationParent("inventory/{farmId}/{itemId}", "f"))
        for (route in listOf("crops/{farmId}", "tasks/{farmId}", "inventory/{farmId}", "farm/{farmId}/{section}", "insights/{farmId}"))
            assertEquals("farm/f", navigationParent(route, "f"))
        assertEquals("farms", navigationParent("account", ""))
        assertEquals("farms", navigationParent("sync", ""))
        assertEquals("farms", navigationParent("notifications", ""))
        assertEquals("login", navigationParent("forgot-password", ""))
        assertEquals("welcome", navigationParent("register", ""))
        assertEquals("welcome", navigationParent("onboarding-preview", ""))
        assertNull(navigationParent("farms", ""))
        assertNull(navigationParent("welcome", ""))
    }
    @Test fun absentFarmNeverCreatesAnEmptyFarmRoute() {
        assertEquals("farms", navigationParent("crops/{farmId}", ""))
        assertEquals("farms", navigationParent("unknown", ""))
    }
    @Test fun sectionAndRecordingHeadingsDescribeTheActualScreen() {
        assertEquals("Team & access", navigationTitle("farm/{farmId}/{section}", "members"))
        assertEquals("Record watering", navigationTitle("monitor/{farmId}/{cycleId}?quick={quick}&taskId={taskId}", quick = "watering"))
        assertEquals("Supplies", navigationTitle("inventory/{farmId}"))
    }
    @Test fun directEntryLabelsNameTheirConcreteFallbackParent() {
        assertEquals("Crops", navigationTitle(navigationParent("monitor/{farmId}/{cycleId}", "f")))
        assertEquals("Supplies", navigationTitle(navigationParent("inventory/{farmId}/{itemId}", "f")))
        assertEquals("Today", navigationTitle(navigationParent("tasks/{farmId}", "f")))
        assertEquals("Today", navigationTitle("farm/{farmId}/{section}", "unknown"))
    }

}
