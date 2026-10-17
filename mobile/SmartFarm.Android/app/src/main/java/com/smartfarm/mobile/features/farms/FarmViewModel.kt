package com.smartfarm.mobile.features.farms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.FarmRepository
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
    val invitation: InvitationDto? = null
)

class FarmViewModel(private val repo: FarmRepository) : ViewModel() {
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
        val list = repo.list()
        mutable.value = mutable.value.copy(farms = list, selected = null,
            plots = emptyList(), members = emptyList(), invitation = null)
    }

    fun loadDetail(farmId: String) {
        mutable.value = FarmUiState(loading = true)
        viewModelScope.launch {
            try {
                val farm = repo.farm(farmId)
                val plots = repo.plots(farmId)
                val members = repo.members(farmId)
                mutable.value = mutable.value.copy(selected = farm, plots = plots, members = members)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error =
                    "Could not load this farm. Check permissions and connection.")
            } finally {
                mutable.value = mutable.value.copy(loading = false)
            }
        }
    }

    fun create(name: String, location: String, onSuccess: (String) -> Unit) = perform {
        val farm = repo.create(FarmInputDto(name.trim(), location.trim()))
        onSuccess(farm.id)
    }

    fun saveFarm(name: String, location: String, onSuccess: () -> Unit) = perform {
        val current = requireNotNull(mutable.value.selected)
        val updated = repo.updateFarm(current.id, FarmInputDto(
            name.trim(), location.trim(), current.timeZoneId, current.latitude,
            current.longitude, requireNotNull(current.rowVersion)))
        mutable.value = mutable.value.copy(selected = updated)
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

    class Factory(private val repo: FarmRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FarmViewModel(repo) as T
    }
}

