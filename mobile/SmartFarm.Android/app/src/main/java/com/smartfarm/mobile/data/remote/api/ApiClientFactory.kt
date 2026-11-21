package com.smartfarm.mobile.data.remote.api

import java.util.concurrent.TimeUnit
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClientFactory {
    /** Localized API messages follow the current app language, not the device's default. */
    internal fun acceptLanguage(language: String): String = if (language == "fil") "fil" else "en"

    /**
     * Android emulator's 10.0.2.2 is not a physical phone's host. With adb reverse
     * tcp:5081 tcp:5081, a USB-connected device instead uses its own 127.0.0.1.
     * Explicit LAN/HTTPS build URLs are never rewritten.
     */
    internal fun forDebugDevice(configuredUrl: String, isEmulator: Boolean): String =
        if (!isEmulator && configuredUrl == "http://10.0.2.2:5081/")
            "http://127.0.0.1:5081/"
        else configuredUrl

    /** HTTP is allowed only in a debug build and only to literal private/loopback IPs. */
    internal fun isLocalDebugHost(host: String): Boolean {
        if (host == "localhost") return true
        val parts = host.split(".")
        if (parts.size != 4 || parts.any { it.isEmpty() || it.length > 3 ||
                it.any { ch -> !ch.isDigit() } }) return false
        val ip = parts.map { it.toIntOrNull() ?: return false }
        if (ip.any { it !in 0..255 }) return false
        val a = ip[0]; val b = ip[1]
        return (a == 127 && b == 0 && ip[2] == 0 && ip[3] == 1) ||
            a == 10 || (a == 172 && b in 16..31) || (a == 192 && b == 168)
    }

    fun create(
        baseUrl: String,
        allowLocalHttpForDebug: Boolean = false,
        languageProvider: () -> String = { "en" }
    ): Retrofit {
        val url = requireNotNull(baseUrl.toHttpUrlOrNull()) { "Set a valid API base URL." }

        require((url.scheme == "https" ||
            (allowLocalHttpForDebug && url.scheme == "http" && isLocalDebugHost(url.host))) &&
            url.username.isEmpty() && url.password.isEmpty()) {
            "The API requires HTTPS except for an explicitly configured private development endpoint."
        }
        require(url.query == null && url.fragment == null && url.encodedPath.endsWith('/')) {
            "The API base URL must end in / and contain no query or fragment."
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val language = acceptLanguage(languageProvider())
                chain.proceed(chain.request().newBuilder()
                    .header("Accept-Language", language).build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder().baseUrl(url).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }
}

