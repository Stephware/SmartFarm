package com.smartfarm.mobile

import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.farms.homeDailyWork
import com.smartfarm.mobile.features.farms.homeSyncSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FarmHomeModelTest {
    private val today = LocalDate.of(2026, 11, 12)

    private fun task(id: String, due: String) =
        TaskDto(id, "farm", null, "worker", 2, due, 0, "Water crops", "version")

    @Test fun workKeepsOverdueTodayAndUpcomingSeparate() {
        val summary = homeDailyWork(
            listOf(task("a", "2026-11-10"), task("b", "2026-11-12"), task("c", "2026-11-14")),
            today
        )
        assertEquals(listOf("a"), summary.overdue.map { it.id })
        assertEquals(listOf("b"), summary.dueToday.map { it.id })
        assertEquals(listOf("c"), summary.upcoming.map { it.id })
        assertEquals(2, summary.dueCount)
    }

    @Test fun syncCountsOnlyTheSelectedUserAndFarm() {
        fun record(id: String, user: String, farm: String, status: String) = PendingOperation(
            operationId = id, userId = user, farmId = farm,
            operationType = "activity", payloadJson = "{}", createdAtUtc = 0L,
            status = status
        )
        val ops = listOf(
            record("a", "current", "farm", "pending"),
            record("b", "current", "farm", "review"),
            record("c", "other", "farm", "failed"),
            record("d", "current", "other-farm", "failed")
        )
        val photo = PendingPhoto("photo", "current", "farm", "cycle", null,
            null, "image/jpeg", 0L, status = "pending")
        assertEquals(3, homeSyncSummary(ops, listOf(photo), "farm", "current").pending)
        assertEquals(1, homeSyncSummary(ops, listOf(photo), "farm", "current").needsReview)
        assertEquals(0, homeSyncSummary(ops, listOf(photo), "farm", null).pending)
    }
}
