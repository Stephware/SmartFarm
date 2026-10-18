package com.smartfarm.mobile.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.smartfarm.mobile.BuildConfig
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.CropRepository
import com.smartfarm.mobile.data.remote.api.CropApi
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import com.smartfarm.mobile.data.repository.MonitoringRepository
import com.smartfarm.mobile.data.remote.api.FarmApi

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: SmartFarmDatabase by lazy {
        Room.databaseBuilder(appContext, SmartFarmDatabase::class.java, "smartfarm.db").build()
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

    val authRepository: AuthRepository by lazy {
        AuthRepository { retrofit.create(AuthApi::class.java) }
    }
}




