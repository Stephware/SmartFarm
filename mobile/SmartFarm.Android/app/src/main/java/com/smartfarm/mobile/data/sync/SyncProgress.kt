package com.smartfarm.mobile.data.sync

/** Session-only acknowledgments; confirmed records live in server history, not the queue. */
data class SyncProgress(
    val userId: String? = null,
    val sendingId: String? = null,
    val sendingFarmId: String? = null,
    val signInRequired: Boolean = false,
    val confirmedFarms: Map<String, Long> = emptyMap()
)
