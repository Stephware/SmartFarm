# SmartFarm android-current source capture

> Source: local repository at ac624be7e5e721f00d990d01f720c682f1977930
> Collected: 2026-11-11
> Published: Unknown

Source evidence only. No build, deployment, SQL or device success is implied.

## `mobile/SmartFarm.Android/app/build.gradle.kts`

````text
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
    implementation("androidx.room:room-ktx:2.8.5")
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
````

## `mobile/SmartFarm.Android/build.gradle.kts`

````text
plugins {
    id("com.android.application") version "9.1.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("androidx.room") version "2.8.5" apply false
}
````

## `mobile/SmartFarm.Android/gradle/wrapper/gradle-wrapper.properties`

````text
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionSha256Sum=b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/di/AppContainer.kt`

````text
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
import com.smartfarm.mobile.data.remote.api.FarmInsightsApi
import com.smartfarm.mobile.data.repository.FarmInsightsRepository

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

    val insightsRepository: FarmInsightsRepository by lazy {
        FarmInsightsRepository(authRepository) { retrofit.create(FarmInsightsApi::class.java) }
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
            com.smartfarm.mobile.data.sync.OfflineObservationQueue(database),
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
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/SmartFarmDatabase.kt`

````text
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
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/OfflineReadCache.kt`

````text
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
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/AuthRepository.kt`

````text
package com.smartfarm.mobile.data.repository

import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.data.remote.dto.AuthTokens
import com.smartfarm.mobile.data.remote.dto.LoginRequest
import com.smartfarm.mobile.data.remote.dto.RefreshRequest
import com.smartfarm.mobile.data.remote.dto.RegisterRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

// Tokens remain in memory. Restarting the app requires signing in again.
// A mutex serializes refresh; the generation prevents any in-flight request
// from reviving credentials or returning private data after logout/account switch.
class AuthRepository(private val onLogin: () -> Unit = {}, private val api: () -> AuthApi) {
    private val sessionLock = Any()
    private val refreshMutex = Mutex()
    private var session: AuthTokens? = null
    private var generation = 0L

    @Volatile var currentUserId: String? = null
        private set

    private data class SessionSnapshot(val tokens: AuthTokens, val generation: Long)

    private fun activeSession(): SessionSnapshot = synchronized(sessionLock) {
        SessionSnapshot(checkNotNull(session) { "Please sign in." }, generation)
    }

    private fun checkSession(snapshot: SessionSnapshot) = synchronized(sessionLock) {
        check(generation == snapshot.generation && session != null) {
            "Session changed. Please sign in again."
        }
    }

