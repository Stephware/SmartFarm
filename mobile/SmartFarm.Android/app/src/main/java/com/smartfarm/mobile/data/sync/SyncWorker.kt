package com.smartfarm.mobile.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfarm.mobile.SmartFarmApplication

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SmartFarmApplication
        return if (app.container.syncRepository.uploadPending()) Result.success() else Result.retry()
    }
}
