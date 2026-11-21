# SmartFarm android source snapshot

> Source: Local SmartFarm repository at 850bfc7dab94675d5a70c13a95a9d00c481c1784
> Collected: 2026-10-27
> Published: Unknown

Read-only capture of repository file contents; not proof of runtime behavior.


## Repository file: mobile/SmartFarm.Android/app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

// The emulator uses SmartFarm.Api; other hosts can be supplied at build time.
val debugApiUrl = providers.gradleProperty("smartfarmDebugApiUrl").orNull
    ?: "http://10.0.2.2:5081/"
val releaseApiUrl = providers.gradleProperty("smartfarmReleaseApiUrl").orNull ?: ""

fun apiUrlLiteral(url: String): String =
    "\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.smartfarm.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.smartfarm.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL", apiUrlLiteral(debugApiUrl))
        }
        getByName("release") {
            // Set -PsmartfarmReleaseApiUrl=https://your-api/ when building a release.
            require(releaseApiUrl.isBlank() || releaseApiUrl.startsWith("https://")) {
                "Release API URL must use HTTPS."
            }
            buildConfigField("String", "API_BASE_URL", apiUrlLiteral(releaseApiUrl))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.activity:activity-ktx:1.12.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    // Glide loads monitoring photos; ExifInterface preserves rotation during optimization.
    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}



## Repository file: mobile/SmartFarm.Android/build.gradle.kts

plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("androidx.room") version "2.8.5" apply false
}


## Repository file: mobile/SmartFarm.Android/gradle/wrapper/gradle-wrapper.properties

distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionSha256Sum=b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/di/AppContainer.kt

package com.smartfarm.mobile.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.smartfarm.mobile.BuildConfig
import com.smartfarm.mobile.data.local.SmartFarmDatabase
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.sync.SyncRepository
import com.smartfarm.mobile.data.sync.LocalPhotoFiles
import com.smartfarm.mobile.data.remote.api.SyncApi
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.StatusApi
import com.smartfarm.mobile.data.repository.StatusRepository
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import com.smartfarm.mobile.data.repository.CropRepository
import com.smartfarm.mobile.data.remote.api.CropApi
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.remote.api.HarvestApi
import com.smartfarm.mobile.data.repository.HarvestRepository
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import com.smartfarm.mobile.data.repository.MonitoringRepository
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.api.WeatherApi
import com.smartfarm.mobile.data.repository.WeatherRepository

class AppContainer(context: Context) {
    val taskRepository by lazy {
        com.smartfarm.mobile.data.repository.TaskRepository(authRepository) {
            retrofit.create(com.smartfarm.mobile.data.remote.api.TaskApi::class.java)
        }
    }
    private val appContext = context.applicationContext
    val database: SmartFarmDatabase by lazy {
        Room.databaseBuilder(appContext, SmartFarmDatabase::class.java, "smartfarm.db")
            .addMigrations(SmartFarmDatabase.MIGRATION_1_2, SmartFarmDatabase.MIGRATION_2_3,
                SmartFarmDatabase.MIGRATION_3_4).build()
    }
    val offlineCache: OfflineReadCache by lazy {
        OfflineReadCache(authRepository, database.cachedResponseDao())
    }

    val workManager: WorkManager by lazy { WorkManager.getInstance(appContext) }

    private val retrofit by lazy {
        ApiClientFactory.create(BuildConfig.API_BASE_URL, allowLocalHttpForDebug = BuildConfig.DEBUG)
    }
    val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(authRepository, offlineCache) { retrofit.create(WeatherApi::class.java) }
    }

    val farmRepository: FarmRepository by lazy {
        FarmRepository(authRepository, offlineCache) { retrofit.create(FarmApi::class.java) }
    }

    val cropRepository: CropRepository by lazy {
        CropRepository(authRepository, offlineCache) { retrofit.create(CropApi::class.java) }
    }

    val monitoringRepository: MonitoringRepository by lazy {
        MonitoringRepository(authRepository, offlineCache) { retrofit.create(MonitoringApi::class.java) }
    }

    val inventoryRepository: InventoryRepository by lazy {
        InventoryRepository(authRepository, offlineCache) { retrofit.create(InventoryApi::class.java) }
    }

    val harvestRepository: HarvestRepository by lazy {
        HarvestRepository(authRepository, offlineCache) { retrofit.create(HarvestApi::class.java) }
    }

    val statusRepository: StatusRepository by lazy {
        StatusRepository { retrofit.create(StatusApi::class.java) }
    }

    val syncRepository: SyncRepository by lazy {
        SyncRepository(authRepository, database.pendingOperationDao(),
            database.pendingPhotoDao(), LocalPhotoFiles(appContext),
            { retrofit.create(SyncApi::class.java) },
            { retrofit.create(MonitoringApi::class.java) },
            { retrofit.create(InventoryApi::class.java) }, workManager)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(onLogin = {
            syncRepository.enablePeriodicChecks()
            syncRepository.schedule()
        }) { retrofit.create(AuthApi::class.java) }
    }
}








## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/SmartFarmDatabase.kt

package com.smartfarm.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.smartfarm.mobile.data.local.dao.CachedResponseDao
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.local.dao.LocalMetadataDao
import com.smartfarm.mobile.data.local.dao.PendingPhotoDao
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.local.dao.PendingOperationDao
import com.smartfarm.mobile.data.local.entity.LocalMetadata
import com.smartfarm.mobile.data.local.entity.PendingOperation

