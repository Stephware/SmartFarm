package com.smartfarm.mobile.data.repository

import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.CropApi
import com.smartfarm.mobile.data.remote.dto.*

class CropRepository(
    private val auth: AuthRepository,
    private val cache: OfflineReadCache? = null,
    private val api: () -> CropApi
) {
    private val typeList = TypeToken.getParameterized(List::class.java, CropTypeDto::class.java).type
    private val cycleList = TypeToken.getParameterized(List::class.java, CropCycleDto::class.java).type

    suspend fun typesSnapshot(farmId: String): CachedRead<List<CropTypeDto>> =
        cache?.read("farm/$farmId/types", typeList) {
            auth.authorized { api().cropTypes(it, farmId) }
        } ?: CachedRead(auth.authorized { api().cropTypes(it, farmId) }, false, System.currentTimeMillis())
    suspend fun types(farmId: String): List<CropTypeDto> = typesSnapshot(farmId).value

    suspend fun cyclesSnapshot(farmId: String): CachedRead<List<CropCycleDto>> =
        cache?.read("farm/$farmId/cycles", cycleList) {
            auth.authorized { api().cropCycles(it, farmId) }
        } ?: CachedRead(auth.authorized { api().cropCycles(it, farmId) }, false, System.currentTimeMillis())
    suspend fun cycles(farmId: String): List<CropCycleDto> = cyclesSnapshot(farmId).value

    suspend fun createType(farmId: String, input: CropTypeInputDto): CropTypeDto {
        val created = auth.authorized { api().createCropType(it, farmId, input) }
        cache?.invalidate("farm/$farmId/types")
        return created
    }

    suspend fun plan(farmId: String, plotId: String, input: CropCyclePlanDto): CropCycleDto {
        val created = auth.authorized { api().plan(it, farmId, plotId, input) }
        cache?.invalidate("farm/$farmId/cycles")
        return created
    }

    suspend fun updatePlan(farmId: String, cycleId: String, input: CropCyclePlanDto): CropCycleDto {
        val updated = auth.authorized { api().updatePlan(it, farmId, cycleId, input) }
        cache?.invalidate("farm/$farmId/cycles")
        return updated
    }

    suspend fun transition(farmId: String, cycleId: String, input: CropTransitionDto): CropCycleDto {
        val updated = auth.authorized { api().transition(it, farmId, cycleId, input) }
        cache?.invalidate("farm/$farmId/cycles")
        cache?.invalidate("farm/$farmId/cycle/$cycleId/monitoring")
        cache?.invalidate("farm/$farmId/cycle/$cycleId/harvests")
        return updated
    }
}
