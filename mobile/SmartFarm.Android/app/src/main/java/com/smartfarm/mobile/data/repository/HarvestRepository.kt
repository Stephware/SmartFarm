package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.HarvestApi
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.remote.dto.VoidHarvestDto

class HarvestRepository(private val auth: AuthRepository, private val api: () -> HarvestApi) {
    suspend fun list(farmId: String, cycleId: String): HarvestOverviewDto =
        auth.authorized { api().list(it, farmId, cycleId) }

    suspend fun record(farmId: String, cycleId: String, input: RecordHarvestDto) =
        auth.authorized { api().record(it, farmId, cycleId, input) }

    suspend fun void(farmId: String, cycleId: String, harvestId: String, reason: String) =
        auth.authorized { api().void(it, farmId, cycleId, harvestId, VoidHarvestDto(reason)) }
}

