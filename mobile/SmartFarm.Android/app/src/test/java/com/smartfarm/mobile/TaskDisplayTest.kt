package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.tasks.TaskDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskDisplayTest {
    private fun task(id: String, assignee: String?, status: Int = 0, due: String = "2026-11-12") =
        TaskDto(id, "farm", null, assignee, 2, due, status, "Water seedlings", "version")

    @Test fun completionActionsMatchAssignedWorkerAndManager() {
        assertTrue(TaskDisplay.canComplete(task("mine", "worker"), "worker", false))
        assertFalse(TaskDisplay.canComplete(task("other", "another"), "worker", false))
        assertFalse(TaskDisplay.canComplete(task("none", null), "worker", false))
        assertFalse(TaskDisplay.canComplete(task("closed", "worker", 1), "worker", true))
        assertTrue(TaskDisplay.canComplete(task("other", "another"), "manager", true))
    }

    @Test fun personalHistoryCannotRevealOtherWorkersRecords() {
        val history = listOf(
            task("mine", "worker", 1),
            task("other", "other", 2),
            task("unassigned", null, 1),
            task("open", "worker", 0)
        )
        assertEquals(listOf("mine"), TaskDisplay.myHistory(history, "worker", false).map { it.id })
        assertEquals(listOf("unassigned"), TaskDisplay.myHistory(history, "owner", true).map { it.id })
    }

    @Test fun datesAreReadableAndInvalidValuesRemainVisible() {
        assertEquals("Nov 12, 2026", TaskDisplay.dueLabel("2026-11-12"))
        assertEquals("not-a-date", TaskDisplay.dueLabel("not-a-date"))
        val sorted = TaskDisplay.farmTasks(listOf(
            task("closed", "worker", 1), task("broken", "worker", due = "not-a-date"),
            task("today", "worker")
        ))
        assertEquals(listOf("today", "broken", "closed"), sorted.map { it.id })
    }
}
