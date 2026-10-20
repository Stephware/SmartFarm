package com.smartfarm.mobile.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.smartfarm.mobile.BuildConfig
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.sync.SyncRepository
import com.smartfarm.mobile.data.sync.LocalPhotoFiles
import com.smartfarm.mobile.data.remote.api.SyncApi
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.StatusApi
import com.smartfarm.mobile.data.repository.StatusRepository
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.CropRepository
import com.smartfarm.mobile.data.remote.api.CropApi
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.remote.api.HarvestApi
import com.smartfarm.mobile.data.repository.HarvestRepository
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import com.smartfarm.mobile.data.repository.MonitoringRepository
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.api.WeatherApi
import com.smartfarm.mobile.data.repository.WeatherRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: SmartFarmDatabase by lazy {
        Room.databaseBuilder(appContext, SmartFarmDatabase::class.java, "smartfarm.db")
            .addMigrations(SmartFarmDatabase.MIGRATION_1_2, SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4).build()
    }
    val offlineCache: OfflineReadCache by lazy {
        OfflineReadCache(authRepository, database.cachedResponseDao())
    }

    val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }

    private val retrofit by lazy {
        ApiClientFactory.create(BuildConfig.API_BASE_URL, allowLocalHttpForDebug = BuildConfig.DEBUG)
    }
    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(authRepository, offlineCache) { retrofit.create(WeatherApi::class.java) }
    }

    val farmRepository: FarmRepository by lazy {
        FarmRepository(authRepository, offlineCache) { retrofit.create(FarmApi::class.java) }
    }

    val cropRepository: CropRepository by lazy {
        CropRepository(authRepository, offlineCache) { retrofit.create(CropApi::class.java) }
    }

    val monitoringRepository: MonitoringRepository by lazy {
        MonitoringRepository(authRepository, offlineCache) { retrofit.create(MonitoringApi::class.java) }
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(authRepository, offlineCache) { retrofit.create(InventoryApi::class.java) }
    }

    val harvestRepository: HarvestRepository by lazy {
        HarvestRepository(authRepository, offlineCache) { retrofit.create(HarvestApi::class.java) }
    }

    val statusRepository: StatusRepository by lazy {
        StatusRepository { retrofit.create(StatusApi::class.java) }
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(authRepository, database.pendingOperationDao(),
            database.pendingPhotoDao(), LocalPhotoFiles(appContext),
            { retrofit.create(SyncApi::class.java) },
            { retrofit.create(MonitoringApi::class.java) },
            { retrofit.create(InventoryApi::class.java) }, workManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(onLogin = {
            syncRepository.enablePeriodicChecks()
            syncRepository.schedule()
        }) { retrofit.create(AuthApi::class.java) }
    }
}






