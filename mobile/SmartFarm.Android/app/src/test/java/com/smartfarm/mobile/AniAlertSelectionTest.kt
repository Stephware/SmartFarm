package com.smartfarm.mobile
import com.smartfarm.mobile.data.remote.api.AniAlertDto
import com.smartfarm.mobile.features.tasks.importantAniAlerts
import org.junit.Assert.*
import org.junit.Test
class AniAlertSelectionTest {
    private fun alert(id: String, severity: Int = 2) = AniAlertDto(id,"farm","Weather","Saved advisory","Advisory","Open-Meteo",severity,true,false,null,null)
    @Test fun onlyFreshUnreadAuthorizedFarmInterruptionsAreShownInPriorityOrder() {
        val normal = alert("normal").copy(shouldInterrupt=false)
        val stale = alert("stale").copy(isStale=true)
        val read = alert("read").copy(readAtUtc="2026-11-20T00:00:00Z")
        val acknowledged = alert("ack").copy(acknowledgedAtUtc="2026-11-20T00:00:00Z")
        val otherFarm = alert("other").copy(farmId="other")
        assertEquals(listOf("high","important"), importantAniAlerts(listOf(normal,stale,read,acknowledged,otherFarm,alert("important"),alert("high",3)),"farm").map { it.id })
    }
}
