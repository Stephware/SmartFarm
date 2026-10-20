package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.HarvestApi
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.HarvestEntryDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.remote.dto.VoidHarvestDto

class HarvestRepository(private val auth: AuthRepository,
    private val cache: OfflineReadCache? = null, private val api: () -> HarvestApi) {
    suspend fun listSnapshot(farmId: String, cycleId: String): CachedRead<HarvestOverviewDto> =
        cache?.read("farm/$farmId/cycle/$cycleId/harvests", HarvestOverviewDto::class.java) {
            auth.authorized { api().list(it, farmId, cycleId) }
        } ?: CachedRead(auth.authorized { api().list(it, farmId, cycleId) },
            false, System.currentTimeMillis())

    suspend fun list(farmId: String, cycleId: String): HarvestOverviewDto =
        listSnapshot(farmId, cycleId).value

    suspend fun record(farmId: String, cycleId: String, input: RecordHarvestDto): HarvestEntryDto {
        val result = auth.authorized { api().record(it, farmId, cycleId, input) }
        cache?.invalidate("farm/$farmId/cycle/$cycleId/harvests")
        return result
    }

    suspend fun void(farmId: String, cycleId: String, harvestId: String, reason: String): HarvestEntryDto {
        val result = auth.authorized { api().void(it, farmId, cycleId, harvestId, VoidHarvestDto(reason)) }
        cache?.invalidate("farm/$farmId/cycle/$cycleId/harvests")
        return result
    }
}
