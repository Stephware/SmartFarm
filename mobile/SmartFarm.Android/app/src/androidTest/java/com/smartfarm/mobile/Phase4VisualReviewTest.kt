package com.smartfarm.mobile

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.data.remote.api.NotificationDto
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.features.crops.*
import com.smartfarm.mobile.features.inventory.*
import com.smartfarm.mobile.features.tasks.*
import com.smartfarm.mobile.ui.components.*
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

/** Real native components with synthetic, test-only data; no production records or credentials. */
class Phase4VisualReviewTest {
    @get:Rule val compose = createComposeRule()
    private val supplies = (1..100).map { index -> InventoryItemDto("item$index", "farm",
        "Supply $index · Rice seed and seasonal field materials", 1, "sacks (50 kg)", null,
        if (index == 1) BigDecimal("999999999999999.999") else BigDecimal("12.500"),
        BigDecimal("20"), index > 1, null, "v") }
    private val cycles = (1..100).map { index -> CropCycleDto("crop$index", "farm", "plot$index",
        "Growing area $index", "rice", "Rice planting $index", 1, "2026-10-01", "2026-10-02",
        "2026-12-01", "kg", 3, null, null, "v") }
    private val tasks get() = (1..100).map { index -> TaskDto("task$index", "farm", "crop$index",
        "farmer", 2, LocalDate.now(ZoneId.of("Asia/Manila")).toString(), 0,
        "Water growing area $index and check drainage", "v") }
    private val notices = (1..100).map { index -> NotificationDto("notice$index",
        "Farm reminder $index", "Review saved fieldwork and check the growing area's condition.",
        Instant.now().toString(), null) }

    @Test fun dataStatesRemainReadableAcrossRolesThemesAndSizes() {
        data class Variant(val name: String, val size: DpSize, val font: Float)
        val variants = listOf(Variant("phone", DpSize(390.dp, 844.dp), 1f),
            Variant("small-text160", DpSize(320.dp, 720.dp), 1.6f),
            Variant("tablet", DpSize(800.dp, 1000.dp), 1f),
            Variant("landscape", DpSize(720.dp, 390.dp), 1.3f))
        val scenes = listOf("supplies-loading", "supplies-error", "supplies-empty", "supplies-cached-empty",
            "supplies-cached-populated", "supplies-populated", "crops-loading", "crops-error", "crops-empty",
            "crops-cached-empty", "crops-populated", "tasks-loading", "tasks-error", "tasks-empty",
            "tasks-populated", "tasks-refresh-error", "notifications-loading", "notifications-error",
            "notifications-empty", "notifications-populated")
        var variant by mutableStateOf(variants.first())
        var scene by mutableStateOf(scenes.first())
        var role by mutableStateOf("Owner")
        var dark by mutableStateOf(false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE)
        val previous = preferences.getBoolean("dark", false)
        val output = File(context.filesDir, "phase4-review").apply { mkdirs() }
        compose.setContent {
            val current = LocalConfiguration.current
            val configuration = remember(variant, current) { Configuration(current).apply {
                screenWidthDp = variant.size.width.value.toInt()
                screenHeightDp = variant.size.height.value.toInt()
                smallestScreenWidthDp = minOf(screenWidthDp, screenHeightDp)
            } }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalFarmHeaderNavigation provides FarmHeaderNavigation("San Fernando community farm", {}, {})) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(variant.size)) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(variant.font)) {
                        key(variant.name, dark, role, scene) { SmartFarmTheme { Scene(scene, role) } }
                    }
                }
            }
        }
        fun capture(name: String) {
            compose.waitForIdle()
            val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            File(output, name + ".png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        try {
            for (v in variants) for (mode in listOf(false, true))
            for (actorRole in listOf("Owner", "Manager", "FieldWorker")) for (name in scenes) {
                preferences.edit().putBoolean("dark", mode).commit()
                compose.runOnIdle { variant = v; dark = mode; role = actorRole; scene = name }
                compose.waitForIdle()
                val file = "$name-$actorRole-${if (mode) "dark" else "light"}-${v.name}"
                capture(file)
                if (name == "supplies-populated" || name == "crops-populated") {
                    val last = if (name == "supplies-populated") inventoryVisibleItems(supplies, "all", "").last().name
                        else cycles.sortedWith(compareBy<CropCycleDto>({ it.plotName }, { it.cropTypeName })).last().cropTypeName
                    if (name == "supplies-populated") {
                        compose.onNodeWithTag("record-list").performScrollToIndex(1)
                        compose.onNodeWithText(inventoryVisibleItems(supplies, "all", "").first().name).assertIsDisplayed()
                        capture(file + "-first")
                    }
                    compose.onNodeWithTag("record-list").performScrollToIndex(100)
                    compose.onNodeWithTag("record-list").performScrollToNode(hasText(last))
                    compose.onNodeWithText(last).assertIsDisplayed()
                    capture(file + "-last")
                }
            }
        } finally { preferences.edit().putBoolean("dark", previous).commit() }
    }

    @Composable private fun Scene(name: String, role: String) {
        val busy = name.endsWith("loading")
        val failed = name.endsWith("error")
        val cached = name.contains("cached")
        val populated = name.endsWith("populated") || name == "tasks-refresh-error"
        val saved = if (cached) Instant.now().minusSeconds(3600).toEpochMilli() else null
        val error = if (failed) "Could not load current records. Check your connection and try again." else null
        val canManage = role != "FieldWorker"
        when {
            name.startsWith("supplies") -> InventoryListScreen(InventoryUiState(farmId = "farm", busy = busy,
                error = error, workspace = InventoryWorkspaceDto(if (populated) supplies else emptyList(),
                    if (populated) 99 else 0), offline = cached, cachedAtUtcMillis = saved),
                canManage, {}, {}, { _, _, _, _, _ -> })
            name.startsWith("crops") -> CropScreen(CropUiState(farmId = "farm", busy = busy, error = error,
                cycles = if (populated) cycles else emptyList(), offline = cached, cachedAtUtcMillis = saved),
                canManage, {}, { _, _, _, _ -> }, { _, _, _, _ -> }, { _, _, _ -> }, {}, {}, { _, _, _, _ -> })
            else -> TaskScreen(TaskUiState(farmId = if (name.startsWith("tasks")) "farm" else null,
                busy = busy, error = error, timeZoneId = "Asia/Manila", tasks = if (populated && name.startsWith("tasks")) tasks else emptyList(),
                notifications = if (populated && name.startsWith("notifications")) notices else emptyList()),
                canManage, "farmer", {}, {}, {}, {}, isOwner = role == "Owner")
        }
    }
}