    suspend fun register(name: String, email: String, password: String) {
        val response = api().register(RegisterRequest(name.trim(), email.trim(), password))
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun login(email: String, password: String): AccountResponse {
        val startingGeneration = synchronized(sessionLock) { generation }
        val client = api()
        val tokens = client.login(LoginRequest(email.trim(), password))
        val account = client.account("Bearer ${tokens.accessToken}")
        val committedGeneration = synchronized(sessionLock) {
            check(generation == startingGeneration) {
                "Session changed while signing in. Please try again."
            }
            session = tokens
            currentUserId = account.id
            ++generation
        }
        // Do not trigger login work if another thread has already logged out.
        if (synchronized(sessionLock) { generation == committedGeneration && session != null }) {
            onLogin()
        }
        return account
    }

    suspend fun account(): AccountResponse = authorized { api().account(it) }

    /**
     * A 401 can occur for multiple simultaneous requests using the old token.
     * Only the first one rotates it; waiting callers reuse the newest token.
     */
    private suspend fun refreshAfterUnauthorized(original: SessionSnapshot): SessionSnapshot =
        refreshMutex.withLock {
            val active = activeSession()
            check(active.generation == original.generation) {
                "Session changed. Please sign in again."
            }
            if (active.tokens != original.tokens) return@withLock active
            val fresh = api().refresh(RefreshRequest(active.tokens.refreshToken))
            synchronized(sessionLock) {
                check(generation == original.generation && session == active.tokens) {
                    "Session changed. Please sign in again."
                }
                session = fresh
            }
            SessionSnapshot(fresh, original.generation)
        }

    suspend fun <T> authorized(block: suspend (String) -> T): T {
        val original = activeSession()
        val result = try {
            block("Bearer ${original.tokens.accessToken}")
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            val refreshed = refreshAfterUnauthorized(original)
            // Logout after refresh must prevent even a retry with the old account.
            checkSession(refreshed)
            val retried = block("Bearer ${refreshed.tokens.accessToken}")
            checkSession(refreshed)
            return retried
        }
        checkSession(original)
        return result
    }

    fun logout() = synchronized(sessionLock) {
        ++generation
        session = null
        currentUserId = null
    }
}
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncRepository.kt`

````text
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
    val currentRowVersion: String,
    val includesFieldActivity: Boolean = false
)

class SyncRepository(
    private val auth: AuthRepository,
    private val dao: PendingOperationDao,
    private val photoDao: PendingPhotoDao,
    private val files: LocalPhotoFiles,
    private val observationQueue: OfflineObservationQueue,
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

    suspend fun queueActivityWithStock(
        farmId: String, cycleId: String, input: ActivityStockInputDto
    ): String {
        require(input.activity.activityType in 2..3)
        require(input.quantityUsed > BigDecimal.ZERO && input.quantityUsed.scale() <= 3)
        require(input.inventoryItemId.isNotBlank() && input.rowVersion.isNotBlank())
        return queue(SyncWriteRequest(farmId, cycleId, "activity_stock", activityStock = input))
    }

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
        val photoId = UUID.randomUUID().toString()

        // Produce the immutable upload file once. Only publish the matching Room
        // rows in one transaction, so a process crash cannot leave a stranded photo.
        val mime = files.copyFrom(resolver, uri, photoId)
        try {
            if (auth.currentUserId != user) throw IllegalStateException("Account changed.")
            val operation = PendingOperation(
                operationId = operationId, userId = user, farmId = farmId,
                operationType = "observation",
                payloadJson = gson.toJson(SyncWriteRequest(
                    farmId, cycleId, "observation", observation = input)),
                createdAtUtc = System.currentTimeMillis())
            val photo = PendingPhoto(
                photoId = photoId, userId = user, farmId = farmId, cycleId = cycleId,
                observationId = null, parentOperationId = operationId,
                mimeType = mime, savedAtUtcMillis = System.currentTimeMillis())
            observationQueue.add(operation, photo)
        } catch (error: Exception) {
            files.delete(photoId)
            throw error
        }
        schedule()
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
        if (operation.operationType in setOf("inventory", "activity_stock"))
            return // Explicit stock review is required.
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
        // A guessed/stale ID must never remove another account's private file.
        if (photoDao.find(user, id) == null) return
        photoDao.remove(user, id)
        files.delete(id)
    }

    // Never silently rebase. The user must review the latest stock total first.
    suspend fun previewStockRetry(id: String): StockRetryPreview {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(id, user)) { "Operation not found." }
        require(row.status == "review" &&
            row.operationType in setOf("inventory", "activity_stock"))
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val combined = input.activityStock
        val itemId = if (row.operationType == "activity_stock") {
            requireNotNull(combined).inventoryItemId
        } else input.targetId
        val change = if (row.operationType == "activity_stock") {
            requireNotNull(combined).quantityUsed.negate()
        } else requireNotNull(input.movement).quantityChange
        val latest = auth.authorized { inventoryApi().item(it, input.farmId, itemId) }
        require(latest.archivedAtUtc == null) { "This supply is archived." }
        if (combined != null) {
            val expectedCategory = if (combined.activity.activityType == 2) 2 else 3
            require(combined.activity.activityType in 2..3 &&
                latest.category == expectedCategory) {
                "The chosen stock item no longer matches this activity."
            }
        }
        val next = latest.quantityOnHand + change
        require(next >= BigDecimal.ZERO && next <= BigDecimal("999999999999999.999")) {
            "Not enough stock, or the quantity is too large."
        }
        return StockRetryPreview(id, latest.name, latest.quantityOnHand,
            change, next, latest.rowVersion,
            includesFieldActivity = row.operationType == "activity_stock")
    }

    suspend fun confirmStockRetry(preview: StockRetryPreview) {
        val user = requireNotNull(auth.currentUserId) { "Sign in first." }
        val row = requireNotNull(dao.find(preview.operationId, user))
        require(row.status == "review" &&
            row.operationType in setOf("inventory", "activity_stock"))
        val input = gson.fromJson(row.payloadJson, SyncWriteRequest::class.java)
        val combined = input.activityStock
        val itemId = if (row.operationType == "activity_stock") {
            requireNotNull(combined).inventoryItemId
        } else input.targetId
        val fresh = auth.authorized { inventoryApi().item(it, input.farmId, itemId) }
        require(fresh.rowVersion == preview.currentRowVersion && fresh.archivedAtUtc == null) {
            "Stock changed again. Check the latest balance."
        }
        val change = if (row.operationType == "activity_stock") {
            val details = requireNotNull(combined)
            val expectedCategory = if (details.activity.activityType == 2) 2 else 3
            require(details.activity.activityType in 2..3 &&
                fresh.category == expectedCategory) { "Supply category changed." }
            details.quantityUsed.negate()
        } else requireNotNull(input.movement).quantityChange
        require(fresh.quantityOnHand + change >= BigDecimal.ZERO) {
            "Not enough stock. Check the latest balance."
        }
        val updated = if (row.operationType == "activity_stock") {
            input.copy(activityStock = requireNotNull(combined).copy(rowVersion = fresh.rowVersion))
        } else {
            input.copy(movement = requireNotNull(input.movement).copy(rowVersion = fresh.rowVersion))
        }
        check(dao.rebaseInventory(row.operationId, user, gson.toJson(updated)) == 1)
        schedule()
    }

    // false requests WorkManager's exponential network/server retry.
    suspend fun uploadPending(): Boolean = uploadMutex.withLock { uploadPendingOnce() }

    private suspend fun uploadPendingOnce(): Boolean {
        val user = auth.currentUserId ?: return true
        // Reconcile abandoned partial files; every account's queued IDs are protected.
        files.pruneAbandoned(photoDao.allPhotoIds().toSet())
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
                    // Never discard local data based on an unrelated/malformed reply.
                    if (!SyncReceiptValidator.operationMatches(
                            response, operation.operationId, operation.operationType)) {
                        dao.needsReview(operation.operationId, user,
                            "Unexpected sync confirmation. Review before retrying.")
                        continue
                    }
                    if (operation.operationType == "activity_stock" &&
                        !SyncReceiptValidator.activityStockMatches(response, input.activityStock)) {
                        dao.needsReview(operation.operationId, user,
                            "Unexpected activity and stock confirmation. Review before retrying.")
                        continue
                    }
                    if (operation.operationType == "observation") {
                        val observationId = SyncReceiptValidator.observationId(response)
                        if (observationId == null) {
                            dao.needsReview(operation.operationId, user,
                                "The server did not return a valid observation ID.")
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
                    val saved = auth.authorized { bearer ->
                        monitoringApi().uploadWithId(bearer, photo.farmId, photo.cycleId,
                            requireNotNull(photo.observationId), photo.photoId, part)
                    }
                    if (!SyncReceiptValidator.photoMatches(saved, photo)) {
                        photoDao.review(user, photo.photoId,
                            "Unexpected photo confirmation. Review before retrying.")
                        continue
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
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncReceiptValidator.kt`

````text
package com.smartfarm.mobile.data.sync

import com.smartfarm.mobile.data.local.entity.PendingPhoto
import com.smartfarm.mobile.data.remote.dto.ObservationPhotoDto
import com.smartfarm.mobile.data.remote.dto.SyncWriteResponse
import com.smartfarm.mobile.data.remote.dto.ActivityStockInputDto
import java.util.UUID

/** Never remove queued changes unless their server receipts identify those changes. */
internal object SyncReceiptValidator {
    fun operationMatches(response: SyncWriteResponse, operationId: String, type: String): Boolean =
        response.operationId.equals(operationId, ignoreCase = true) &&
            response.operationType == type

