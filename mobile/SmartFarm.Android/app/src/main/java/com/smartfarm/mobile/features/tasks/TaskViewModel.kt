package com.smartfarm.mobile.features.tasks

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.NotificationDto
import com.smartfarm.mobile.data.remote.api.TaskCreateInput
import com.smartfarm.mobile.data.remote.api.TaskUpdateInput
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.data.remote.dto.FarmMemberDto
import com.smartfarm.mobile.data.repository.TaskRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.CropRepository
import com.smartfarm.mobile.data.repository.NetworkErrorMessages
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskFormSave(val form: String, val receipt: String)

data class TaskUiState(
    val farmId: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val tasks: List<TaskDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val notificationTimeZones: Map<String, String> = emptyMap(),
    val members: List<FarmMemberDto> = emptyList(),
    val cycles: List<CropCycleDto> = emptyList(),
    val timeZoneId: String? = null,
    val choicesReady: Boolean = false,
    val choicesError: String? = null,
    val savedCount: Int = 0,
    val lastFormSave: TaskFormSave? = null
)

data class AniAlertState(val farmId: String? = null, val busy: Boolean = false,
    val items: List<com.smartfarm.mobile.data.remote.api.AniAlertDto> = emptyList(), val error: String? = null)
internal fun importantAniAlerts(items: List<com.smartfarm.mobile.data.remote.api.AniAlertDto>, farmId: String) =
    items.filter { it.farmId == farmId && it.shouldInterrupt && !it.isStale && it.readAtUtc == null && it.acknowledgedAtUtc == null }
        .sortedByDescending { it.severity ?: 0 }

