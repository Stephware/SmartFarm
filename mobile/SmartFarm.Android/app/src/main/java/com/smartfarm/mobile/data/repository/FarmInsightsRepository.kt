package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.FarmInsightsApi
import com.smartfarm.mobile.data.remote.dto.FarmHarvestSummaryDto
import com.smartfarm.mobile.data.remote.dto.FarmFinanceSummaryDto

/** Reports are server-confirmed, never inferred from pending offline writes. */
class FarmInsightsRepository(
    private val auth: AuthRepository, private val api: () -> FarmInsightsApi
) {
    suspend fun harvest(farmId: String, from: String?, to: String?): FarmHarvestSummaryDto =
        auth.authorized { api().harvestReport(it, farmId, from, to) }

    suspend fun finances(farmId: String, from: String?, to: String?): FarmFinanceSummaryDto =
        auth.authorized { api().financeInsights(it, farmId, from, to) }
}
