package com.smartfarm.mobile.features.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.FarmHarvestSummaryDto
import com.smartfarm.mobile.data.remote.dto.FarmFinanceSummaryDto
import com.smartfarm.mobile.data.repository.FarmInsightsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

data class FarmInsightsUiState(
    val farmId: String = "", val loading: Boolean = false,
    val harvest: FarmHarvestSummaryDto? = null,
    val finance: FarmFinanceSummaryDto? = null,
    val harvestError: String? = null, val financeError: String? = null,
    val from: String? = null, val to: String? = null,
    val loaded: Boolean = false
)

class FarmInsightsViewModel(private val repo: FarmInsightsRepository) : ViewModel() {
    private val mutable = MutableStateFlow(FarmInsightsUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun clear() {
        job?.cancel()
        mutable.value = FarmInsightsUiState()
    }

    fun load(farmId: String, from: String? = null, to: String? = null) {
        job?.cancel()
        // Discard prior farm data before switching identity or period.
        mutable.value = FarmInsightsUiState(farmId = farmId, from = from, to = to, loading = true)
        job = viewModelScope.launch {
            try {
                val harvest = try {
                    repo.harvest(farmId, from, to)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                mutable.value = mutable.value.copy(
                    harvest = harvest,
                    harvestError = if (harvest == null) "Harvest report unavailable. Connect and retry." else null
                )
                val finance = try {
                    repo.finances(farmId, from, to)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                mutable.value = mutable.value.copy(
                    finance = finance, financeError = if (finance == null)
                        "Finance insights unavailable. Check connection and access." else null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                // A cancelled request must never overwrite a newer farm, period, or logout.
                if (job === coroutineContext[Job]) {
                    mutable.value = mutable.value.copy(loading = false, loaded = true)
                }
            }
        }
    }

    class Factory(private val repo: FarmInsightsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FarmInsightsViewModel(repo) as T
    }
}
