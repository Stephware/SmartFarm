package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import java.util.UUID
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoUploadContractTest {
    @Test fun photoRetriesKeepStableIdsAndBody() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val farm = UUID.randomUUID().toString()
            val crop = UUID.randomUUID().toString()
            val observation = UUID.randomUUID().toString()
            val photo = UUID.randomUUID().toString()
            val json = """{"id":"$photo","observationId":"$observation","contentType":"image/jpeg","fileSizeBytes":5,"uploadedAtUtc":"2026-10-20T00:00:00Z"}"""
            repeat(2) {
                server.enqueue(MockResponse().setResponseCode(200)
                    .addHeader("Content-Type", "application/json").setBody(json))
            }
            val api = ApiClientFactory.create(server.url("/").toString(),
                allowLocalHttpForDebug = true).create(MonitoringApi::class.java)
            val bytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x00, 0x01)
            suspend fun upload() = api.uploadWithId("Bearer sample-token", farm, crop,
                observation, photo, MultipartBody.Part.createFormData("photo", "field.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaType())))
            assertEquals(photo, upload().id)
            assertEquals(photo, upload().id)

            val first = server.takeRequest()
            val retry = server.takeRequest()
            val route = "/api/farms/$farm/crop-cycles/$crop/monitoring/observations/$observation/photos/$photo"
            assertEquals("PUT", first.method)
            assertEquals(route, first.path)
            assertEquals(first.path, retry.path)
            assertEquals(first.body.readByteString(), retry.body.readByteString())
            assertEquals("Bearer sample-token", first.getHeader("Authorization"))
            assertTrue(first.getHeader("Content-Type")!!.contains("multipart/form-data"))
        } finally { server.shutdown() }
    }
}
