package com.smartfarm.mobile.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.smartfarm.mobile.BuildConfig
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.repository.AuthRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: SmartFarmDatabase by lazy {
        Room.databaseBuilder(appContext, SmartFarmDatabase::class.java, "smartfarm.db").build()
    }
    val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }

    val authRepository: AuthRepository by lazy {
        AuthRepository {
            ApiClientFactory.create(
                BuildConfig.API_BASE_URL, allowLocalHttpForDebug = BuildConfig.DEBUG
            ).create(AuthApi::class.java)
        }
    }
}

