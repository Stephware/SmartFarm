package com.smartfarm.mobile.data.remote.api

import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClientFactory {
    fun create(baseUrl: String, allowLocalHttpForDebug: Boolean = false): Retrofit {
        val url = requireNotNull(baseUrl.toHttpUrlOrNull()) { "Set a valid API base URL." }
        val allowedDebugHosts = setOf("10.0.2.2", "127.0.0.1", "localhost")
        require((url.scheme == "https" ||
            (allowLocalHttpForDebug && url.scheme == "http" && url.host in allowedDebugHosts)) &&
            url.username.isEmpty() && url.password.isEmpty()) {
            "The API requires HTTPS except for a local development emulator endpoint."
        }
        require(url.query == null && url.fragment == null && url.encodedPath.endsWith('/')) {
            "The API base URL must end in / and contain no query or fragment."
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder().baseUrl(url).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }
}

