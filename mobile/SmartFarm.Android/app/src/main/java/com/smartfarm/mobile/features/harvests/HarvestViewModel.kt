package com.smartfarm.mobile.features.harvests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.repository.HarvestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class HarvestUiState(
    val farmId: String = "",
    val cycleId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val overview: HarvestOverviewDto? = null
)

class HarvestViewModel(private val repository: HarvestRepository) : ViewModel() {
    private val mutable = MutableStateFlow(HarvestUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = HarvestUiState() }

    fun load(farmId: String, cycleId: String) {
        mutable.value = HarvestUiState(farmId = farmId, cycleId = cycleId, busy = true)
        viewModelScope.launch {
            try {
                mutable.value = mutable.value.copy(overview = repository.list(farmId, cycleId))
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load harvest history.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    private fun perform(message: String, operation: suspend (String, String) -> Unit) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.overview == null) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                operation(snapshot.farmId, snapshot.cycleId)
                val refreshed = repository.list(snapshot.farmId, snapshot.cycleId)
                mutable.value = mutable.value.copy(overview = refreshed, notice = message)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    error = "Request not completed. Check permissions, quantity and connection; refresh the history.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun record(quantity: String, notes: String) {
        val value = quantity.toBigDecimalOrNull()
        if (value == null || value <= BigDecimal.ZERO || value.scale() > 3 ||
            value > BigDecimal("999999999999999.999")) {
            mutable.value = mutable.value.copy(error = "Enter a positive quantity with up to 3 decimals.")
            return
        }
        perform("Partial harvest recorded.") { farm, cycle ->
            repository.record(farm, cycle, RecordHarvestDto(value, notes.trim().ifBlank { null }))
        }
    }

    fun void(harvestId: String, reason: String) {
        if (reason.trim().length !in 3..300) {
            mutable.value = mutable.value.copy(error = "A reason of 3–300 characters is required.")
            return
        }
        perform("Harvest voided; audit history preserved.") { farm, cycle ->
            repository.void(farm, cycle, harvestId, reason.trim())
        }
    }

    class Factory(private val repository: HarvestRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HarvestViewModel(repository) as T
    }
}