    fun observationId(response: SyncWriteResponse): String? = runCatching {
        UUID.fromString(response.result.get("id")?.asString).toString()
    }.getOrNull()

    fun activityStockMatches(
        response: SyncWriteResponse, input: ActivityStockInputDto?
    ): Boolean = runCatching {
        val payload = requireNotNull(input)
        val activityId = response.result.getAsJsonObject("activity").get("id").asString
        val inventoryId = response.result.getAsJsonObject("inventory").get("id").asString
        UUID.fromString(activityId)
        UUID.fromString(inventoryId)
        inventoryId.equals(payload.inventoryItemId, ignoreCase = true)
    }.getOrDefault(false)

    fun photoMatches(response: ObservationPhotoDto, photo: PendingPhoto): Boolean =
        response.id.equals(photo.photoId, ignoreCase = true) &&
            response.observationId.equals(photo.observationId, ignoreCase = true)
}
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inventory/InventoryViewModel.kt`

````text
package com.smartfarm.mobile.features.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.data.repository.InventoryRepository
import com.smartfarm.mobile.data.repository.NetworkErrorMessages
import kotlinx.coroutines.CancellationException
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutable.value = mutable.value.copy(error = NetworkErrorMessages.forRequest(
                    error, "Could not load inventory."))
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutable.value = mutable.value.copy(error = NetworkErrorMessages.forRequest(
                    error, "Could not load inventory item."))
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
            } catch (cancelled: CancellationException) {
                throw cancelled
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
            } catch (cancelled: CancellationException) {
                throw cancelled
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
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/insights/FarmInsightsViewModel.kt`

````text
package com.smartfarm.mobile.features.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartfarm.mobile.data.remote.dto.FarmHarvestSummaryDto
import com.smartfarm.mobile.data.remote.dto.FarmFinanceSummaryDto
import com.smartfarm.mobile.data.repository.FarmInsightsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

data class FarmInsightsUiState(
    val farmId: String = "", val loading: Boolean = false,
    val harvest: FarmHarvestSummaryDto? = null,
    val finance: FarmFinanceSummaryDto? = null,
    val harvestError: String? = null, val financeError: String? = null,
    val from: String? = null, val to: String? = null,
    val loaded: Boolean = false
)

class FarmInsightsViewModel(private val repo: FarmInsightsRepository) : ViewModel() {
    private val mutable = MutableStateFlow(FarmInsightsUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null

    fun clear() {
        job?.cancel()
        mutable.value = FarmInsightsUiState()
    }

    fun load(farmId: String, from: String? = null, to: String? = null) {
        job?.cancel()
        // Discard prior farm data before switching identity or period.
        mutable.value = FarmInsightsUiState(farmId = farmId, from = from, to = to, loading = true)
        job = viewModelScope.launch {
            try {
                val harvest = try {
                    repo.harvest(farmId, from, to)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                mutable.value = mutable.value.copy(
                    harvest = harvest,
                    harvestError = if (harvest == null) "Harvest report unavailable. Connect and retry." else null
                )
                val finance = try {
                    repo.finances(farmId, from, to)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                mutable.value = mutable.value.copy(
                    finance = finance, financeError = if (finance == null)
                        "Finance insights unavailable. Check connection and access." else null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } finally {
                // A cancelled request must never overwrite a newer farm, period, or logout.
                if (job === coroutineContext[Job]) {
                    mutable.value = mutable.value.copy(loading = false, loaded = true)
                }
            }
        }
    }

    class Factory(private val repo: FarmInsightsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FarmInsightsViewModel(repo) as T
    }
}
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/farms/WeatherFreshness.kt`

````text
package com.smartfarm.mobile.features.farms

import com.smartfarm.mobile.data.remote.dto.WeatherDayDto
import java.time.Duration
import java.time.Instant

/** Forecast updates are expected every 6 hours; two missed updates mark them stale. */
internal object WeatherFreshness {
    private val maxAge: Duration = Duration.ofHours(12)
    private val allowedClockSkew: Duration = Duration.ofMinutes(5)

    fun isStale(days: List<WeatherDayDto>, now: Instant = Instant.now()): Boolean {
        if (days.isEmpty()) return false
        return days.any { day ->
            val checked = runCatching { Instant.parse(day.retrievedAtUtc) }.getOrNull()
                ?: return@any true
            checked.isBefore(now.minus(maxAge)) || checked.isAfter(now.plus(allowedClockSkew))
        }
    }
}
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/SmartFarmNavHost.kt`

````text
package com.smartfarm.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
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
import com.smartfarm.mobile.features.insights.FarmInsightsViewModel
import com.smartfarm.mobile.features.insights.FarmInsightsScreen
import com.smartfarm.mobile.features.syncstatus.SyncStatusViewModel
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.Glide
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmDetailScreen

@Composable
fun SmartFarmNavHost(auth: AuthViewModel, farms: FarmViewModel, crops: CropViewModel, monitoring: MonitoringViewModel, inventory: InventoryViewModel, harvests: HarvestViewModel, status: StatusViewModel, sync: SyncStatusViewModel, tasks: com.smartfarm.mobile.features.tasks.TaskViewModel, insights: FarmInsightsViewModel) {
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val currentFarmId = currentEntry?.arguments?.getString("farmId").orEmpty()
    val showFarmTabs = currentRoute in setOf(
        "farm/{farmId}", "tasks/{farmId}", "crops/{farmId}", "inventory/{farmId}"
    ) && currentFarmId.isNotBlank()
    val appContext = LocalContext.current
    val resolver = appContext.contentResolver
    val state by auth.state.collectAsStateWithLifecycle()
    val pending by sync.operations.collectAsStateWithLifecycle()
    LaunchedEffect(state.account?.id) {
        farms.clear()
        if (state.account != null) sync.load()
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (state.account != null && showFarmTabs) {
                FarmWorkspaceBar(selectedRoute = currentRoute.orEmpty()) { destination ->
                    if (destination == "account") {
                        navController.navigate("account") { launchSingleTop = true }
                    } else {
                        val route = destination.replace("{farmId}", currentFarmId)
                        navController.navigate(route) {
                            // Keep the active farm as the shared root of all workspace tabs.
                            launchSingleTop = true
                            restoreState = true
                            popUpTo("farm/$currentFarmId") { saveState = true }
                        }
                    }
                }
            }
        }
    ) { workspacePadding ->
    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = Modifier.padding(workspacePadding)
    ) {
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
                        navController.navigate("farms") { popUpTo("welcome") }
                    }
                },
                onRegister = { navController.navigate("register") })
        }
        composable("register") {
            RegisterScreen(state,
                onSubmit = { name, email, password ->
                    auth.register(name, email, password) {
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
                insights.clear()
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
                onBack = { navController.navigate("account") }, onRetry = { farms.load() },
                onCancelRequest = farms::cancelJoinRequest)
        }
        composable("farm/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val farmState by farms.state.collectAsStateWithLifecycle()
            val homeTasks by tasks.state.collectAsStateWithLifecycle()
            val homeCrops by crops.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) {
                farms.loadDetail(farmId)
                tasks.load(farmId)
                crops.load(farmId)
            }
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
                onTasks = { navController.navigate("tasks/$farmId") },
                onInsights = { navController.navigate("insights/$farmId") },
                onRetry = { farms.loadDetail(farmId) },
                onRegenerateCode = farms::regenerateJoinCode,
                homeTasks = homeTasks, homeCrops = homeCrops, actorId = state.account?.id)
        }
        composable("tasks/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { tasks.load(farmId) }
            com.smartfarm.mobile.features.tasks.TaskScreen(taskState,
                canManage = farmState.selected?.id == farmId &&
                    farmState.selected?.role in listOf("Owner", "Manager"),
                actorId = state.account?.id,
                isOwner = farmState.selected?.id == farmId && farmState.selected?.role == "Owner",
                onBack = { navController.popBackStack() },
                onRetry = { tasks.load(farmId) }, onComplete = tasks::complete, onRead = tasks::read,
                onCreate = tasks::create, onUpdate = tasks::update,
                onOpenCrop = { task ->
                    val cycleId = task.cropCycleId
                    if (cycleId != null && taskState.cycles.any { it.id == cycleId && it.status == 1 }) {
                        when (task.taskType) {
                            5 -> navController.navigate("harvests/$farmId/$cycleId")
                            2 -> navController.navigate("monitor/$farmId/$cycleId?quick=watering")
                            4 -> navController.navigate("monitor/$farmId/$cycleId?quick=inspection")
                            else -> navController.navigate("monitor/$farmId/$cycleId")
                        }
                    }
                })
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
                onQuickMonitoring = { cycle, mode ->
                    navController.navigate("monitor/$farmId/${cycle.id}?quick=$mode")
                },
                onHarvests = { cycle -> navController.navigate("harvests/$farmId/${cycle.id}") },
                onTransition = { cycle, status, planted, reason ->
                    crops.transition(cycle, status, planted, reason)
                }, onRetry = { crops.load(farmId) })
        }
        composable("monitor/{farmId}/{cycleId}?quick={quick}",
            arguments = listOf(navArgument("quick") {
                type = NavType.StringType
                defaultValue = ""
            })) { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val cycleId = entry.arguments?.getString("cycleId").orEmpty()
            val monitoringState by monitoring.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            val taskState by tasks.state.collectAsStateWithLifecycle()
            val manager = farmState.selected?.id == farmId &&
                farmState.selected?.role in listOf("Owner", "Manager")
            LaunchedEffect(farmId, cycleId) {
                monitoring.load(farmId, cycleId)
                if (manager) tasks.load(farmId)
            }
            MonitoringScreen(monitoringState,
                onBack = { navController.popBackStack() },
                onActivity = { kind, notes -> monitoring.logActivity(kind, notes) },
                onObservation = { health, growth, notes, photo ->
                    monitoring.observe(health, growth, notes, photo, resolver)
                },
                onUpload = { observationId, uri ->
                    monitoring.uploadPhoto(observationId, uri, resolver)
                },
                onPreview = { monitoring.preview(it) },
                onThumbnail = { monitoring.thumbnail(it) },
                onDismissPreview = { monitoring.dismissPreview() }, pending = pending,
                onSync = { navController.navigate("sync") },
                onRetry = { monitoring.load(farmId, cycleId) },
                quick = entry.arguments?.getString("quick").orEmpty(),
                onIssueObservation = { health, growth, notes, category, severity, photo ->
                    monitoring.observeIssue(health, growth, notes,
                        category, severity, photo, resolver)
                },
                canManageIssues = manager,
                taskOptions = taskState.tasks,
                onFollowUp = { observation, status, taskId ->
                    monitoring.updateFollowUp(observation, status, taskId)
                },
                onActivityWithStock = { type, notes, item, quantity ->
                    monitoring.logActivityWithStock(type, notes, item, quantity)
                })
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
        composable("insights/{farmId}") { entry ->
            val farmId = entry.arguments?.getString("farmId").orEmpty()
            val insightState by insights.state.collectAsStateWithLifecycle()
            val farmState by farms.state.collectAsStateWithLifecycle()
            LaunchedEffect(farmId) { insights.load(farmId) }
            FarmInsightsScreen(
                state = insightState,
                farmTimeZone = farmState.selected?.takeIf { it.id == farmId }?.timeZoneId,
                onBack = { navController.popBackStack() },
                onReload = { from, to -> insights.load(farmId, from, to) }
            )
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
}
````

## `mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/theme/Theme.kt`

````text
package com.smartfarm.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Harvest Gold + Pine visual system. Keep gold on large surfaces or with a dark-ink label.
internal object AniGoPalette {
    val pine = Color(0xFF153D2D)
    val pineMid = Color(0xFF28503C)
    val harvestGold = Color(0xFFB58A32)
    val goldInk = Color(0xFF80601D)
    val goldSoft = Color(0xFFFFF2D8)
    val cream = Color(0xFFF7F8F4)
}

internal val LightColors = lightColorScheme(
    primary = AniGoPalette.pine, onPrimary = Color.White,
    primaryContainer = Color(0xFFE5F1E8), onPrimaryContainer = Color(0xFF153D2D),
    secondary = AniGoPalette.goldInk, onSecondary = Color.White,
    secondaryContainer = AniGoPalette.goldSoft, onSecondaryContainer = Color(0xFF574016),
    tertiary = Color(0xFF80601D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8BC), onTertiaryContainer = Color(0xFF574016),
    background = AniGoPalette.cream, onBackground = Color(0xFF1B3428),
    surface = Color.White, onSurface = Color(0xFF1B3428),
    surfaceVariant = Color(0xFFEEF0EA), onSurfaceVariant = Color(0xFF506156),
    surfaceTint = AniGoPalette.pine, surfaceDim = Color(0xFFE1E7DF),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFBF8),
    surfaceContainer = Color(0xFFF3F6F0), surfaceContainerHigh = Color(0xFFEBF0E9),
    surfaceContainerHighest = Color(0xFFE5ECE3),
    outline = Color(0xFF728076), outlineVariant = Color(0xFFC5D3C8),
    error = Color(0xFFA10B22), onError = Color.White,
    errorContainer = Color(0xFFFFF0F1), onErrorContainer = Color(0xFF730F1E)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFC6E6CF), onPrimary = AniGoPalette.pine,
    primaryContainer = Color(0xFF244C35), onPrimaryContainer = Color(0xFFDDF4E2),
    secondary = Color(0xFFF4D28B), onSecondary = Color(0xFF3F2D0B),
    secondaryContainer = Color(0xFF493B1C), onSecondaryContainer = Color(0xFFFCE4AC),
    tertiary = Color(0xFFF1BE6B), onTertiary = Color(0xFF34280B),
    tertiaryContainer = Color(0xFF51421F), onTertiaryContainer = Color(0xFFFFF0C9),
    background = Color(0xFF111D16), onBackground = Color(0xFFF2F5ED),
    surface = Color(0xFF1D2C22), onSurface = Color(0xFFF2F5ED),
    surfaceVariant = Color(0xFF34463A), onSurfaceVariant = Color(0xFFC1D0C2),
    surfaceTint = Color(0xFFC6E6CF), surfaceDim = Color(0xFF111D16),
    surfaceBright = Color(0xFF34483A),
    surfaceContainerLowest = Color(0xFF111913), surfaceContainerLow = Color(0xFF19271D),
    surfaceContainer = Color(0xFF26352C), surfaceContainerHigh = Color(0xFF2D4032),
    surfaceContainerHighest = Color(0xFF364A3B),
    outline = Color(0xFF94A79B), outlineVariant = Color(0xFF526457),
    error = Color(0xFFFFB3B5), onError = Color(0xFF680016),
    errorContainer = Color(0xFF8F0025), onErrorContainer = Color(0xFFFFDADE)
)

internal val FarmTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
)

internal val FarmShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun SmartFarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = FarmTypography,
        shapes = FarmShapes,
        content = content
    )
}
````
