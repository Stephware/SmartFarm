package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiClientFactoryTest {
    @Test fun acceptsHttpsEndpointWithoutMakingRequest() {
        val retrofit = ApiClientFactory.create("https://api.example.test/v1/")
        assertEquals("https://api.example.test/v1/", retrofit.baseUrl().toString())
    }

    @Test fun permitsOnlyLocalHttpForDebug() {
        val retrofit = ApiClientFactory.create(
            "http://10.0.2.2:5081/", allowLocalHttpForDebug = true)
        assertEquals("http://10.0.2.2:5081/", retrofit.baseUrl().toString())
        assertThrows(IllegalArgumentException::class.java) {
            ApiClientFactory.create("http://10.0.2.2:5081/")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ApiClientFactory.create("http://external.example.test/", allowLocalHttpForDebug = true)
        }
    }

    @Test fun physicalDebugPhoneUsesAdbReverseAndEmulatorKeepsItsGateway() {
        assertEquals("http://127.0.0.1:5081/",
            ApiClientFactory.forDebugDevice("http://10.0.2.2:5081/", false))
        assertEquals("http://10.0.2.2:5081/",
            ApiClientFactory.forDebugDevice("http://10.0.2.2:5081/", true))
        assertEquals("http://192.168.1.25:5081/",
            ApiClientFactory.forDebugDevice("http://192.168.1.25:5081/", false))
        assertEquals("https://api.example.test/",
            ApiClientFactory.forDebugDevice("https://api.example.test/", false))
    }

    @Test fun explicitlyConfiguredPrivateWifiWorksOnlyInDebug() {
        for (url in listOf("http://192.168.1.25:5081/",
            "http://172.20.10.5:5081/", "http://10.5.0.1:5081/",
            "http://127.0.0.1:5081/")) {
            assertEquals(url, ApiClientFactory.create(url, allowLocalHttpForDebug = true)
                .baseUrl().toString())
            assertThrows(IllegalArgumentException::class.java) {
                ApiClientFactory.create(url, allowLocalHttpForDebug = false)
            }
        }
        for (url in listOf("http://8.8.8.8:5081/", "http://172.32.0.1:5081/",
            "http://192.169.1.25:5081/", "http://example.test:5081/",
            "http://0.0.0.0:5081/", "http://192.168.1.999:5081/")) {
            assertThrows(IllegalArgumentException::class.java) {
                ApiClientFactory.create(url, allowLocalHttpForDebug = true)
            }
        }
    }

    @Test fun sendsUpdatedSelectedLanguageOnEveryRequest() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{}"))
            server.enqueue(MockResponse().setBody("{}"))
            var language = "en"
            val retrofit = ApiClientFactory.create(server.url("/").toString(),
                allowLocalHttpForDebug = true, languageProvider = { language })
            val factory = retrofit.callFactory()
            val request = Request.Builder().url(server.url("/ping")).build()
            factory.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
            }
            assertEquals("en", server.takeRequest().getHeader("Accept-Language"))
            language = "fil"
            factory.newCall(request).execute().use { response ->
                assertEquals(200, response.code)
            }
            assertEquals("fil", server.takeRequest().getHeader("Accept-Language"))
        }
    }

    @Test fun rejectsInvalidAndUnsafeEndpoints() {
        listOf("", "http://api.example.test/", "https://user:password@api.example.test/",
            "https://api.example.test/v1", "https://api.example.test/?token=value",
            "https://api.example.test/#fragment").forEach { endpoint ->
            assertThrows(IllegalArgumentException::class.java) { ApiClientFactory.create(endpoint) }
        }
    }
}

