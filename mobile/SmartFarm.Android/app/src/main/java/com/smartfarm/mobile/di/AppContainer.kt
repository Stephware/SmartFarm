package com.smartfarm.mobile.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.smartfarm.mobile.data.local.SmartFarmDatabase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: SmartFarmDatabase by lazy {
        Room.databaseBuilder(appContext, SmartFarmDatabase::class.java, "smartfarm.db").build()
    }
    val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }
}
