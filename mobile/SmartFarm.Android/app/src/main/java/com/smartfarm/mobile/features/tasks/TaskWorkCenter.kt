package com.smartfarm.mobile.features.tasks

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.data.remote.api.TaskDto
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure presentation rules: never create reminders or treat a task as completed.
 * Use the farm timezone, not the phone timezone, to evaluate deadlines.
 */
object TaskWorkCenter {
    fun today(timeZoneId: String, now: Instant = Instant.now()): LocalDate =
        now.atZone(ZoneId.of(timeZoneId)).toLocalDate()

    fun dueDate(task: TaskDto): LocalDate? = runCatching {
        LocalDate.parse(task.dueDate)
    }.getOrNull()

    fun myOpenTasks(tasks: List<TaskDto>, userId: String?, isOwner: Boolean): List<TaskDto> =
        tasks.filter { task ->
            task.status == 0 &&
                (task.assignedToUserId == userId && userId != null ||
                    isOwner && task.assignedToUserId == null)
        }.sortedWith(compareBy({ dueDate(it) ?: LocalDate.MAX }, { it.id }))

    fun overdue(tasks: List<TaskDto>, today: LocalDate): List<TaskDto> =
        tasks.filter { dueDate(it)?.isBefore(today) == true }

    fun dueToday(tasks: List<TaskDto>, today: LocalDate): List<TaskDto> =
        tasks.filter { dueDate(it) == today }

    fun upcoming(tasks: List<TaskDto>, today: LocalDate): List<TaskDto> =
        tasks.filter { dueDate(it)?.isAfter(today) == true }
}

/** UI presets only. Never auto-schedule or auto-submit a job. */
data class WorkTaskPreset(val type: Int, val label: String, val suggestedDescription: String)

object WorkTaskPresets {
    val options get() = listOf(
        WorkTaskPreset(2, uiText(R.string.ui_watering_2ee1114, "Watering"), uiText(R.string.ui_area_or_plants_watered_namount_optional_a3c5809, "Area or plants watered:\nAmount (optional):")),
        WorkTaskPreset(4, uiText(R.string.ui_inspection_6e4fa13, "Inspection"), uiText(R.string.ui_what_to_check_nfindings_to_record_979a55e, "What to check:\nFindings to record:")),
        WorkTaskPreset(3, uiText(R.string.ui_fertilizing_3cc00f9, "Fertilizing"), uiText(R.string.ui_product_area_napplication_details_d7bba08, "Product / area:\nApplication details:")),
        WorkTaskPreset(5, uiText(R.string.ui_harvest_c91ab8b, "Harvest"), uiText(R.string.ui_expected_harvest_area_npreparation_b1c133d, "Expected harvest area:\nPreparation:"))
    )
}
