package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.WeatherApi
import com.smartfarm.mobile.data.remote.dto.WeatherOverviewDto

class WeatherRepository(
    private val auth: AuthRepository,
    private val cache: OfflineReadCache,
    private val api: () -> WeatherApi
) {
    suspend fun overview(farmId: String): CachedRead<WeatherOverviewDto> =
        cache.read("farm/$farmId/weather", WeatherOverviewDto::class.java) {
            auth.authorized { api().overview(it, farmId) }
        }
}
