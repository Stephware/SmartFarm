package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.CropApi
import com.smartfarm.mobile.data.remote.dto.CropCycleDto
import com.smartfarm.mobile.data.remote.dto.CropCyclePlanDto
import com.smartfarm.mobile.data.remote.dto.CropTransitionDto
import com.smartfarm.mobile.data.remote.dto.CropTypeDto
import com.smartfarm.mobile.data.remote.dto.CropTypeInputDto

class CropRepository(
    private val auth: AuthRepository,
    private val api: () -> CropApi
) {
    suspend fun types(farmId: String): List<CropTypeDto> =
        auth.authorized { api().cropTypes(it, farmId) }

    suspend fun createType(farmId: String, input: CropTypeInputDto): CropTypeDto =
        auth.authorized { api().createCropType(it, farmId, input) }

    suspend fun cycles(farmId: String): List<CropCycleDto> =
        auth.authorized { api().cropCycles(it, farmId) }

    suspend fun plan(farmId: String, plotId: String, input: CropCyclePlanDto): CropCycleDto =
        auth.authorized { api().plan(it, farmId, plotId, input) }

    suspend fun updatePlan(farmId: String, cycleId: String, input: CropCyclePlanDto): CropCycleDto =
        auth.authorized { api().updatePlan(it, farmId, cycleId, input) }

    suspend fun transition(farmId: String, cycleId: String, input: CropTransitionDto): CropCycleDto =
        auth.authorized { api().transition(it, farmId, cycleId, input) }
}

