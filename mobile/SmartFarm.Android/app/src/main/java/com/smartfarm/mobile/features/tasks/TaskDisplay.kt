package com.smartfarm.mobile.features.tasks

import com.smartfarm.mobile.data.remote.api.TaskDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** UI-only grouping. API rules remain authoritative for task completion. */
internal object TaskDisplay {
    fun canComplete(task: TaskDto, actorId: String?, canManage: Boolean): Boolean =
        task.status == 0 && (canManage ||
            (actorId != null && task.assignedToUserId == actorId))

    fun myHistory(tasks: List<TaskDto>, actorId: String?, isOwner: Boolean): List<TaskDto> =
        tasks.filter { task ->
            task.status != 0 && (actorId != null && task.assignedToUserId == actorId ||
                isOwner && task.assignedToUserId == null)
        }.sortedWith(compareByDescending<TaskDto> { TaskWorkCenter.dueDate(it) ?: LocalDate.MIN }
            .thenBy { it.id })

    fun dueLabel(date: String): String =
        runCatching {
            LocalDate.parse(date).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
        }.getOrDefault(date)

    /** Keep tasks with malformed dates discoverable in the management list rather than hiding them. */
    fun farmTasks(tasks: List<TaskDto>): List<TaskDto> =
        tasks.sortedWith(compareBy<TaskDto>({ it.status != 0 },
            { TaskWorkCenter.dueDate(it) ?: LocalDate.MAX }, { it.id }))
}