class TaskViewModel(
    private val repository: TaskRepository,
    private val farmRepository: FarmRepository,
    private val cropRepository: CropRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(TaskUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var alertJob: Job? = null
    private val alertMutable = MutableStateFlow(AniAlertState())
    val alerts = alertMutable.asStateFlow()
    fun loadAlerts(farmId: String) {
        alertJob?.cancel()
        alertMutable.value = AniAlertState(farmId, busy = true)
        alertJob = viewModelScope.launch {
            try { alertMutable.value = AniAlertState(farmId, items = importantAniAlerts(repository.alerts(), farmId)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { alertMutable.value = AniAlertState(farmId, error = uiText(R.string.ui_alerts_unavailable_reconnect_and_refresh_df192e6, "Alerts unavailable. Reconnect and refresh.")) }
        }
    }
    fun acknowledgeAlert(id: String) {
        if (alertMutable.value.busy || alertMutable.value.items.none { it.id == id }) return
        alertMutable.value = alertMutable.value.copy(busy = true, error = null)
        alertJob = viewModelScope.launch {
            try {
                repository.acknowledge(id)
                alertMutable.value = alertMutable.value.copy(busy = false, items = alertMutable.value.items.filterNot { it.id == id })
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { alertMutable.value = alertMutable.value.copy(busy = false, error = uiText(R.string.ui_could_not_acknowledge_reconnect_and_try_again_a4a9ff7, "Could not acknowledge. Reconnect and try again.")) }
        }
    }
    private val formSaves = mutableMapOf<String, TaskFormSave>()

    fun load(farmId: String? = null) {
        job?.cancel()
        mutable.value = TaskUiState(farmId = farmId, busy = true, lastFormSave = formSaves[farmId])
        job = viewModelScope.launch {
            try {
                if (farmId == null) {
                    val notifications = repository.notifications()
                    val zones = try {
                        val contexts = repository.notificationFarms(notifications.map { it.id }.toSet())
                        val farms = farmRepository.listSnapshot()
                        val byFarm = farms.value.associate { it.id to it.timeZoneId }
                        contexts.mapNotNull { (id, farm) ->
                            (if (farm == null) "Asia/Manila" else byFarm[farm])?.let { id to it }
                        }.toMap()
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { emptyMap() }
                    mutable.value = TaskUiState(notifications = notifications, notificationTimeZones = zones)
                    return@launch
                }
                // Task list requires a live connection; don't present old tasks as current work.
                val tasks = repository.tasks(farmId)
                val context = try {
                    val farm = farmRepository.farmSnapshot(farmId)
                    val members = farmRepository.membersSnapshot(farmId)
                    val cycles = cropRepository.cyclesSnapshot(farmId)
                    TaskUiState(
                        farmId = farmId, tasks = tasks, members = members.value,
                        cycles = cycles.value, timeZoneId = farm.value.timeZoneId,
                        choicesReady = !farm.offline && !members.offline && !cycles.offline,
                        choicesError = if (farm.offline || members.offline || cycles.offline)
                            uiText(R.string.ui_farm_options_are_saved_copies_reconnect_and_refresh__6b97598, "Farm options are saved copies. Reconnect and refresh before assigning work.")
                        else null
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    TaskUiState(
                        farmId = farmId, tasks = tasks,
                        choicesError = uiText(R.string.ui_farm_options_could_not_be_loaded_refresh_before_crea_739aa63, "Farm options could not be loaded. Refresh before creating or editing tasks.")
                    )
                }
                mutable.value = context.copy(lastFormSave = formSaves[farmId])
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutable.value = mutable.value.copy(
                    error = NetworkErrorMessages.forRequest(
                        error, uiText(R.string.ui_tasks_require_a_connection_reconnect_and_refresh_bef_f855ba2, "Tasks require a connection. Reconnect and refresh before acting.")
                    )
                )
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    private fun change(farmId: String?, success: String, form: String? = null, mutation: suspend () -> Unit) {
        if (mutable.value.busy || farmId != mutable.value.farmId) return
        mutable.value = mutable.value.copy(busy = true, error = null, notice = null)
        job = viewModelScope.launch {
            try {
                mutation()
                if (farmId != null && form != null) {
                    formSaves[farmId] = TaskFormSave(form, java.util.UUID.randomUUID().toString())
                }
                // A successful response is definitive. Refresh failures never imply retrying POST.
                mutable.value = mutable.value.copy(
                    notice = success, savedCount = mutable.value.savedCount + 1, lastFormSave = formSaves[farmId]
                )
                try {
                    if (farmId == null) {
                        mutable.value = mutable.value.copy(notifications = repository.notifications())
                    } else {
                        mutable.value = mutable.value.copy(tasks = repository.tasks(farmId))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutable.value = mutable.value.copy(
                        error = uiText(R.string.ui_saved_on_the_server_but_refreshing_the_list_failed_r_1f344dd, "Saved on the server, but refreshing the list failed. Refresh before doing anything else.")
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutable.value = mutable.value.copy(
                    error = NetworkErrorMessages.forMutation(error,
                        uiText(R.string.ui_the_result_could_not_be_confirmed_refresh_the_list_b_3b9e1b8, "The result could not be confirmed. Refresh the list before resubmitting to avoid duplicates."))
                )
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun create(input: TaskCreateInput) {
        val current = mutable.value
        if (!current.choicesReady || current.farmId == null) return
        change(current.farmId, uiText(R.string.ui_task_created_9c02f26, "Task created."), "new-task") { repository.create(current.farmId, input) }
    }

    fun update(task: TaskDto, input: TaskUpdateInput) {
        val current = mutable.value
        if (!current.choicesReady || current.farmId != task.farmId) return
        change(task.farmId, uiText(R.string.ui_task_updated_204dad8, "Task updated."), "task-${task.id}") { repository.update(task, input) }
    }

    fun complete(task: TaskDto) = change(task.farmId, uiText(R.string.ui_task_completed_4d76c48, "Task completed.")) {
        repository.complete(task)
    }

    fun read(id: String) = change(null, uiText(R.string.ui_notification_marked_as_read_0ad6c4c, "Notification marked as read.")) { repository.read(id) }

    fun clear() {
        alertJob?.cancel()
        alertMutable.value = AniAlertState()
        job?.cancel()
        formSaves.clear()
        mutable.value = TaskUiState()
    }

    class Factory(
        private val tasks: TaskRepository,
        private val farms: FarmRepository,
        private val crops: CropRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TaskViewModel(tasks, farms, crops) as T
    }
}
