package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import com.smartfarm.mobile.data.remote.dto.ActivityInputDto
import com.smartfarm.mobile.data.remote.dto.MonitoringDto
import com.smartfarm.mobile.data.remote.dto.ObservationInputDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class MonitoringRepository(
    private val authentication: AuthRepository,
    private val cache: OfflineReadCache? = null,
    private val api: () -> MonitoringApi
) {
    suspend fun listSnapshot(farmId: String, cycleId: String): CachedRead<MonitoringDto> =
        cache?.read("farm/$farmId/cycle/$cycleId/monitoring", MonitoringDto::class.java) {
            authentication.authorized { api().list(it, farmId, cycleId) }
        } ?: CachedRead(authentication.authorized { api().list(it, farmId, cycleId) },
            false, System.currentTimeMillis())

    suspend fun list(farmId: String, cycleId: String): MonitoringDto =
        listSnapshot(farmId, cycleId).value

    suspend fun activity(farmId: String, cycleId: String, kind: Int, notes: String?) =
        authentication.authorized {
            api().activity(it, farmId, cycleId, ActivityInputDto(kind, notes))
        }

    suspend fun observation(farmId: String, cycleId: String, health: Int,
        stage: String?, notes: String?) =
        authentication.authorized {
            api().observation(it, farmId, cycleId, ObservationInputDto(health, stage, notes))
        }

    suspend fun upload(farmId: String, cycleId: String, observationId: String,
        bytes: ByteArray, mime: String) =
        authentication.authorized {
            val part = MultipartBody.Part.createFormData("photo", "observation",
                bytes.toRequestBody(mime.toMediaType()))
            api().upload(it, farmId, cycleId, observationId, part)
        }

    suspend fun imageBytes(farmId: String, cycleId: String, photoId: String): ByteArray =
        authentication.authorized {
            api().photo(it, farmId, cycleId, photoId).use { body ->
                require(body.contentLength() <= 5L * 1024 * 1024 || body.contentLength() == -1L)
                body.byteStream().use { input ->
                    val buffer = java.io.ByteArrayOutputStream()
                    val block = ByteArray(8192)
                    while (true) {
                        val read = input.read(block)
                        if (read < 0) break
                        buffer.write(block, 0, read)
                        require(buffer.size() <= 5 * 1024 * 1024)
                    }
                    buffer.toByteArray()
                }
            }
        }
}

