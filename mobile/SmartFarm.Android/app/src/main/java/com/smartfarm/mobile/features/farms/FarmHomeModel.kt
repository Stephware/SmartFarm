package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.features.tasks.TaskWorkCenter
import java.time.LocalDate

/** Client-side presentation only; task lists still come from the live server. */
internal data class HomeDailyWork(
    val overdue: List<TaskDto>,
    val dueToday: List<TaskDto>,
    val upcoming: List<TaskDto>
) {
    val dueCount: Int get() = overdue.size + dueToday.size
}

internal fun homeDailyWork(tasks: List<TaskDto>, today: LocalDate): HomeDailyWork =
    HomeDailyWork(
        overdue = TaskWorkCenter.overdue(tasks, today),
        dueToday = TaskWorkCenter.dueToday(tasks, today),
        upcoming = TaskWorkCenter.upcoming(tasks, today)
    )

internal data class HomeSyncSummary(val pending: Int, val needsReview: Int)

/** Scope local records to both signed-in user and chosen farm; never count them as server totals. */
internal fun homeSyncSummary(
    operations: List<PendingOperation>,
    photos: List<PendingPhoto>,
    farmId: String,
    userId: String?
): HomeSyncSummary {
    if (farmId.isBlank() || userId.isNullOrBlank()) return HomeSyncSummary(0, 0)
    val ops = operations.filter { it.farmId == farmId && it.userId == userId }
    val images = photos.filter { it.farmId == farmId && it.userId == userId }
    fun needsReview(status: String, error: String?): Boolean =
        status == "review" || status == "failed" || !error.isNullOrBlank()
    return HomeSyncSummary(
        pending = ops.size + images.size,
        needsReview = ops.count { needsReview(it.status, it.errorMessage) } +
            images.count { needsReview(it.status, it.errorMessage) }
    )
}
