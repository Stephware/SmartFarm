package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.TaskApi
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.TaskCloseInput

class TaskRepository(private val auth: AuthRepository, private val api: () -> TaskApi) {
    suspend fun tasks(farmId: String) = auth.authorized { api().tasks(it, farmId) }
    suspend fun notifications() = auth.authorized { api().notifications(it) }
    suspend fun complete(task: TaskDto) = auth.authorized {
        api().close(it, task.farmId, task.id, TaskCloseInput(1, task.rowVersion))
    }
    suspend fun read(id: String) = auth.authorized { api().read(it, id) }
}
