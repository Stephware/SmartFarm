package com.smartfarm.mobile

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.smartfarm.mobile.di.AppContainer

class SmartFarmApplication : Application(), Configuration.Provider {
    val container by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(Log.INFO).build()
}
