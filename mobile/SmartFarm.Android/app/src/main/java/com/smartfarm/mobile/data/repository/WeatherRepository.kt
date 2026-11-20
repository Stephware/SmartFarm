package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.WeatherApi
import com.smartfarm.mobile.data.remote.dto.WeatherOverviewDto
import com.smartfarm.mobile.data.remote.dto.WeatherGuidanceDto

class WeatherRepository(
    private val auth: AuthRepository,
    private val cache: OfflineReadCache,
    private val api: () -> WeatherApi
) {
    // Never cache AI/advisory guidance as a current warning or offline confirmation.
    suspend fun guidance(farmId: String, advisoryId: String, cropCycleId: String?): WeatherGuidanceDto =
        auth.authorized { api().guidance(it, farmId, advisoryId, cropCycleId) }

    suspend fun overview(farmId: String): CachedRead<WeatherOverviewDto> =
        cache.read("farm/$farmId/weather", WeatherOverviewDto::class.java) {
            auth.authorized { api().overview(it, farmId) }
        }
}
