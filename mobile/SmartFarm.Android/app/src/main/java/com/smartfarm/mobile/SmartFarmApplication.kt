package com.smartfarm.mobile

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.smartfarm.mobile.di.AppContainer

class SmartFarmApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        com.smartfarm.mobile.localization.AppLanguage.initialize(this)
    }
    val container by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build()
}
