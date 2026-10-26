package com.smartfarm.mobile.features.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class InventoryUiState(
    val farmId: String = "",
    val itemId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val saveReceipts: Map<String, String> = emptyMap(),
    val workspace: InventoryWorkspaceDto = InventoryWorkspaceDto(emptyList(), 0),
    val item: InventoryItemDto? = null,
    val transactions: List<InventoryTransactionDto> = emptyList(),
    val activities: List<InventoryActivityOptionDto> = emptyList(),
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class InventoryViewModel(private val repo: InventoryRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(InventoryUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = InventoryUiState() }

    fun load(farmId: String) {
        mutable.value = InventoryUiState(farmId = farmId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val snapshot = repo.listSnapshot(farmId)
                mutable.value = mutable.value.copy(workspace = snapshot.value,
                    offline = snapshot.offline,
                    cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load inventory.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun loadItem(farmId: String, itemId: String) {
        mutable.value = InventoryUiState(farmId = farmId, itemId = itemId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val item = repo.itemSnapshot(farmId, itemId)
                val history = repo.historySnapshot(farmId, itemId)
                val activityResult = if (item.value.archivedAtUtc == null)
                    repo.activitiesSnapshot(farmId) else null
                val snapshots = listOfNotNull(item, history, activityResult)
                val cached = snapshots.any { it.offline }
                mutable.value = mutable.value.copy(
                    item = item.value, transactions = history.value,
                    activities = activityResult?.value ?: emptyList(),
                    offline = cached,
                    cachedAtUtcMillis = snapshots.filter { it.offline }
                        .minOfOrNull { it.savedAtUtcMillis })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load inventory item.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    private fun work(after: (() -> Unit)? = null, block: suspend (InventoryUiState) -> Unit) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                block(snapshot)
                mutable.value = mutable.value.copy(workspace = repo.list(snapshot.farmId))
                if (snapshot.itemId.isNotBlank()) {
                    mutable.value = mutable.value.copy(
                        item = repo.item(snapshot.farmId, snapshot.itemId),
                        transactions = repo.history(snapshot.farmId, snapshot.itemId))
                }
                after?.invoke()
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    error = "Stock change failed. Check values and permissions; reload if inventory has changed.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun create(name: String, category: Int, unit: String, starting: String, reorder: String) = work {
        repo.create(it.farmId, InventoryItemInputDto(name.trim(), category, unit.trim(),
            reorderLevel = reorder.takeIf { v -> v.isNotBlank() }?.let(::BigDecimal),
            initialQuantity = BigDecimal(starting)))
    }

    fun update(name: String, category: Int, unit: String, description: String, reorder: String) = work {
        val current = requireNotNull(it.item)
        repo.update(it.farmId, it.itemId, InventoryItemInputDto(name.trim(), category, unit.trim(),
            description.trim().ifBlank { null },
            reorder.takeIf { v -> v.isNotBlank() }?.let(::BigDecimal),
            rowVersion = current.rowVersion))
    }

    fun archive(onSuccess: () -> Unit) = work(onSuccess) {
        val current = requireNotNull(it.item)
        repo.archive(it.farmId, it.itemId, current.rowVersion)
    }

    fun movement(type: Int, quantity: String, activity: String?, reason: String) {
        val snapshot = mutable.value
        val current = snapshot.item ?: return
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = sync.queueInventory(snapshot.farmId, snapshot.itemId, InventoryMovementDto(
                    type, BigDecimal(quantity), activity, reason.trim().ifBlank { null },
                    current.rowVersion))
                mutable.value = mutable.value.copy(saveReceipts = mutable.value.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.itemId}" to id))
                mutable.value = mutable.value.copy(notice =
                    "Stock change queued. Do not record it again; check offline sync status.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not save the change on this device.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun reverse(transactionId: String, reason: String) = work {
        val current = requireNotNull(it.item)
        repo.reverse(it.farmId, it.itemId, transactionId,
            InventoryReversalDto(current.rowVersion, reason.trim()))
    }

    class Factory(private val repo: InventoryRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InventoryViewModel(repo, sync) as T
    }
}

