package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.WeatherApi
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherApiContractTest {
    @Test fun readsSavedForecastAndAlertsForOneFarm() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val farmId = UUID.randomUUID().toString()
            val advisoryId = UUID.randomUUID().toString()
            server.enqueue(MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                  {
                    "farmId":"$farmId", "hasCoordinates":true, "timeZoneId":"Asia/Manila",
                    "days":[{
                        "date":"2026-10-21","minTemperatureC":23.5,
                        "maxTemperatureC":35.0,"rainfallMm":21.2,
                        "retrievedAtUtc":"2026-10-20T08:00:00Z","provider":"Open-Meteo"
                    }],
                    "advisories":[{
                        "id":"$advisoryId","date":"2026-10-21",
                        "type":1,"severity":2,"status":0,
                        "message":"Heavy rain is forecast.",
                        "createdAtUtc":"2026-10-20T08:00:00Z"
                    }]
                  }
                """.trimIndent()))
            val api = ApiClientFactory.create(
                server.url("/").toString(), allowLocalHttpForDebug = true)
                .create(WeatherApi::class.java)
            val result = api.overview("Bearer token", farmId)
            assertTrue(result.hasCoordinates)
            assertEquals("Asia/Manila", result.timeZoneId)
            assertEquals("21.2", result.days.single().rainfallMm!!.toPlainString())
            assertEquals(2, result.advisories.single().severity)
            assertEquals(0, result.advisories.single().status)
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/farms/$farmId/weather", request.path)
            assertEquals("Bearer token", request.getHeader("Authorization"))
        } finally {
            server.shutdown()
        }
    }
}
