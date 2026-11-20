package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.math.BigDecimal

data class FarmUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val farms: List<FarmDto> = emptyList(),
    val selected: FarmDto? = null,
    val plots: List<PlotDto> = emptyList(),
    val members: List<FarmMemberDto> = emptyList(),
    val myRequests: List<FarmJoinRequestDto> = emptyList(),
    val enrollment: FarmEnrollmentOverviewDto? = null,
    val enrollmentMessage: String? = null,
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null,
    val weather: WeatherOverviewDto? = null,
    val weatherOffline: Boolean = false,
    val weatherCachedAtUtcMillis: Long? = null,
    val weatherMessage: String? = null,
    val joinSaveVersion: Int = 0,
    val plotSaveVersion: Int = 0,
    val plotEditSaveVersion: Int = 0,
    val savedPlotId: String? = null
)

class FarmViewModel(
    private val repo: FarmRepository,
    private val weatherRepo: WeatherRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(FarmUiState())
    val state = mutable.asStateFlow()
    private var loadJob: Job? = null
    private var actionJob: Job? = null

    private fun perform(block: suspend () -> Unit) {
        if (mutable.value.loading) return
        mutable.value = mutable.value.copy(loading = true, error = null)
        actionJob = viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                mutable.value = mutable.value.copy(error = enrollmentError(error,
                    uiText(R.string.ui_could_not_complete_the_request_check_permissions_cur_f1d9819, "Could not complete the request. Check permissions, current data and connection.")))
            } finally { mutable.value = mutable.value.copy(loading = false) }
        }
    }


    private fun enrollmentError(error: Exception, fallback: String): String =
        when ((error as? HttpException)?.code()) {
            429 -> uiText(R.string.ui_too_many_requests_please_wait_and_try_again_02f43f1, "Too many requests. Please wait and try again.")
            409 -> uiText(R.string.ui_this_request_changed_refresh_to_load_the_latest_farm_4d5a6cb, "This request changed. Refresh to load the latest farm access details.")
            else -> fallback
        }

    fun clear() {
        loadJob?.cancel()
        actionJob?.cancel()
        mutable.value = FarmUiState()
    }

    fun load() {
        clear()
        mutable.value = FarmUiState(loading = true)
        loadJob = viewModelScope.launch {
            try {
                val snapshot = repo.listSnapshot()
                mutable.value = mutable.value.copy(farms = snapshot.value, offline = snapshot.offline,
                    cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
                loadMyRequests()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.value = mutable.value.copy(error = uiText(R.string.ui_could_not_load_farms_check_your_connection_7ec686d, "Could not load farms. Check your connection."))
            } finally { mutable.value = mutable.value.copy(loading = false) }
        }
    }

    private suspend fun loadMyRequests() {
        mutable.value = mutable.value.copy(myRequests = emptyList(), enrollmentMessage = null)
        try {
            mutable.value = mutable.value.copy(myRequests = repo.myJoinRequests())
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            mutable.value = mutable.value.copy(enrollmentMessage = enrollmentError(error,
                uiText(R.string.ui_join_request_status_unavailable_connect_and_refresh__cc6ef45, "Join request status unavailable. Connect and refresh to check owner approval.")))
        }
    }

    private suspend fun loadEnrollment(farmId: String) {
        mutable.value = mutable.value.copy(enrollment = null, enrollmentMessage = null)
        if (mutable.value.selected?.role != "Owner") return
        if (mutable.value.offline) {
            mutable.value = mutable.value.copy(enrollmentMessage = uiText(R.string.ui_join_requests_and_farm_code_are_unavailable_offline_e6bb540, "Join requests and farm code are unavailable offline."))
            return
        }
        try {
            val overview = repo.enrollment(farmId)
            mutable.value = mutable.value.copy(enrollment = overview)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            mutable.value = mutable.value.copy(enrollmentMessage = enrollmentError(error,
                uiText(R.string.ui_join_requests_and_farm_code_are_unavailable_refresh__2ceaf11, "Join requests and farm code are unavailable. Refresh to try again.")))
        }
    }

    fun loadDetail(farmId: String) {
        loadJob?.cancel()
        actionJob?.cancel()
        mutable.value = if (mutable.value.selected?.id == farmId)
            mutable.value.copy(loading = true, error = null, enrollment = null, myRequests = emptyList(),
                enrollmentMessage = null) else FarmUiState(loading = true,
                farms = mutable.value.farms)
        loadJob = viewModelScope.launch {
            try {
                val farm = repo.farmSnapshot(farmId)
                val plots = repo.plotsSnapshot(farmId)
                val members = repo.membersSnapshot(farmId)
                val weatherData = try {
                    weatherRepo.overview(farmId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null // Weather availability must not prevent opening a farm.
                }
                // Cached weather must not disable online farm management.
                // Only farm, plot and member snapshots control the farm offline state.
                val snapshots = listOf(farm, plots, members)
                val cached = snapshots.any { it.offline }
                mutable.value = mutable.value.copy(
                    selected = farm.value, plots = plots.value, members = members.value,
                    offline = cached,
                    cachedAtUtcMillis = snapshots.filter { it.offline }
                        .minOfOrNull { it.savedAtUtcMillis },
                    weather = weatherData?.value,
                    weatherOffline = weatherData?.offline == true,
                    weatherCachedAtUtcMillis = weatherData?.takeIf { it.offline }
                        ?.savedAtUtcMillis,
                    weatherMessage = if (weatherData == null) uiText(R.string.ui_weather_data_unavailable_da1a726, "Weather data unavailable.") else null)
                loadEnrollment(farmId)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.value = mutable.value.copy(error =
                    uiText(R.string.ui_could_not_load_this_farm_check_permissions_and_conne_5bbbf3a, "Could not load this farm. Check permissions and connection."))
            } finally {
                mutable.value = mutable.value.copy(loading = false)
            }
        }
    }

    private fun coordinates(latitude: String, longitude: String): Pair<BigDecimal?, BigDecimal?> {
        val lat = latitude.trim().takeIf { it.isNotEmpty() }
        val lon = longitude.trim().takeIf { it.isNotEmpty() }
        require((lat == null) == (lon == null)) { "Both coordinates are required." }
        val a = lat?.toBigDecimalOrNull()
        val b = lon?.toBigDecimalOrNull()
        require((lat == null || a != null) && (lon == null || b != null))
        require(a == null || (a >= BigDecimal("-90") && a <= BigDecimal("90") && a.scale() <= 6))
        require(b == null || (b >= BigDecimal("-180") && b <= BigDecimal("180") && b.scale() <= 6))
        return a to b
    }

    fun create(name: String, location: String, timeZone: String,
        latitude: String, longitude: String, onSuccess: (String) -> Unit) = perform {
        val (lat, lon) = coordinates(latitude, longitude)
        val farm = repo.create(FarmInputDto(name.trim(), location.trim(),
            timeZone.trim(), lat, lon))
        onSuccess(farm.id)
    }

    fun saveFarm(name: String, location: String, timeZone: String,
        latitude: String, longitude: String, onSuccess: () -> Unit) = perform {
        val current = requireNotNull(mutable.value.selected)
        val (lat, lon) = coordinates(latitude, longitude)
        val updated = repo.updateFarm(current.id, FarmInputDto(
            name.trim(), location.trim(), timeZone.trim(), lat, lon,
            requireNotNull(current.rowVersion)))
        mutable.value = mutable.value.copy(
            selected = updated, weather = null, weatherOffline = false,
            weatherCachedAtUtcMillis = null,
            weatherMessage = uiText(R.string.ui_farm_settings_saved_reopen_this_farm_to_refresh_weat_07fb4c3, "Farm settings saved. Reopen this farm to refresh weather."))
        onSuccess()
    }

    fun archiveFarm(onSuccess: () -> Unit) = perform {
        val farm = requireNotNull(mutable.value.selected)
        repo.archiveFarm(farm.id, RowVersionDto(requireNotNull(farm.rowVersion)))
        mutable.value = mutable.value.copy(selected = null, plots = emptyList(), members = emptyList(), enrollment = null)
        onSuccess()
    }

    fun addPlot(name: String, area: String, onSaved: () -> Unit = {}) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.createPlot(farmId, PlotInputDto(name.trim(), BigDecimal(area)))
        mutable.value = mutable.value.copy(plots = repo.plots(farmId),
            plotSaveVersion = mutable.value.plotSaveVersion + 1)
        onSaved()
    }

    fun savePlot(plot: PlotDto, name: String, area: String) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.updatePlot(farmId, plot.id, PlotInputDto(name.trim(), BigDecimal(area), plot.rowVersion))
        mutable.value = mutable.value.copy(plots = repo.plots(farmId),
            plotEditSaveVersion = mutable.value.plotEditSaveVersion + 1, savedPlotId = plot.id)
    }

    fun archivePlot(plot: PlotDto) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.archivePlot(farmId, plot.id, RowVersionDto(plot.rowVersion))
        mutable.value = mutable.value.copy(plots = repo.plots(farmId))
    }

    fun requestToJoin(code: String, onSaved: () -> Unit = {}) = perform {
        require(code.isNotBlank() && !mutable.value.offline)
        repo.requestToJoin(code)
        onSaved()
        loadMyRequests()
        mutable.value = mutable.value.copy(
            enrollmentMessage = mutable.value.enrollmentMessage ?:
                uiText(R.string.ui_request_sent_farm_access_awaits_owner_approval_b8538c6, "Request sent. Farm access awaits owner approval."),
            joinSaveVersion = mutable.value.joinSaveVersion + 1)
    }

    fun cancelJoinRequest(request: FarmJoinRequestDto) = perform {
        require(!mutable.value.offline && mutable.value.myRequests.any {
            it.id == request.id && it.farmId == request.farmId && it.status == "Pending"
        })
        repo.cancelJoinRequest(request.farmId, request.id)
        loadMyRequests()
        if (mutable.value.enrollmentMessage == null) mutable.value = mutable.value.copy(
            enrollmentMessage = uiText(R.string.ui_request_canceled_you_can_apply_again_with_the_farm_s_cfaff52, "Request canceled. You can apply again with the farm's current code."))
    }

    fun regenerateJoinCode(currentCode: String) = perform {
        val farm = requireNotNull(mutable.value.selected)
        val overview = requireNotNull(mutable.value.enrollment)
        require(farm.role == "Owner" && !mutable.value.offline && overview.joinCode == currentCode)
        val updated = try {
            repo.regenerateJoinCode(farm.id, currentCode)
        } catch (error: HttpException) {
            if (error.code() == 409) mutable.value = mutable.value.copy(enrollment = null)
            throw error
        }
        mutable.value = mutable.value.copy(enrollment = overview.copy(joinCode = updated.joinCode),
            enrollmentMessage = uiText(R.string.ui_new_farm_code_ready_the_previous_code_no_longer_work_b186086, "New farm code ready. The previous code no longer works for new requests."))
    }

    fun decideJoinRequest(requestId: String, approve: Boolean) = perform {
        val farm = requireNotNull(mutable.value.selected)
        require(farm.role == "Owner" && !mutable.value.offline)
        require(mutable.value.enrollment?.requests?.any {
            it.id == requestId && it.farmId == farm.id && it.status == "Pending"
        } == true)
        repo.decideJoinRequest(farm.id, requestId, approve)
        loadEnrollment(farm.id)
        mutable.value = mutable.value.copy(members = repo.members(farm.id))
    }

    fun changeRole(member: FarmMemberDto, role: Int) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.changeRole(farmId, member.id, role)
        mutable.value = mutable.value.copy(members = repo.members(farmId))
    }

    fun remove(member: FarmMemberDto) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.removeMember(farmId, member.id)
        mutable.value = mutable.value.copy(members = repo.members(farmId))
    }

    class Factory(private val repo: FarmRepository,
        private val weather: WeatherRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FarmViewModel(repo, weather) as T
    }
}