@Database(entities = [LocalMetadata::class, PendingOperation::class, CachedResponse::class, PendingPhoto::class], version = 4, exportSchema = true)
abstract class SmartFarmDatabase : RoomDatabase() {
    abstract fun metadataDao(): LocalMetadataDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun pendingPhotoDao(): PendingPhotoDao
    abstract fun cachedResponseDao(): CachedResponseDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_photos` (
                        `photoId` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `farmId` TEXT NOT NULL,
                        `cycleId` TEXT NOT NULL,
                        `observationId` TEXT,
                        `parentOperationId` TEXT,
                        `mimeType` TEXT NOT NULL,
                        `savedAtUtcMillis` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT
                    )
                """.trimIndent())
            }
        }


        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cached_responses` (
                        `userId` TEXT NOT NULL,
                        `resourceKey` TEXT NOT NULL,
                        `json` TEXT NOT NULL,
                        `savedAtUtcMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `resourceKey`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SQLiteConnection) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_operations` (
                        `operationId` TEXT NOT NULL PRIMARY KEY,
                        `userId` TEXT NOT NULL,
                        `farmId` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `createdAtUtc` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT
                    )
                """.trimIndent())
            }
        }
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/OfflineReadCache.kt

package com.smartfarm.mobile.data.local

import com.google.gson.Gson
import com.smartfarm.mobile.data.local.dao.CachedResponseDao
import com.smartfarm.mobile.data.local.entity.CachedResponse
import com.smartfarm.mobile.data.repository.AuthRepository
import java.io.IOException
import java.lang.reflect.Type

data class CachedRead<T>(
    val value: T,
    val offline: Boolean,
    val savedAtUtcMillis: Long
)

// Always prefer the API. Only connection failures can fall back to a prior snapshot.
// HTTP 401/403/404 and server validation errors are never replaced with cached data.
class OfflineReadCache(
    private val authentication: AuthRepository,
    private val dao: CachedResponseDao,
    private val gson: Gson = Gson()
) {
    suspend fun <T> read(key: String, type: Type, fetch: suspend () -> T): CachedRead<T> {
        val userId = requireNotNull(authentication.currentUserId) { "Sign in to open cached data." }
        val live = try {
            fetch()
        } catch (network: IOException) {
            // Do not display snapshots belonging to a different signed-in user.
            if (authentication.currentUserId != userId) throw network
            val saved = dao.find(userId, key) ?: throw network
            return try {
                val value = gson.fromJson<T>(saved.json, type) ?: throw network
                CachedRead(value, true, saved.savedAtUtcMillis)
            } catch (_: com.google.gson.JsonParseException) {
                throw network
            }
        }
        val savedAt = System.currentTimeMillis()
        if (authentication.currentUserId == userId) {
            dao.save(CachedResponse(userId, key, gson.toJson(live), savedAt))
        }
        return CachedRead(live, false, savedAt)
    }

    suspend fun invalidate(key: String) {
        authentication.currentUserId?.let { dao.delete(it, key) }
    }

    suspend fun invalidatePrefix(prefix: String) {
        authentication.currentUserId?.let { dao.deleteByPrefix(it, prefix + "%") }
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/ImageOptimizer.kt

package com.smartfarm.mobile.data.sync

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

/**
 * Optimizes a photo exactly once before it enters the offline sync queue.
 * Keeps already-small PNG/WebP files lossless and respects the API's 5 MB limit.
 */
internal object ImageOptimizer {
    const val MAX_SOURCE_BYTES = 30L * 1024 * 1024
    const val MAX_EDGE = 2560
    const val JPEG_QUALITY = 93
    private val jpegQualities = intArrayOf(JPEG_QUALITY, 90, 87, 85)

    fun optimize(source: File, destination: File, mime: String): String {
        require(source != destination) { "Source and destination must differ." }
        if (source.length() !in 1L..MAX_SOURCE_BYTES) {
            throw IOException("Photo must be smaller than 30 MB before optimization.")
        }
        if (mime != "image/jpeg" && source.length() <= LocalPhotoFiles.MAX_BYTES) {
            source.copyTo(destination, overwrite = true)
            return mime
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > 120_000_000L) {
            throw IOException("Photo cannot be decoded at a safe size.")
        }

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 4096) sample *= 2
        val decoded = BitmapFactory.decodeFile(source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IOException("Photo cannot be decoded.")
        var working = decoded
        try {
            val exif = ExifInterface(source.absolutePath)
            if (exif.rotationDegrees != 0 || exif.isFlipped) {
                val matrix = Matrix().apply {
                    if (exif.isFlipped) postScale(-1f, 1f)
                    postRotate(exif.rotationDegrees.toFloat())
                }
                working = Bitmap.createBitmap(
                    decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
            val longest = maxOf(working.width, working.height)
            if (longest > MAX_EDGE) {
                val scale = MAX_EDGE.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(working,
                    maxOf(1, (working.width * scale).toInt()),
                    maxOf(1, (working.height * scale).toInt()), true)
                if (working !== decoded) working.recycle()
                working = scaled
            }

            if (mime == "image/jpeg") {
                for (quality in jpegQualities) {
                    write(working, destination, Bitmap.CompressFormat.JPEG, quality)
                    if (destination.length() <= LocalPhotoFiles.MAX_BYTES) break
                }
            } else {
                // Oversized lossless photos stay lossless; fail instead of removing transparency.
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.PNG
                write(working, destination, format, 100)
            }

            if (destination.length() > LocalPhotoFiles.MAX_BYTES) {
                throw IOException("Photo cannot fit within 5 MB without reducing quality further.")
            }
            if (source.length() <= LocalPhotoFiles.MAX_BYTES &&
                source.length() <= destination.length()) {
                source.copyTo(destination, overwrite = true)
                return mime
            }
            return if (mime == "image/jpeg") "image/jpeg"
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "image/webp"
                else "image/png"
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally {
            if (working !== decoded) working.recycle()
            decoded.recycle()
        }
    }

    private fun write(bitmap: Bitmap, target: File, format: Bitmap.CompressFormat, quality: Int) {
        target.outputStream().use {
            if (!bitmap.compress(format, quality, it)) throw IOException("Could not optimize photo.")
        }
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/LocalPhotoFiles.kt

package com.smartfarm.mobile.data.sync

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Only app-generated file names are used. Files are not shared or backed up.
class LocalPhotoFiles(context: Context) {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "queued-photos")
    private val cameraDirectory = File(appContext.filesDir, "camera")
    companion object { const val MAX_BYTES = 5 * 1024 * 1024 }

    init {
        // Clean abandoned camera captures after a day; queued copies remain untouched.
        cameraDirectory.listFiles()?.forEach { file ->
            if (file.name.matches(Regex("capture-[0-9a-fA-F-]+\\.jpg")) &&
                System.currentTimeMillis() - file.lastModified() > 86_400_000L) {
                file.delete()
            }
        }
    }

    fun deleteTemporaryCameraSource(uri: Uri) {
        if (uri.authority != "${appContext.packageName}.photos") return
        val name = uri.lastPathSegment ?: return
        if (name.matches(Regex("capture-[0-9a-fA-F-]+\\.jpg")))
            File(cameraDirectory, name).delete()
    }

    fun file(id: String): File {
        val uuid = UUID.fromString(id).toString()
        return File(directory, "$uuid.image")
    }

    // Stage up to 30 MB, then store one compressed file for all upload retries.
    suspend fun copyFrom(resolver: ContentResolver, uri: Uri, id: String): String =
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val destination = file(id)
            val staging = File(directory, "${UUID.fromString(id)}.source")
            val buffer = ByteArray(8192)
            var length = 0L
            try {
                resolver.openInputStream(uri)?.use { input ->
                    staging.outputStream().use { output ->
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            length += count
                            if (length > ImageOptimizer.MAX_SOURCE_BYTES) {
                                throw IOException("Photo must be smaller than 30 MB.")
                            }
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: throw IOException("Cannot open selected photo.")

                val header = ByteArray(12)
                val headerSize = staging.inputStream().use { it.read(header) }
                val mime = when {
                    headerSize >= 3 && header[0] == 0xff.toByte() &&
                        header[1] == 0xd8.toByte() && header[2] == 0xff.toByte() -> "image/jpeg"
                    headerSize >= 8 && header.copyOfRange(0, 8).contentEquals(
                        byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)) -> "image/png"
                    headerSize >= 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                        String(header, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
                    else -> throw IOException("Choose a JPEG, PNG, or WebP photo.")
                }
                if (length == 0L) throw IOException("Empty photo.")
                ImageOptimizer.optimize(staging, destination, mime)
            } catch (error: Exception) {
                destination.delete()
                throw error
            } finally {
                staging.delete()
            }
        }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) { file(id).delete() }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncRepository.kt

package com.smartfarm.mobile.data.sync

import android.content.ContentResolver
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.smartfarm.mobile.data.local.dao.PendingOperationDao
import com.smartfarm.mobile.data.local.dao.PendingPhotoDao
import com.smartfarm.mobile.data.local.entity.PendingOperation
import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.remote.api.MonitoringApi
import com.smartfarm.mobile.data.remote.api.SyncApi
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.AuthRepository
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException

data class StockRetryPreview(
    val operationId: String,
    val itemName: String,
    val currentBalance: BigDecimal,
    val change: BigDecimal,
    val expectedBalance: BigDecimal,
    val currentRowVersion: String
)

class SyncRepository(
    private val auth: AuthRepository,
    private val dao: PendingOperationDao,
    private val photoDao: PendingPhotoDao,
    private val files: LocalPhotoFiles,
    private val api: () -> SyncApi,
    private val monitoringApi: () -> MonitoringApi,
    private val inventoryApi: () -> InventoryApi,
    private val work: WorkManager
) {
    private val gson = Gson()
    private val uploadMutex = Mutex()

    fun watch(): Flow<List<PendingOperation>> =
        dao.watch(requireNotNull(auth.currentUserId) { "Sign in to view pending changes." })

    fun watchPhotos(): Flow<List<PendingPhoto>> =
        photoDao.watch(requireNotNull(auth.currentUserId) { "Sign in to view pending photos." })

    suspend fun queueActivity(farmId: String, cycleId: String, input: ActivityInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "activity", activity = input))

    suspend fun queueObservation(farmId: String, cycleId: String, input: ObservationInputDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "observation", observation = input))

    suspend fun queueInventory(farmId: String, itemId: String, input: InventoryMovementDto): String =
        queue(SyncWriteRequest(farmId, itemId, "inventory", movement = input))

    suspend fun queueHarvest(farmId: String, cycleId: String, input: RecordHarvestDto): String =
        queue(SyncWriteRequest(farmId, cycleId, "harvest", harvest = input))

    private suspend fun queue(input: SyncWriteRequest, operationId: String = UUID.randomUUID().toString()): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        require(input.farmId.isNotBlank() && input.targetId.isNotBlank())
        dao.add(PendingOperation(
            operationId = operationId, userId = user, farmId = input.farmId,
            operationType = input.operationType, payloadJson = gson.toJson(input),
            createdAtUtc = System.currentTimeMillis()))
        schedule()
        return operationId
    }

    private suspend fun stagePhoto(
        farmId: String, cycleId: String, observationId: String?,
        parentOperationId: String?, uri: Uri, resolver: ContentResolver
    ): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        val photoId = UUID.randomUUID().toString()
        val mime = files.copyFrom(resolver, uri, photoId)
        try {
            if (auth.currentUserId != user) throw IllegalStateException("Account changed.")
            photoDao.add(PendingPhoto(
                photoId = photoId, userId = user, farmId = farmId, cycleId = cycleId,
                observationId = observationId, parentOperationId = parentOperationId,
                mimeType = mime, savedAtUtcMillis = System.currentTimeMillis()))
        } catch (error: Exception) {
            files.delete(photoId)
            throw error
        }
        return photoId
    }

    suspend fun queuePhoto(
        farmId: String, cycleId: String, observationId: String,
        uri: Uri, resolver: ContentResolver
    ): String {
        require(UUID.fromString(observationId).toString() == observationId.lowercase())
        val id = stagePhoto(farmId, cycleId, observationId, null, uri, resolver)
        schedule()
        files.deleteTemporaryCameraSource(uri)
        return id
    }

    // One stable observation operation ID is used for both the upload and photo dependency.
    suspend fun queueObservationWithPhoto(
        farmId: String, cycleId: String, input: ObservationInputDto,
        uri: Uri, resolver: ContentResolver
    ): String {
        val user = requireNotNull(auth.currentUserId) { "Sign in before saving." }
        val operationId = UUID.randomUUID().toString()
        val photoId = stagePhoto(farmId, cycleId, null, operationId, uri, resolver)
        try {
            queue(SyncWriteRequest(farmId, cycleId, "observation", observation = input), operationId)
        } catch (error: Exception) {
            photoDao.remove(user, photoId)
            files.delete(photoId)
            throw error
        }
        files.deleteTemporaryCameraSource(uri)
        return operationId
    }

    fun schedule() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        // Ensure a new upload job can follow one that is just finishing.
        work.enqueueUniqueWork("smartfarm-sync", ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build())
    }

    fun enablePeriodicChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()
        work.enqueueUniquePeriodicWork("smartfarm-periodic-sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints).build())
    }

    suspend fun retry(id: String) {
        val user = auth.currentUserId ?: return
        val operation = dao.find(id, user) ?: return
        if (operation.operationType == "inventory") return // Explicit stock review is required.
        dao.retryAfterReview(id, user)
        schedule()
    }

    suspend fun retryPhoto(id: String) {
        val user = auth.currentUserId ?: return
        photoDao.retry(user, id)
        schedule()
    }

    suspend fun discard(id: String) {
        val user = auth.currentUserId ?: return
        for (photo in photoDao.forObservationOperation(user, id)) {
            photoDao.remove(user, photo.photoId)
            files.delete(photo.photoId)
        }
        dao.remove(id, user)
    }

    suspend fun discardPhoto(id: String) {
        val user = auth.currentUserId ?: return
        photoDao.remove(user, id)
        files.delete(id)
    }

    // Never silently rebase. The user must first see and accept the latest stock total.
    suspend fun previewStockRetry(id: String): StockRetryPreview {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(id, user)) { "Operation not found." }
        require(row.status == "review" && row.operationType == "inventory")
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val movement = requireNotNull(input.movement)
        val latest = auth.authorized { inventoryApi().item(it, input.farmId, input.targetId) }
        require(latest.archivedAtUtc == null) { "This inventory item is archived." }
        val next = latest.quantityOnHand + movement.quantityChange
        require(next >= BigDecimal.ZERO && next <= BigDecimal("999999999999999.999")) {
            "Not enough stock, or the quantity is too large."
        }
        return StockRetryPreview(id, latest.name, latest.quantityOnHand,
            movement.quantityChange, next, latest.rowVersion)
    }

    suspend fun confirmStockRetry(preview: StockRetryPreview) {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(preview.operationId, user))
        require(row.operationType == "inventory" && row.status == "review")
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val fresh = auth.authorized { inventoryApi().item(it, input.farmId, input.targetId) }
        require(fresh.rowVersion == preview.currentRowVersion && fresh.archivedAtUtc == null) {
            "Stock changed again. Check the latest balance."
        }
        val movement = requireNotNull(input.movement)
        val updated = input.copy(movement = movement.copy(rowVersion = fresh.rowVersion))
        check(dao.rebaseInventory(row.operationId, user, gson.toJson(updated)) == 1)
        schedule()
    }

    // false requests WorkManager's exponential network/server retry.
    suspend fun uploadPending(): Boolean = uploadMutex.withLock { uploadPendingOnce() }

    private suspend fun uploadPendingOnce(): Boolean {
        val user = auth.currentUserId ?: return true
        while (true) {
            val pending = dao.pending(user)
            if (pending.isEmpty()) break
            for (operation in pending) {
                if (auth.currentUserId != user) return true
                val input = try {
                    gson.fromJson(operation.payloadJson, SyncWriteRequest::class.java)
                } catch (_: Exception) {
                    dao.needsReview(operation.operationId, user, "Invalid local entry.")
                    continue
                }
                try {
                    val response = auth.authorized {
                        api().submit(it, operation.operationId, input)
                    }
                    if (operation.operationType == "observation") {
                        val observationId = response.result.get("id")?.asString
                        if (observationId.isNullOrBlank()) {
                            dao.needsReview(operation.operationId, user,
                                "The server did not return an observation ID.")
                            continue
                        }
                        photoDao.resolveObservation(user, operation.operationId, observationId)
                    }
                    dao.remove(operation.operationId, user)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    when (error.code()) {
                        400, 403, 404, 409, 422 ->
                            dao.needsReview(operation.operationId, user,
                                "Server rejected this change (${error.code()}). Review before retrying.")
                        401 -> return true
                        else -> return false
                    }
                } catch (_: IOException) {
                    return false
                } catch (_: IllegalStateException) {
                    return true
                }
            }
        }
        while (true) {
            val photos = photoDao.ready(user)
            if (photos.isEmpty()) return true
            for (photo in photos) {
                if (auth.currentUserId != user) return true
                val file = files.file(photo.photoId)
                if (!file.exists() || file.length() !in 1..LocalPhotoFiles.MAX_BYTES.toLong()) {
                    photoDao.review(user, photo.photoId,
                        "Local photo is missing or too large. Discard and attach it again.")
                    continue
                }
                try {
                    val body = file.asRequestBody(photo.mimeType.toMediaType())
                    val part = MultipartBody.Part.createFormData("photo", "field-photo", body)
                    auth.authorized { bearer ->
                        monitoringApi().uploadWithId(bearer, photo.farmId, photo.cycleId,
                            requireNotNull(photo.observationId), photo.photoId, part)
                    }
                    photoDao.remove(user, photo.photoId)
                    files.delete(photo.photoId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: HttpException) {
                    when (error.code()) {
                        400, 403, 404, 409, 422 ->
                            photoDao.review(user, photo.photoId,
                                "Photo rejected (${error.code()}). Review before retrying.")
                        401 -> return true
                        else -> return false
                    }
                } catch (_: IOException) {
                    return false
                } catch (_: IllegalStateException) {
                    return true
                }
            }
        }
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncWorker.kt

package com.smartfarm.mobile.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartfarm.mobile.SmartFarmApplication

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as SmartFarmApplication
        return if (app.container.syncRepository.uploadPending()) Result.success() else Result.retry()
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/AuthRepository.kt

package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.remote.dto.AuthTokens
import com.smartfarm.mobile.data.remote.dto.LoginRequest
import com.smartfarm.mobile.data.remote.dto.RefreshRequest
import com.smartfarm.mobile.data.remote.dto.RegisterRequest
import retrofit2.HttpException
import com.google.gson.JsonParser

class FarmCodeException(message: String) : IllegalArgumentException(message)

// Tokens deliberately remain in memory until secure persisted storage is designed.
// Restarting the app requires signing in again; never save tokens in Room/plain preferences.
class AuthRepository(private val onLogin: () -> Unit = {}, private val api: () -> AuthApi) {
    @Volatile var currentUserId: String? = null
        private set
    private var session: AuthTokens? = null

    suspend fun register(name: String, email: String, password: String, confirm: String,
        joinFarm: Boolean = false, farmCode: String? = null) {
        require(!joinFarm || !farmCode.isNullOrBlank()) { "A farm code is required." }
        val response = api().register(RegisterRequest(name.trim(), email.trim(), password, confirm, joinFarm,
            farmCode?.trim().takeIf { joinFarm }))
        if (!response.isSuccessful) {
            val codeError = runCatching {
                val json = JsonParser.parseString(response.errorBody()?.string()).asJsonObject
                json.get("farmCodeError")?.takeUnless { it.isJsonNull }?.asString
                    ?: json.get("errors")?.takeIf { it.isJsonObject }?.asJsonObject
                        ?.getAsJsonArray("FarmCode")?.firstOrNull()?.asString
            }.getOrNull()
            if (joinFarm && !codeError.isNullOrBlank()) throw FarmCodeException(codeError)
            throw HttpException(response)
        }
    }

    suspend fun login(email: String, password: String): AccountResponse {
        val client = api()
        val tokens = client.login(LoginRequest(email.trim(), password))
        val account = client.account("Bearer ${tokens.accessToken}")
        session = tokens
        currentUserId = account.id
        onLogin()
        return account
    }

    suspend fun account(): AccountResponse {
        val client = api()
        var tokens = checkNotNull(session) { "Please sign in." }
        return try {
            client.account("Bearer ${tokens.accessToken}")
        } catch (exception: HttpException) {
            if (exception.code() != 401) throw exception
            tokens = client.refresh(RefreshRequest(tokens.refreshToken))
            val result = client.account("Bearer ${tokens.accessToken}")
            session = tokens
            result
        }
    }

    suspend fun <T> authorized(block: suspend (String) -> T): T {
        var active = checkNotNull(session) { "Please log in." }
        return try {
            block("Bearer ${active.accessToken}")
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val client = api()
            active = client.refresh(RefreshRequest(active.refreshToken))
            val result = block("Bearer ${active.accessToken}")
            session = active
            result
        }
    }

    fun logout() {
        currentUserId = null
        session = null
    }
}




## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/CropRepository.kt

package com.smartfarm.mobile.data.repository

import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.CropApi
import com.smartfarm.mobile.data.remote.dto.*

class CropRepository(
    private val auth: AuthRepository,
    private val cache: OfflineReadCache? = null,
    private val api: () -> CropApi
) {
    private val typeList = TypeToken.getParameterized(List::class.java, CropTypeDto::class.java).type
    private val cycleList = TypeToken.getParameterized(List::class.java, CropCycleDto::class.java).type

    suspend fun typesSnapshot(farmId: String): CachedRead<List<CropTypeDto>> =
        cache?.read("farm/$farmId/types", typeList) {
            auth.authorized { api().cropTypes(it, farmId) }
        } ?: CachedRead(auth.authorized { api().cropTypes(it, farmId) }, false, System.currentTimeMillis())
    suspend fun types(farmId: String): List<CropTypeDto> = typesSnapshot(farmId).value

    suspend fun cyclesSnapshot(farmId: String): CachedRead<List<CropCycleDto>> =
        cache?.read("farm/$farmId/cycles", cycleList) {
            auth.authorized { api().cropCycles(it, farmId) }
        } ?: CachedRead(auth.authorized { api().cropCycles(it, farmId) }, false, System.currentTimeMillis())
    suspend fun cycles(farmId: String): List<CropCycleDto> = cyclesSnapshot(farmId).value

    suspend fun createType(farmId: String, input: CropTypeInputDto): CropTypeDto {
        val created = auth.authorized { api().createCropType(it, farmId, input) }
        cache?.invalidate("farm/$farmId/types")
        return created
    }

    suspend fun plan(farmId: String, plotId: String, input: CropCyclePlanDto): CropCycleDto {
        val created = auth.authorized { api().plan(it, farmId, plotId, input) }
        cache?.invalidate("farm/$farmId/cycles")
        return created
    }

    suspend fun updatePlan(farmId: String, cycleId: String, input: CropCyclePlanDto): CropCycleDto {
        val updated = auth.authorized { api().updatePlan(it, farmId, cycleId, input) }
        cache?.invalidate("farm/$farmId/cycles")
        return updated
    }

    suspend fun transition(farmId: String, cycleId: String, input: CropTransitionDto): CropCycleDto {
        val updated = auth.authorized { api().transition(it, farmId, cycleId, input) }
        cache?.invalidate("farm/$farmId/cycles")
        cache?.invalidate("farm/$farmId/cycle/$cycleId/monitoring")
        cache?.invalidate("farm/$farmId/cycle/$cycleId/harvests")
        return updated
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/FarmRepository.kt

package com.smartfarm.mobile.data.repository

import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.remote.dto.*
import java.lang.reflect.Type

class FarmRepository(
    private val authentication: AuthRepository,
    private val cache: OfflineReadCache? = null,
    private val api: () -> FarmApi
) {
    private val farmListType: Type = TypeToken.getParameterized(List::class.java, FarmDto::class.java).type
    private val plotListType: Type = TypeToken.getParameterized(List::class.java, PlotDto::class.java).type
    private val memberListType: Type = TypeToken.getParameterized(List::class.java, FarmMemberDto::class.java).type

    private suspend fun <T> cached(key: String, type: Type, fetch: suspend () -> T): CachedRead<T> =
        cache?.read(key, type, fetch) ?: CachedRead(fetch(), false, System.currentTimeMillis())

    suspend fun listSnapshot(): CachedRead<List<FarmDto>> =
        cached("farms", farmListType) { authentication.authorized { api().farms(it) } }
    suspend fun list(): List<FarmDto> = listSnapshot().value

    suspend fun farmSnapshot(farmId: String): CachedRead<FarmDto> =
        cached("farm/$farmId", FarmDto::class.java) {
            authentication.authorized { api().farm(it, farmId) }
        }
    suspend fun farm(farmId: String): FarmDto = farmSnapshot(farmId).value

    suspend fun plotsSnapshot(farmId: String): CachedRead<List<PlotDto>> =
        cached("farm/$farmId/plots", plotListType) {
            authentication.authorized { api().plots(it, farmId) }
        }
    suspend fun plots(farmId: String): List<PlotDto> = plotsSnapshot(farmId).value

    suspend fun membersSnapshot(farmId: String): CachedRead<List<FarmMemberDto>> =
        cached("farm/$farmId/members", memberListType) {
            authentication.authorized { api().members(it, farmId) }
        }
    suspend fun members(farmId: String): List<FarmMemberDto> = membersSnapshot(farmId).value

    suspend fun create(input: FarmInputDto): FarmDto {
        val result = authentication.authorized { api().createFarm(it, input) }
        cache?.invalidate("farms")
        return result
    }

    suspend fun updateFarm(farmId: String, input: FarmInputDto): FarmDto {
        val result = authentication.authorized { api().updateFarm(it, farmId, input) }
        cache?.invalidate("farms")
        cache?.invalidate("farm/$farmId")
        cache?.invalidate("farm/$farmId/weather")
        return result
    }
    suspend fun archiveFarm(farmId: String, input: RowVersionDto) {
        authentication.authorized { api().archiveFarm(it, farmId, input) }
        cache?.invalidate("farms")
        cache?.invalidatePrefix("farm/$farmId")
    }

    suspend fun createPlot(farmId: String, input: PlotInputDto): PlotDto {
        val result = authentication.authorized { api().createPlot(it, farmId, input) }
        cache?.invalidate("farm/$farmId/plots")
        return result
    }
    suspend fun updatePlot(farmId: String, plotId: String, input: PlotInputDto): PlotDto {
        val result = authentication.authorized { api().updatePlot(it, farmId, plotId, input) }
        cache?.invalidate("farm/$farmId/plots")
        return result
    }
    suspend fun archivePlot(farmId: String, plotId: String, input: RowVersionDto) {
        authentication.authorized { api().archivePlot(it, farmId, plotId, input) }
        cache?.invalidate("farm/$farmId/plots")
    }

    // Enrollment is online-only: never persist owner codes or applicants in the farm cache.
    suspend fun myJoinRequests(): List<FarmJoinRequestDto> =
        authentication.authorized { api().myJoinRequests(it) }
    suspend fun requestToJoin(code: String): FarmJoinRequestDto =
        authentication.authorized { api().requestToJoin(it, FarmJoinInputDto(code.trim())) }
    suspend fun enrollment(farmId: String): FarmEnrollmentOverviewDto =
        authentication.authorized { api().enrollment(it, farmId) }
    suspend fun decideJoinRequest(farmId: String, requestId: String, approve: Boolean) {
        authentication.authorized { api().decideJoinRequest(it, farmId, requestId, FarmJoinDecisionDto(approve)) }
        cache?.invalidate("farm/$farmId/members")
    }
    suspend fun changeRole(farmId: String, memberId: String, role: Int) {
        authentication.authorized { api().changeRole(it, farmId, memberId, UpdateMemberDto(role)) }
        cache?.invalidate("farm/$farmId/members")
    }
    suspend fun removeMember(farmId: String, memberId: String) {
        authentication.authorized { api().removeMember(it, farmId, memberId) }
        cache?.invalidate("farm/$farmId/members")
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/HarvestRepository.kt

package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.HarvestApi
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.HarvestEntryDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.remote.dto.VoidHarvestDto

class HarvestRepository(private val auth: AuthRepository,
    private val cache: OfflineReadCache? = null, private val api: () -> HarvestApi) {
    suspend fun listSnapshot(farmId: String, cycleId: String): CachedRead<HarvestOverviewDto> =
        cache?.read("farm/$farmId/cycle/$cycleId/harvests", HarvestOverviewDto::class.java) {
            auth.authorized { api().list(it, farmId, cycleId) }
        } ?: CachedRead(auth.authorized { api().list(it, farmId, cycleId) },
            false, System.currentTimeMillis())

    suspend fun list(farmId: String, cycleId: String): HarvestOverviewDto =
        listSnapshot(farmId, cycleId).value

    suspend fun record(farmId: String, cycleId: String, input: RecordHarvestDto): HarvestEntryDto {
        val result = auth.authorized { api().record(it, farmId, cycleId, input) }
        cache?.invalidate("farm/$farmId/cycle/$cycleId/harvests")
        return result
    }

    suspend fun void(farmId: String, cycleId: String, harvestId: String, reason: String): HarvestEntryDto {
        val result = auth.authorized { api().void(it, farmId, cycleId, harvestId, VoidHarvestDto(reason)) }
        cache?.invalidate("farm/$farmId/cycle/$cycleId/harvests")
        return result
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/InventoryRepository.kt

package com.smartfarm.mobile.data.repository

import com.google.gson.reflect.TypeToken
import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.InventoryApi
import com.smartfarm.mobile.data.remote.dto.*

class InventoryRepository(
    private val auth: AuthRepository, private val cache: OfflineReadCache? = null,
    private val api: () -> InventoryApi
) {
    private val transactionList = TypeToken.getParameterized(
        List::class.java, InventoryTransactionDto::class.java).type
    private val activityList = TypeToken.getParameterized(
        List::class.java, InventoryActivityOptionDto::class.java).type

    suspend fun listSnapshot(farmId: String): CachedRead<InventoryWorkspaceDto> =
        cache?.read("farm/$farmId/inventory", InventoryWorkspaceDto::class.java) {
            auth.authorized { api().list(it, farmId) }
        } ?: CachedRead(auth.authorized { api().list(it, farmId) },
            false, System.currentTimeMillis())
    suspend fun list(farmId: String) = listSnapshot(farmId).value

    suspend fun activitiesSnapshot(farmId: String): CachedRead<List<InventoryActivityOptionDto>> =
        cache?.read("farm/$farmId/inventory/activities", activityList) {
            auth.authorized { api().activities(it, farmId) }
        } ?: CachedRead(auth.authorized { api().activities(it, farmId) },
            false, System.currentTimeMillis())
    suspend fun activities(farmId: String) = activitiesSnapshot(farmId).value

    suspend fun itemSnapshot(farmId: String, itemId: String): CachedRead<InventoryItemDto> =
        cache?.read("farm/$farmId/inventory/$itemId", InventoryItemDto::class.java) {
            auth.authorized { api().item(it, farmId, itemId) }
        } ?: CachedRead(auth.authorized { api().item(it, farmId, itemId) },
            false, System.currentTimeMillis())
    suspend fun item(farmId: String, itemId: String) = itemSnapshot(farmId, itemId).value

    suspend fun historySnapshot(farmId: String, itemId: String): CachedRead<List<InventoryTransactionDto>> =
        cache?.read("farm/$farmId/inventory/$itemId/history", transactionList) {
            auth.authorized { api().history(it, farmId, itemId) }
        } ?: CachedRead(auth.authorized { api().history(it, farmId, itemId) },
            false, System.currentTimeMillis())
    suspend fun history(farmId: String, itemId: String) = historySnapshot(farmId, itemId).value

    suspend fun create(farmId: String, input: InventoryItemInputDto): InventoryItemDto =
        auth.authorized { api().create(it, farmId, input) }.also {
            cache?.invalidate("farm/$farmId/inventory")
        }

    suspend fun update(farmId: String, itemId: String, input: InventoryItemInputDto): InventoryItemDto =
        auth.authorized { api().update(it, farmId, itemId, input) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }

    suspend fun archive(farmId: String, itemId: String, version: String) =
        auth.authorized { api().archive(it, farmId, itemId, RowVersionDto(version)) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }

    suspend fun post(farmId: String, itemId: String, input: InventoryMovementDto): InventoryItemDto =
        auth.authorized { api().post(it, farmId, itemId, input) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }

    suspend fun reverse(farmId: String, itemId: String, transactionId: String,
        input: InventoryReversalDto): InventoryItemDto =
        auth.authorized { api().reverse(it, farmId, itemId, transactionId, input) }.also {
            cache?.invalidatePrefix("farm/$farmId/inventory")
        }
}



## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/MonitoringRepository.kt

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



## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/StatusRepository.kt

package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.StatusApi

class StatusRepository(private val api: () -> StatusApi) {
    suspend fun backendAvailable(): Boolean {
        val status = api().check()
        return status.app == "SmartFarm" && status.status == "ok"
    }
}



## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/TaskRepository.kt

package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.TaskApi
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.TaskCloseInput

class TaskRepository(private val auth: AuthRepository, private val api: () -> TaskApi) {
    suspend fun tasks(farmId: String) = auth.authorized { api().tasks(it, farmId) }
    suspend fun notifications() = auth.authorized { api().notifications(it) }
    suspend fun complete(task: TaskDto) = auth.authorized {
        api().close(it, task.farmId, task.id, TaskCloseInput(1, task.rowVersion))
    }
    suspend fun read(id: String) = auth.authorized { api().read(it, id) }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/WeatherRepository.kt

package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.local.CachedRead
import com.smartfarm.mobile.data.local.OfflineReadCache
import com.smartfarm.mobile.data.remote.api.WeatherApi
import com.smartfarm.mobile.data.remote.dto.WeatherOverviewDto

class WeatherRepository(
    private val auth: AuthRepository,
    private val cache: OfflineReadCache,
    private val api: () -> WeatherApi
) {
    suspend fun overview(farmId: String): CachedRead<WeatherOverviewDto> =
        cache.read("farm/$farmId/weather", WeatherOverviewDto::class.java) {
            auth.authorized { api().overview(it, farmId) }
        }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/monitoring/MonitoringViewModel.kt

package com.smartfarm.mobile.features.monitoring

import android.content.ContentResolver
import android.util.LruCache
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.ActivityDto
import com.smartfarm.mobile.data.remote.dto.ObservationDto
import com.smartfarm.mobile.data.repository.MonitoringRepository
import com.smartfarm.mobile.data.sync.SyncRepository
import com.smartfarm.mobile.data.remote.dto.ActivityInputDto
import com.smartfarm.mobile.data.remote.dto.ObservationInputDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MonitoringUiState(
    val farmId: String = "",
    val cycleId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val saveReceipts: Map<String, String> = emptyMap(),
    val canRecord: Boolean = false,
    val activities: List<ActivityDto> = emptyList(),
    val observations: List<ObservationDto> = emptyList(),
    val preview: ByteArray? = null,
    val previewPhotoId: String? = null,
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class MonitoringViewModel(private val repository: MonitoringRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(MonitoringUiState())
    val state = mutable.asStateFlow()
    // Bounded in-memory cache for already-authorized private photo responses.
    // No persistent disk cache; clear it when the user logs out.
    private val photoCache = object : LruCache<String, ByteArray>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    fun clear() {
        photoCache.evictAll()
        mutable.value = MonitoringUiState()
    }

    fun load(farmId: String, cycleId: String) {
        mutable.value = MonitoringUiState(farmId = farmId, cycleId = cycleId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try { refresh(farmId, cycleId) }
            catch (_: Exception) { mutable.value = mutable.value.copy(error = "Could not load monitoring history.") }
            finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    private suspend fun refresh(farmId: String, cycleId: String) {
        val snapshot = repository.listSnapshot(farmId, cycleId)
        val data = snapshot.value
        mutable.value = mutable.value.copy(
            canRecord = data.canRecord, activities = data.activities,
            observations = data.observations, offline = snapshot.offline,
            cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
    }

    private fun submit(form: String, action: suspend (String, String) -> String) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank() || !snapshot.canRecord) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = action(snapshot.farmId, snapshot.cycleId)
                val current = mutable.value
                mutable.value = current.copy(saveReceipts = current.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.cycleId}/$form" to id))
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(notice = "Saved on this device. It will sync when online.")
            } catch (_: Exception) {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(error = "Save failed. Check the values or device storage.")
            } finally {
                if (mutable.value.farmId == snapshot.farmId && mutable.value.cycleId == snapshot.cycleId)
                    mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun logActivity(kind: Int, notes: String) = submit("activity") { farmId, cycleId ->
        sync.queueActivity(farmId, cycleId, ActivityInputDto(kind, notes.trim().ifBlank { null }))
    }

    fun observe(health: Int, growth: String, notes: String,
        uri: Uri?, resolver: ContentResolver) = submit("observation") { farmId, cycleId ->
        val input = ObservationInputDto(health,
            growth.trim().ifBlank { null }, notes.trim().ifBlank { null })
        if (uri == null) sync.queueObservation(farmId, cycleId, input)
        else sync.queueObservationWithPhoto(farmId, cycleId, input, uri, resolver)
    }

    // Attach to an existing observation even without a connection.
    fun uploadPhoto(observationId: String, uri: Uri, resolver: ContentResolver) =
        submit("photo") { farmId, cycleId ->
            sync.queuePhoto(farmId, cycleId, observationId, uri, resolver)
        }

    fun preview(photoId: String) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null,
            preview = null, previewPhotoId = null)
        viewModelScope.launch {
            try {
                // Authenticated Retrofit download; Glide handles decoding and bitmap cache.
                val bytes = photoCache.get(photoId) ?: withContext(Dispatchers.IO) {
                    repository.imageBytes(snapshot.farmId, snapshot.cycleId, photoId)
                }.also { photoCache.put(photoId, it) }
                mutable.value = mutable.value.copy(preview = bytes, previewPhotoId = photoId)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not open photo.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    class Factory(private val repository: MonitoringRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MonitoringViewModel(repository, sync) as T
    }
}



## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/harvests/HarvestViewModel.kt

package com.smartfarm.mobile.features.harvests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.HarvestOverviewDto
import com.smartfarm.mobile.data.remote.dto.RecordHarvestDto
import com.smartfarm.mobile.data.repository.HarvestRepository
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class HarvestUiState(
    val farmId: String = "",
    val cycleId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val saveReceipts: Map<String, String> = emptyMap(),
    val overview: HarvestOverviewDto? = null,
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class HarvestViewModel(private val repository: HarvestRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(HarvestUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = HarvestUiState() }

    fun load(farmId: String, cycleId: String) {
        mutable.value = HarvestUiState(farmId = farmId, cycleId = cycleId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val snapshot = repository.listSnapshot(farmId, cycleId)
                mutable.value = mutable.value.copy(overview = snapshot.value,
                    offline = snapshot.offline,
                    cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load harvest history.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    private fun perform(message: String, operation: suspend (String, String) -> Unit) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.overview == null) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                operation(snapshot.farmId, snapshot.cycleId)
            } catch (_: Exception) {
                // A lost response does not prove the server rejected the write.
                // Never encourage blindly retrying a non-idempotent harvest POST.
                mutable.value = mutable.value.copy(
                    error = "Could not confirm the save. Refresh harvest history before trying again.",
                    busy = false)
                return@launch
            }
            try {
                mutable.value = mutable.value.copy(
                    overview = repository.list(snapshot.farmId, snapshot.cycleId),
                    notice = message)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    notice = "The server confirmed the change, but history could not refresh. Reopen to check it.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun record(quantity: String, notes: String) {
        val value = quantity.toBigDecimalOrNull()
        if (value == null || value <= BigDecimal.ZERO ||
            value.stripTrailingZeros().scale() > 3 ||
            value > BigDecimal("999999999999999.999") || notes.length > 1000) {
            mutable.value = mutable.value.copy(error = "Enter a positive quantity with up to 3 decimals.")
            return
        }
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.overview?.canRecord != true) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = sync.queueHarvest(snapshot.farmId, snapshot.cycleId,
                    RecordHarvestDto(value, notes.trim().ifBlank { null }))
                mutable.value = mutable.value.copy(saveReceipts = mutable.value.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.cycleId}" to id))
                mutable.value = mutable.value.copy(notice =
                    "Harvest saved on this device. Sync status shows when the server confirms it.")
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not save harvest on this device.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun void(harvestId: String, reason: String) {
        if (reason.trim().length !in 3..300) {
            mutable.value = mutable.value.copy(error = "A reason of 3–300 characters is required.")
            return
        }
        perform("Harvest voided; audit history preserved.") { farm, cycle ->
            repository.void(farm, cycle, harvestId, reason.trim())
        }
    }

    class Factory(private val repository: HarvestRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HarvestViewModel(repository, sync) as T
    }
}



## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inventory/InventoryViewModel.kt

package com.smartfarm.mobile.features.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

internal fun inventoryMovementQuantity(type: Int, text: String): BigDecimal {
    val value = text.toBigDecimalOrNull()
    require(type in 2..4 && value != null && value.signum() != 0 &&
        value.stripTrailingZeros().scale() <= 3 && value.abs() <= BigDecimal("999999999999999.999") &&
        (type == 4 || value.signum() > 0)) { "Enter a positive quantity with up to 3 decimals; adjustments may be negative." }
    return if (type == 3) value.negate() else value
}

data class InventoryUiState(
    val farmId: String = "",
    val itemId: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val saveReceipts: Map<String, String> = emptyMap(),
    val workspace: InventoryWorkspaceDto = InventoryWorkspaceDto(emptyList(), 0),
    val item: InventoryItemDto? = null,
    val transactions: List<InventoryTransactionDto> = emptyList(),
    val activities: List<InventoryActivityOptionDto> = emptyList(),
    val offline: Boolean = false,
    val cachedAtUtcMillis: Long? = null
)

class InventoryViewModel(private val repo: InventoryRepository,
    private val sync: SyncRepository) : ViewModel() {
    private val mutable = MutableStateFlow(InventoryUiState())
    val state = mutable.asStateFlow()

    fun clear() { mutable.value = InventoryUiState() }

    fun load(farmId: String) {
        mutable.value = InventoryUiState(farmId = farmId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val snapshot = repo.listSnapshot(farmId)
                mutable.value = mutable.value.copy(workspace = snapshot.value,
                    offline = snapshot.offline,
                    cachedAtUtcMillis = snapshot.savedAtUtcMillis.takeIf { snapshot.offline })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load inventory.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun loadItem(farmId: String, itemId: String) {
        mutable.value = if (mutable.value.farmId == farmId && mutable.value.itemId == itemId)
            mutable.value.copy(busy = true, error = null) else
            InventoryUiState(farmId = farmId, itemId = itemId, busy = true, saveReceipts = mutable.value.saveReceipts)
        viewModelScope.launch {
            try {
                val item = repo.itemSnapshot(farmId, itemId)
                val history = repo.historySnapshot(farmId, itemId)
                val activityResult = if (item.value.archivedAtUtc == null)
                    repo.activitiesSnapshot(farmId) else null
                val snapshots = listOfNotNull(item, history, activityResult)
                val cached = snapshots.any { it.offline }
                mutable.value = mutable.value.copy(
                    item = item.value, transactions = history.value,
                    activities = activityResult?.value ?: emptyList(),
                    offline = cached,
                    cachedAtUtcMillis = snapshots.filter { it.offline }
                        .minOfOrNull { it.savedAtUtcMillis })
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not load inventory item.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    private fun work(after: (() -> Unit)? = null, block: suspend (InventoryUiState) -> Unit) {
        val snapshot = mutable.value
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                block(snapshot)
                mutable.value = mutable.value.copy(workspace = repo.list(snapshot.farmId))
                if (snapshot.itemId.isNotBlank()) {
                    mutable.value = mutable.value.copy(
                        item = repo.item(snapshot.farmId, snapshot.itemId),
                        transactions = repo.history(snapshot.farmId, snapshot.itemId))
                }
                after?.invoke()
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(
                    error = "Stock change failed. Check values and permissions; reload if inventory has changed.")
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }

    fun create(name: String, category: Int, unit: String, starting: String, reorder: String) = work {
        repo.create(it.farmId, InventoryItemInputDto(name.trim(), category, unit.trim(),
            reorderLevel = reorder.takeIf { v -> v.isNotBlank() }?.let(::BigDecimal),
            initialQuantity = BigDecimal(starting)))
    }

    fun update(name: String, category: Int, unit: String, description: String, reorder: String) = work {
        val current = requireNotNull(it.item)
        repo.update(it.farmId, it.itemId, InventoryItemInputDto(name.trim(), category, unit.trim(),
            description.trim().ifBlank { null },
            reorder.takeIf { v -> v.isNotBlank() }?.let(::BigDecimal),
            rowVersion = current.rowVersion))
    }

    fun archive(onSuccess: () -> Unit) = work(onSuccess) {
        val current = requireNotNull(it.item)
        repo.archive(it.farmId, it.itemId, current.rowVersion)
    }

    fun movement(type: Int, quantity: String, activity: String?, reason: String) {
        val snapshot = mutable.value
        val current = snapshot.item ?: return
        if (snapshot.busy || snapshot.farmId.isBlank()) return
        mutable.value = snapshot.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try {
                val id = sync.queueInventory(snapshot.farmId, snapshot.itemId, InventoryMovementDto(
                    type, inventoryMovementQuantity(type, quantity), activity, reason.trim().ifBlank { null },
                    current.rowVersion))
                mutable.value = mutable.value.copy(saveReceipts = mutable.value.saveReceipts +
                    ("${snapshot.farmId}/${snapshot.itemId}" to id))
                mutable.value = mutable.value.copy(notice =
                    "Stock change queued. Do not record it again; check offline sync status.")
            } catch (invalid: IllegalArgumentException) {
                mutable.value = mutable.value.copy(error = invalid.message)
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(error = "Could not save the change on this device.")
            } finally {
                mutable.value = mutable.value.copy(busy = false)
            }
        }
    }

    fun reverse(transactionId: String, reason: String) = work {
        val current = requireNotNull(it.item)
        repo.reverse(it.farmId, it.itemId, transactionId,
            InventoryReversalDto(current.rowVersion, reason.trim()))
    }

    class Factory(private val repo: InventoryRepository,
        private val sync: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InventoryViewModel(repo, sync) as T
    }
}



## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/tasks/TaskViewModel.kt

package com.smartfarm.mobile.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.NotificationDto
import com.smartfarm.mobile.data.repository.TaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskUiState(val farmId: String? = null, val busy: Boolean = false, val error: String? = null,
    val tasks: List<TaskDto> = emptyList(), val notifications: List<NotificationDto> = emptyList())

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val mutable = MutableStateFlow(TaskUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun load(farmId: String? = null) {
        job?.cancel()
        mutable.value = TaskUiState(farmId = farmId)
        request(farmId)
    }

    private fun request(farmId: String?, change: suspend () -> Unit = {}) {
        mutable.value = mutable.value.copy(busy = true, error = null)
        job = viewModelScope.launch {
            try {
                change()
                mutable.value = if (farmId == null) TaskUiState(notifications = repository.notifications())
                    else TaskUiState(farmId = farmId, tasks = repository.tasks(farmId))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.value = mutable.value.copy(busy = false,
                    error = "Could not refresh these records. Check your connection and try again before repeating an update.")
            }
        }
    }

    fun complete(task: TaskDto) {
        if (!mutable.value.busy) request(task.farmId) { repository.complete(task) }
    }
    fun read(id: String) {
        if (!mutable.value.busy) request(null) { repository.read(id) }
    }
    fun clear() { job?.cancel(); mutable.value = TaskUiState() }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TaskViewModel(repository) as T
    }
}


## Repository file: mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/SmartFarmNavHost.kt

package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthViewModel
import com.smartfarm.mobile.features.status.StatusViewModel
import com.smartfarm.mobile.features.auth.LoginScreen
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.ui.components.WelcomeScreen
import com.smartfarm.mobile.features.farms.FarmViewModel
import com.smartfarm.mobile.features.crops.CropViewModel
import com.smartfarm.mobile.features.crops.CropScreen
import com.smartfarm.mobile.features.monitoring.MonitoringViewModel
import com.smartfarm.mobile.features.monitoring.MonitoringScreen
import com.smartfarm.mobile.features.inventory.InventoryViewModel
import com.smartfarm.mobile.features.inventory.InventoryListScreen
import com.smartfarm.mobile.features.inventory.InventoryItemScreen
import com.smartfarm.mobile.features.harvests.HarvestViewModel
import com.smartfarm.mobile.features.harvests.HarvestScreen
import com.smartfarm.mobile.features.syncstatus.SyncStatusViewModel
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmDetailScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel, farms: FarmViewModel, crops: CropViewModel, monitoring: MonitoringViewModel, inventory: InventoryViewModel, harvests: HarvestViewModel, status: StatusViewModel, sync: SyncStatusViewModel, tasks: com.smartfarm.mobile.features.tasks.TaskViewModel) {
    val navController = rememberNavController()
    val appContext = LocalContext.current
    val resolver = appContext.contentResolver
    val state by auth.state.collectAsStateWithLifecycle()
    val pending by sync.operations.collectAsStateWithLifecycle()
    LaunchedEffect(state.account?.id) {
        farms.clear()
        if (state.account != null) sync.load()
    }
    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            val statusState by status.state.collectAsStateWithLifecycle()
            WelcomeScreen(
                status = statusState,
                onCheckBackend = { status.check() },
                onLogin = { navController.navigate("login") },
                onRegister = { navController.navigate("register") })
        }
        composable("login") {
            LoginScreen(state,
                onSubmit = { email, password ->
                    auth.login(email, password) {
                        navController.navigate("account") { popUpTo("welcome") }
                    }
                },
                onRegister = { navController.navigate("register") })
        }
        composable("register") {
            RegisterScreen(state,
                onSubmit = { name, email, password, confirm, joinFarm, farmCode ->
                    auth.register(name, email, password, confirm, joinFarm, farmCode) {
                        navController.navigate("login") { popUpTo("welcome") }
                    }
                },
                onLogin = { navController.navigate("login") })
        }
        composable("account") {
            AccountScreen(state, onLogout = {
                auth.logout()
                farms.clear()
                crops.clear()
                monitoring.clear()
                // Remove any decoded private photos after signing out.
                Glide.get(appContext).clearMemory()
                inventory.clear()
                harvests.clear()
                sync.clear()
                tasks.clear()
                navController.navigate("welcome") { popUpTo("welcome") { inclusive = true } }
            }, onFarms = { navController.navigate("farms") },
                onSync = { navController.navigate("sync") },
                onNotifications = { navController.navigate("notifications") })
        }
        composable("sync") {
            val pending by sync.operations.collectAsStateWithLifecycle()
            val photos by sync.photos.collectAsStateWithLifecycle()
            val preview by sync.stockPreview.collectAsStateWithLifecycle()
            val syncError by sync.error.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { sync.load() }
            SyncStatusScreen(pending, photos, preview, syncError,
                onBack = { navController.popBackStack() },
                onRetry = { sync.retry(it) },
                onDiscard = { sync.discard(it) },
                onRetryPhoto = { sync.retryPhoto(it) },
                onDiscardPhoto = { sync.discardPhoto(it) },
                onReviewStock = { sync.reviewStock(it) },
                onApproveStock = { sync.approveStockRetry() },
                onCancelStock = { sync.cancelReview() },
                onSyncNow = { sync.syncNow() })
        }
        composable("farms") {
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { farms.load() }
            FarmListScreen(farmState,
                onOpen = { navController.navigate("farm/$it") },
                onCreate = { name, location, zone, latitude, longitude ->
                    farms.create(name, location, zone, latitude, longitude) {
                    navController.navigate("farm/$it")
                } },
                onRequest = farms::requestToJoin,
                onBack = { navController.popBackStack() }, onRetry = { farms.load() })
        }
        composable("farm/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { farms.loadDetail(farmId) }
            FarmDetailScreen(farmState,
                onBack = { navController.popBackStack() },
                onSaveFarm = { name, location, zone, latitude, longitude ->
                    farms.saveFarm(name, location, zone, latitude, longitude) {} },
                onArchiveFarm = { farms.archiveFarm { navController.popBackStack() } },
                onAddPlot = { name, area -> farms.addPlot(name, area) },
                onSavePlot = { plot, name, area -> farms.savePlot(plot, name, area) },
                onArchivePlot = { plot -> farms.archivePlot(plot) },
                onCrops = { navController.navigate("crops/$farmId") },
                onInventory = { navController.navigate("inventory/$farmId") },
                onDecision = farms::decideJoinRequest,
                onChangeRole = { member, role -> farms.changeRole(member, role) },
                onRemoveMember = { farms.remove(it) },
                onTasks = { navController.navigate("tasks/$farmId") }, onRetry = { farms.loadDetail(farmId) })
        }
        composable("tasks/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { tasks.load(farmId) }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState,
                canManage = farmState.selected?.id == farmId && farmState.selected?.role in listOf("Owner", "Manager"),
                actorId = state.account?.id, onBack = { navController.popBackStack() },
                onRetry = { tasks.load(farmId) }, onComplete = tasks::complete, onRead = tasks::read)
        }
        composable("notifications") {
            val taskState by tasks.state.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) { tasks.load() }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState, false, state.account?.id,
                onBack = { navController.popBackStack() }, onRetry = { tasks.load() },
                onComplete = tasks::complete, onRead = tasks::read)
        }
        composable("crops/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cropState by crops.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { crops.load(farmId) }
            CropScreen(cropState,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.popBackStack() },
                onCreateType = { name, unit, growth, watering ->
                    crops.createType(name, unit, growth, watering)
                },
                onPlan = { plotId, typeId, date, expected ->
                    crops.plan(plotId, typeId, date, expected)
                },
                onUpdatePlan = { cycle, date, expected -> crops.updatePlan(cycle, date, expected) },
                onMonitoring = { cycle -> navController.navigate("monitor/$farmId/${cycle.id}") },
                onHarvests = { cycle -> navController.navigate("harvests/$farmId/${cycle.id}") },
                onTransition = { cycle, status, planted, reason ->
                    crops.transition(cycle, status, planted, reason)
                }, onRetry = { crops.load(farmId) })
        }
        composable("monitor/{farmId}/{cycleId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val monitoringState by monitoring.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, cycleId) { monitoring.load(farmId, cycleId) }
            MonitoringScreen(monitoringState,
                onBack = { navController.popBackStack() },
                onActivity = { kind, notes -> monitoring.logActivity(kind, notes) },
                onObservation = { health, growth, notes, photo ->
                    monitoring.observe(health, growth, notes, photo, resolver)
                },
                onUpload = { observationId, uri ->
                    monitoring.uploadPhoto(observationId, uri, resolver)
                },
                onPreview = { monitoring.preview(it) }, pending = pending,
                onSync = { navController.navigate("sync") }, onRetry = { monitoring.load(farmId, cycleId) })
        }
        composable("inventory/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val state by inventory.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { inventory.load(farmId) }
            InventoryListScreen(state,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.popBackStack() },
                onOpen = { navController.navigate("inventory/$farmId/$it") },
                onCreate = { name, category, unit, opening, reorder ->
                    inventory.create(name, category, unit, opening, reorder)
                }, onRetry = { inventory.load(farmId) })
        }
        composable("inventory/{farmId}/{itemId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val itemId = entry.arguments?.getString("itemId").orEmpty()
            val state by inventory.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, itemId) { inventory.loadItem(farmId, itemId) }
            InventoryItemScreen(state,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                onBack = { navController.popBackStack() },
                onSave = { name, category, unit, description, reorder ->
                    inventory.update(name, category, unit, description, reorder)
                },
                onArchive = { inventory.archive { navController.popBackStack() } },
                onMovement = { kind, quantity, activityId, reason ->
                    inventory.movement(kind, quantity, activityId, reason)
                },
                onReverse = { transactionId, reason -> inventory.reverse(transactionId, reason) },
                pending = pending, onSync = { navController.navigate("sync") },
                onRetry = { inventory.loadItem(farmId, itemId) })
        }
        composable("harvests/{farmId}/{cycleId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val harvestState by harvests.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId, cycleId) { harvests.load(farmId, cycleId) }
            HarvestScreen(
                state = harvestState,
                onBack = { navController.popBackStack() },
                onRecord = { quantity, notes -> harvests.record(quantity, notes) },
                onVoid = { harvestId, reason -> harvests.void(harvestId, reason) },
                pending = pending, onSync = { navController.navigate("sync") },
                onRetry = { harvests.load(farmId, cycleId) }
            )
        }
    }
}






