package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.TaskApi
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.TaskCloseInput
import com.smartfarm.mobile.data.remote.api.TaskCreateInput
import com.smartfarm.mobile.data.remote.api.TaskUpdateInput

class TaskRepository(private val auth: AuthRepository, private val api: () -> TaskApi) {
    suspend fun tasks(farmId: String) = auth.authorized { api().tasks(it, farmId) }
    suspend fun create(farmId: String, input: TaskCreateInput) = auth.authorized {
        api().create(it, farmId, input)
    }
    suspend fun update(task: TaskDto, input: TaskUpdateInput) = auth.authorized {
        api().update(it, task.farmId, task.id, input)
    }
    suspend fun notificationFarms(ids: Set<String>): Map<String, String?> {
        val found = mutableMapOf<String, String?>()
        var page = 1
        do {
            val result = auth.authorized { api().alertPage(it, page) }
            result.items.filter { it.id in ids }.forEach { found[it.id] = it.farmId }
            page++
        } while (result.hasMore && !found.keys.containsAll(ids))
        return found
    }
    suspend fun alerts() = auth.authorized { api().alerts(it).items }
    suspend fun acknowledge(id: String) = auth.authorized { api().acknowledge(it, id) }
    suspend fun notifications() = auth.authorized { api().notifications(it) }
    suspend fun complete(task: TaskDto) = auth.authorized {
        api().close(it, task.farmId, task.id, TaskCloseInput(1, task.rowVersion))
    }
    suspend fun read(id: String) = auth.authorized { api().read(it, id) }
}
