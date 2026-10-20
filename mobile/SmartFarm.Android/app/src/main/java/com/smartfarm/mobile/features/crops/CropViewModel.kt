package com.smartfarm.mobile.features.crops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.data.remote.dto.CropCyclePlanDto
import com.smartfarm.mobile.data.remote.dto.CropTransitionDto
import com.smartfarm.mobile.data.remote.dto.CropTypeDto
import com.smartfarm.mobile.data.remote.dto.CropTypeInputDto
import com.smartfarm.mobile.data.remote.dto.PlotDto
import com.smartfarm.mobile.data.repository.CropRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CropUiState(
    val farmId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val types: List<CropTypeDto> = emptyList(),
    val plots: List<PlotDto> = emptyList(),
    val cycles: List<CropCycleDto> = emptyList(),
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class CropViewModel(
    private val crops: CropRepository,
    private val farms: FarmRepository
) : ViewModel() {
    private val mutable = MutableStateFlow(CropUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = CropUiState() }

    fun load(farmId: String) {
        if (mutable.value.busy && mutable.value.farmId == farmId) return
        mutable.value = CropUiState(farmId = farmId, busy = true)
        viewModelScope.launch {
            try {
                val types = crops.typesSnapshot(farmId)
                val plots = farms.plotsSnapshot(farmId)
                val cycles = crops.cyclesSnapshot(farmId)
                val cached = types.offline || plots.offline || cycles.offline
                mutable.value = CropUiState(farmId, types = types.value,
                    plots = plots.value, cycles = cycles.value, offline = cached,
                    cachedAtUtcMillis = listOf(types, plots, cycles)
                        .filter { it.offline }.minOfOrNull { it.savedAtUtcMillis })
            } catch (_: Exception) {
                mutable.value = CropUiState(farmId, error = "Could not load crops. Check farm access and connection.")
            }
        }
    }

    private fun work(block: suspend (String) -> Unit) {
        val farmId = mutable.value.farmId
        if (mutable.value.busy || farmId.isBlank()) return
        mutable.value = mutable.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                block(farmId)
                mutable.value = mutable.value.copy(
                    types = crops.types(farmId), cycles = crops.cycles(farmId))
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    error = "Could not save. Check dates, permissions, crop status and connection; reload if the record changed.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun createType(name: String, unit: String, growth: String, watering: String) = work { farmId ->
        crops.createType(farmId, CropTypeInputDto(name.trim(),
            growth.takeIf { it.isNotBlank() }?.toIntOrNull(),
            watering.takeIf { it.isNotBlank() }?.toIntOrNull(),
            unit.trim()))
    }

    fun plan(plotId: String, typeId: String, date: String, expected: String) = work { farmId ->
        val planting = LocalDate.parse(date)
        val harvest = expected.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        require(harvest == null || !harvest.isBefore(planting))
        crops.plan(farmId, plotId, CropCyclePlanDto(
            typeId, planting.toString(), harvest?.toString()))
    }

    fun updatePlan(cycle: CropCycleDto, date: String, expected: String) = work { farmId ->
        val planting = LocalDate.parse(date)
        val harvest = expected.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
        require(harvest == null || !harvest.isBefore(planting))
        crops.updatePlan(farmId, cycle.id, CropCyclePlanDto(
            cycle.cropTypeId, planting.toString(), harvest?.toString(),
            cycle.harvestUnit, cycle.wateringIntervalDays, cycle.rowVersion))
    }

    fun transition(cycle: CropCycleDto, status: Int, planted: String, reason: String) = work { farmId ->
        val date = if (status == 1) LocalDate.parse(planted).toString() else null
        crops.transition(farmId, cycle.id, CropTransitionDto(
            cycle.rowVersion, status, date, reason.trim().ifBlank { null }))
    }

    class Factory(private val crops: CropRepository, private val farms: FarmRepository) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CropViewModel(crops, farms) as T
    }
}

