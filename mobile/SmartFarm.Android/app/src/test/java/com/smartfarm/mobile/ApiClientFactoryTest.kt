package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiClientFactoryTest {
    @Test fun acceptsHttpsEndpointWithoutMakingRequest() {
        val retrofit = ApiClientFactory.create("https://api.example.test/v1/")
        assertEquals("https://api.example.test/v1/", retrofit.baseUrl().toString())
    }

    @Test fun rejectsInvalidAndUnsafeEndpoints() {
        listOf("", "http://api.example.test/", "https://user:password@api.example.test/",
            "https://api.example.test/v1", "https://api.example.test/?token=value",
            "https://api.example.test/#fragment").forEach { endpoint ->
            assertThrows(IllegalArgumentException::class.java) { ApiClientFactory.create(endpoint) }
        }
    }
}
