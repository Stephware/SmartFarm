package com.smartfarm.mobile.features.inventory

import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import java.util.Locale

internal fun inventoryCategoryName(category: Int): String = when (category) {
    1 -> "Seeds"
    2 -> "Fertilizers"
    3 -> "Pesticides"
    else -> "Other"
}

/** Filter the existing farm-scoped snapshot without modifying cached balances. */
internal fun inventoryVisibleItems(
    items: List<InventoryItemDto>, filter: String, search: String
): List<InventoryItemDto> {
    val term = search.trim()
    return items.filter { item ->
        (when (filter) {
            "low" -> item.isLowStock
            "seeds" -> item.category == 1
            "fertilizers" -> item.category == 2
            "pesticides" -> item.category == 3
            "other" -> item.category == 4
            else -> true
        }) && (term.isEmpty() || item.name.contains(term, ignoreCase = true))
    }.sortedWith(compareByDescending<InventoryItemDto> { it.isLowStock }
        .thenBy { it.name.lowercase(Locale.ROOT) })
}
