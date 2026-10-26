package com.smartfarm.mobile.features.farms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.WeatherRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class FarmUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val farms: List<FarmDto> = emptyList(),
    val selected: FarmDto? = null,
    val plots: List<PlotDto> = emptyList(),
    val members: List<FarmMemberDto> = emptyList(),
    val invitation: InvitationDto? = null,
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null,
    val weather: WeatherOverviewDto? = null,
    val weatherMessage: String? = null
)

class FarmViewModel(
    private val repo: FarmRepository,
    private val weatherRepo: WeatherRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(FarmUiState())
    val state = mutable.asStateFlow()

    private fun perform(block: suspend () -> Unit) {
        if (mutable.value.loading) return
        mutable.value = mutable.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try { block() }
            catch (_: Exception) {
                mutable.value = mutable.value.copy(error =
                    "Could not complete the request. Check permissions, current data and connection.")
            } finally { mutable.value = mutable.value.copy(loading = false) }
        }
    }


    fun clear() {
        mutable.value = FarmUiState()
    }

    fun load() = perform {
        val snapshot = repo.listSnapshot()
        mutable.value = mutable.value.copy(farms = snapshot.value, selected = null,
            plots = emptyList(), members = emptyList(), invitation = null,
            offline = snapshot.offline,
            cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
    }

    fun loadDetail(farmId: String) {
        mutable.value = if (mutable.value.selected?.id == farmId)
            mutable.value.copy(loading = true, error = null) else FarmUiState(loading = true)
        viewModelScope.launch {
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
                val snapshots = listOfNotNull(farm, plots, members, weatherData)
                val cached = snapshots.any { it.offline }
                mutable.value = mutable.value.copy(
                    selected = farm.value, plots = plots.value, members = members.value,
                    offline = cached,
                    cachedAtUtcMillis = snapshots.filter { it.offline }
                        .minOfOrNull { it.savedAtUtcMillis },
                    weather = weatherData?.value,
                    weatherMessage = if (weatherData == null) "Weather data unavailable." else null)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error =
                    "Could not load this farm. Check permissions and connection.")
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
            selected = updated, weather = null,
            weatherMessage = "Farm settings saved. Reopen this farm to refresh weather.")
        onSuccess()
    }

    fun archiveFarm(onSuccess: () -> Unit) = perform {
        val farm = requireNotNull(mutable.value.selected)
        repo.archiveFarm(farm.id, RowVersionDto(requireNotNull(farm.rowVersion)))
        mutable.value = mutable.value.copy(selected = null, plots = emptyList(), members = emptyList())
        onSuccess()
    }

    fun addPlot(name: String, area: String) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.createPlot(farmId, PlotInputDto(name.trim(), BigDecimal(area)))
        mutable.value = mutable.value.copy(plots = repo.plots(farmId))
    }

    fun savePlot(plot: PlotDto, name: String, area: String) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.updatePlot(farmId, plot.id, PlotInputDto(name.trim(), BigDecimal(area), plot.rowVersion))
        mutable.value = mutable.value.copy(plots = repo.plots(farmId))
    }

    fun archivePlot(plot: PlotDto) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        repo.archivePlot(farmId, plot.id, RowVersionDto(plot.rowVersion))
        mutable.value = mutable.value.copy(plots = repo.plots(farmId))
    }

    fun invite(email: String, role: Int) = perform {
        val farmId = requireNotNull(mutable.value.selected).id
        mutable.value = mutable.value.copy(invitation = repo.invite(farmId, InviteMemberDto(email.trim(), role)))
    }

    fun acceptInvitation(token: String, onSuccess: (String) -> Unit) = perform {
        val accepted = repo.accept(token.trim())
        onSuccess(accepted.farmId)
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

