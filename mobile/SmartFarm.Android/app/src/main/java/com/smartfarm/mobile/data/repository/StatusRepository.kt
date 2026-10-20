package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.StatusApi

class StatusRepository(private val api: () -> StatusApi) {
    suspend fun backendAvailable(): Boolean {
        val status = api().check()
        return status.app == "SmartFarm" && status.status == "ok"
    }
}

