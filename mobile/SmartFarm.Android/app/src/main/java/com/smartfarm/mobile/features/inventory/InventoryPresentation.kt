package com.smartfarm.mobile.features.inventory

import com.smartfarm.mobile.R
import com.smartfarm.mobile.localization.uiText

import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import java.util.Locale

internal fun inventoryCategoryName(category: Int): String = when (category) {
    1 -> uiText(R.string.ui_seeds_45a1954, "Seeds")
    2 -> uiText(R.string.ui_fertilizers_1841450, "Fertilizers")
    3 -> uiText(R.string.ui_pesticides_3172816, "Pesticides")
    else -> uiText(R.string.ui_other_f97e9da, "Other")
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

private val inventoryMaximum = java.math.BigDecimal("999999999999999.999")

/** Convert a desired physical balance to the existing signed adjustment payload. */
internal fun inventoryCorrectionDelta(
    confirmedBalance: java.math.BigDecimal, targetText: String
): java.math.BigDecimal? {
    val target = targetText.trim().toBigDecimalOrNull() ?: return null
    if (target.signum() < 0 || target > inventoryMaximum ||
        target.stripTrailingZeros().scale() > 3) return null
    val difference = target.subtract(confirmedBalance)
    return difference.takeIf { it.signum() != 0 &&
        it.abs() <= inventoryMaximum &&
        it.stripTrailingZeros().scale() <= 3 }
}

/** Preview uses confirmed server stock only. Pending movements are explicitly excluded. */
internal fun inventoryBalancePreview(
    confirmedBalance: java.math.BigDecimal, type: Int, userInput: String
): java.math.BigDecimal? {
    val delta = if (type == 4) inventoryCorrectionDelta(confirmedBalance, userInput)
        else runCatching { inventoryMovementQuantity(type, userInput.trim()) }.getOrNull()
    val next = confirmedBalance.add(delta ?: return null)
    return next.takeIf { it.signum() >= 0 && it <= inventoryMaximum }
}
