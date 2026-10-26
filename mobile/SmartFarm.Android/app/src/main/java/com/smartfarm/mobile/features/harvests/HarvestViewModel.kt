package com.smartfarm.mobile.features.harvests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.repository.HarvestRepository
import com.smartfarm.mobile.data.sync.SyncRepository
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
    val saveReceipts: Map<String, String> = emptyMap(),
    val overview: HarvestOverviewDto? = null,
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class HarvestViewModel(private val repository: HarvestRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(HarvestUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = HarvestUiState() }

    fun load(farmId: String, cycleId: String) {
        mutable.value = HarvestUiState(farmId = farmId, cycleId = cycleId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val snapshot = repository.listSnapshot(farmId, cycleId)
                mutable.value = mutable.value.copy(overview = snapshot.value,
                    offline = snapshot.offline,
                    cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
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
            } catch (_: Exception) {
                // A lost response does not prove the server rejected the write.
                // Never encourage blindly retrying a non-idempotent harvest POST.
                mutable.value = mutable.value.copy(
                    error = "Could not confirm the save. Refresh harvest history before trying again.",
                    busy = false)
                return@launch
            }
            try {
                mutable.value = mutable.value.copy(
                    overview = repository.list(snapshot.farmId, snapshot.cycleId),
                    notice = message)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    notice = "The server confirmed the change, but history could not refresh. Reopen to check it.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun record(quantity: String, notes: String) {
        val value = quantity.toBigDecimalOrNull()
        if (value == null || value <= BigDecimal.ZERO ||
            value.stripTrailingZeros().scale() > 3 ||
            value > BigDecimal("999999999999999.999") || notes.length > 1000) {
            mutable.value = mutable.value.copy(error = "Enter a positive quantity with up to 3 decimals.")
            return
        }
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.overview?.canRecord != true) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = sync.queueHarvest(snapshot.farmId, snapshot.cycleId,
                    RecordHarvestDto(value, notes.trim().ifBlank { null }))
                mutable.value = mutable.value.copy(saveReceipts = mutable.value.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.cycleId}" to id))
                mutable.value = mutable.value.copy(notice =
                    "Harvest saved on this device. Sync status shows when the server confirms it.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not save harvest on this device.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
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

    class Factory(private val repository: HarvestRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HarvestViewModel(repository, sync) as T
    }
}

