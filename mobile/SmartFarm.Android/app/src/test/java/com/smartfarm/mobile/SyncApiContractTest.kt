package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.SyncApi
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.remote.dto.SyncWriteRequest
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncApiContractTest {
    @Test fun retriesUseTheSameOperationIdAndPayload() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val id = UUID.randomUUID().toString()
            val body = """{"operationId":"$id","operationType":"harvest","result":{},"replayed":false}"""
            server.enqueue(MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json").setBody(body))
            server.enqueue(MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body.replace("\"replayed\":false", "\"replayed\":true")))

            val api = ApiClientFactory.create(server.url("/").toString(),
                allowLocalHttpForDebug = true).create(SyncApi::class.java)
            val request = SyncWriteRequest(
                farmId = UUID.randomUUID().toString(),
                targetId = UUID.randomUUID().toString(),
                operationType = "harvest",
                harvest = RecordHarvestDto(BigDecimal("2.250"), "Field harvest"))
            val first = api.submit("Bearer sample-token", id, request)
            val replay = api.submit("Bearer sample-token", id, request)

            assertFalse(first.replayed)
            assertTrue(replay.replayed)
            val firstRequest = server.takeRequest()
            val secondRequest = server.takeRequest()
            assertEquals("POST", firstRequest.method)
            assertEquals("/api/sync/operations/$id", firstRequest.path)
            assertEquals(firstRequest.path, secondRequest.path)
            assertEquals("Bearer sample-token", firstRequest.getHeader("Authorization"))
            assertEquals(firstRequest.body.readUtf8(), secondRequest.body.readUtf8())
        } finally {
            server.shutdown()
        }
    }
}
