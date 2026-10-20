package com.smartfarm.mobile.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.smartfarm.mobile.BuildConfig
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.sync.SyncRepository
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

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: SmartFarmDatabase by lazy {
        Room.databaseBuilder(appContext, SmartFarmDatabase::class.java, "smartfarm.db")
            .addMigrations(SmartFarmDatabase.MIGRATION_1_2).build()
    }
    val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }

    private val retrofit by lazy {
        ApiClientFactory.create(BuildConfig.API_BASE_URL, allowLocalHttpForDebug = BuildConfig.DEBUG)
    }
    val farmRepository: FarmRepository by lazy {
        FarmRepository(authRepository) { retrofit.create(FarmApi::class.java) }
    }

    val cropRepository: CropRepository by lazy {
        CropRepository(authRepository) { retrofit.create(CropApi::class.java) }
    }

    val monitoringRepository: MonitoringRepository by lazy {
        MonitoringRepository(authRepository) { retrofit.create(MonitoringApi::class.java) }
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(authRepository) { retrofit.create(InventoryApi::class.java) }
    }

    val harvestRepository: HarvestRepository by lazy {
        HarvestRepository(authRepository) { retrofit.create(HarvestApi::class.java) }
    }

    val statusRepository: StatusRepository by lazy {
        StatusRepository { retrofit.create(StatusApi::class.java) }
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(authRepository, database.pendingOperationDao(),
            { retrofit.create(SyncApi::class.java) }, workManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository({ retrofit.create(AuthApi::class.java) }) {
            syncRepository.schedule()
        }
    }
}






