package com.smartfarm.mobile.data.remote.api

import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClientFactory {
    fun create(baseUrl: String): Retrofit {
        val url = requireNotNull(baseUrl.toHttpUrlOrNull()) { "An HTTPS API base URL is required." }
        require(url.scheme == "https" && url.username.isEmpty() && url.password.isEmpty()) {
            "The API URL must use HTTPS without embedded credentials."
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
