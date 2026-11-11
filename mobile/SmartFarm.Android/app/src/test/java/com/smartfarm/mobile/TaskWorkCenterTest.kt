package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import com.smartfarm.mobile.features.tasks.WorkTaskPresets
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskWorkCenterTest {
    private fun task(
        id: String, due: String, assignedTo: String? = "worker", status: Int = 0
    ) = TaskDto(id, "farm", null, assignedTo, 4, due, status, null, "row")

    @Test fun farmCalendarDayNotDeviceCalendarDay() {
        val utc = Instant.parse("2026-11-10T16:30:00Z")
        assertEquals(LocalDate.parse("2026-11-11"), TaskWorkCenter.today("Asia/Manila", utc))
        assertEquals(LocalDate.parse("2026-11-10"), TaskWorkCenter.today("UTC", utc))
    }

    @Test fun personalWorkIncludesOnlyOpenAssignedTasksAndOwnerUnassigned() {
        val all = listOf(
            task("mine", "2026-11-11"),
            task("someone-else", "2026-11-11", "other"),
            task("unassigned", "2026-11-11", null),
            task("closed", "2026-11-10", status = 1)
        )
        assertEquals(listOf("mine"), TaskWorkCenter.myOpenTasks(all, "worker", false).map { it.id })
        assertEquals(listOf("unassigned"), TaskWorkCenter.myOpenTasks(all, "owner", true).map { it.id })
        assertTrue(TaskWorkCenter.myOpenTasks(all, "owner", false).isEmpty())
    }

    @Test fun dueBucketsDoNotCountCompletedWorkOrInvalidDates() {
        val today = LocalDate.parse("2026-11-11")
        val assigned = TaskWorkCenter.myOpenTasks(listOf(
            task("old", "2026-11-10"),
            task("now", "2026-11-11"),
            task("next", "2026-11-12"),
            task("broken", "not-a-date"),
            task("done", "2026-11-10", status = 1)
        ), "worker", false)
        assertEquals(listOf("old"), TaskWorkCenter.overdue(assigned, today).map { it.id })
        assertEquals(listOf("now"), TaskWorkCenter.dueToday(assigned, today).map { it.id })
        assertEquals(listOf("next"), TaskWorkCenter.upcoming(assigned, today).map { it.id })
    }

    @Test fun presetsOnlySupplyFormTextNotCalendarSchedules() {
        assertEquals(4, WorkTaskPresets.options.size)
        assertTrue(WorkTaskPresets.options.any { it.type == 2 })
        assertFalse(WorkTaskPresets.options.any { it.suggestedDescription.isBlank() })
        assertTrue(WorkTaskPresets.options.all { it.suggestedDescription.length <= 500 })
    }
}
