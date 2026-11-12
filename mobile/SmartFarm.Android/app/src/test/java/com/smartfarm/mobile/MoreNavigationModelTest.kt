package com.smartfarm.mobile

import com.smartfarm.mobile.navigation.moreFarmTools
import com.smartfarm.mobile.navigation.moreManagement
import com.smartfarm.mobile.navigation.morePersonal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreNavigationModelTest {
    @Test fun ownersAndManagersGetSettingsButWorkersDoNot() {
        assertTrue(moreManagement("Owner").any { it.route == "settings" })
        assertTrue(moreManagement("Manager").any { it.route == "settings" })
        assertFalse(moreManagement("FieldWorker").any { it.route == "settings" })
        assertTrue(moreManagement("FieldWorker").any { it.route == "members" })
    }
    @Test fun menuKeepsEverySecondaryDestinationReachable() {
        val all = moreFarmTools("Owner") + moreManagement("Owner") + morePersonal
        assertEquals(all.map { it.route }.size, all.map { it.route }.distinct().size)
        assertTrue(all.map { it.route }.containsAll(listOf(
            "inventory", "weather", "plots", "insights", "members",
            "settings", "notifications", "sync", "account")))
        assertTrue(moreFarmTools(null).isEmpty())
        assertTrue(moreManagement(null).isEmpty())
    }
}
