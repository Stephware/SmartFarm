package com.smartfarm.mobile

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.features.auth.*
import com.smartfarm.mobile.features.crops.*
import com.smartfarm.mobile.features.farms.*
import com.smartfarm.mobile.features.harvests.*
import com.smartfarm.mobile.features.inventory.*
import com.smartfarm.mobile.features.monitoring.*
import com.smartfarm.mobile.features.syncstatus.SyncStatusScreen
import com.smartfarm.mobile.features.tasks.*
import com.smartfarm.mobile.navigation.*
import com.smartfarm.mobile.ui.components.*
import com.smartfarm.mobile.ui.theme.*
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

/** Synthetic models for native layout review only; production screens still use real API state. */
class Phase3VisualReviewTest {
    @get:Rule val compose = createComposeRule()
    private val farm = FarmDto("farm", "San Fernando community vegetable farm", "Pampanga, Philippines", "Asia/Manila", role = "Owner")
    private val plot = PlotDto("plot", "farm", "North growing area", BigDecimal("250"), "version")
    private val crop = CropCycleDto("crop", "farm", "plot", plot.name, "type", "Tomato and sweet pepper", 1,
        "2026-10-01", "2026-10-01", "2026-12-01", "kg", 3, null, null, "version")
    private val crops = CropUiState(farmId = "farm", cycles = listOf(crop))
    private val weather = WeatherOverviewDto("farm", true, "Asia/Manila", listOf(WeatherDayDto(
        LocalDate.now(ZoneId.of("Asia/Manila")).toString(), BigDecimal("24"), BigDecimal("31"), BigDecimal("5"),
        Instant.now().toString(), "Test forecast provider", BigDecimal("65"), BigDecimal("12"))), emptyList())
    private val farmState = FarmUiState(selected = farm, plots = listOf(plot), weather = weather)

    @Test fun affectedScreensRenderInBothThemesAndAdaptiveLayouts() {
        data class Variant(val name: String, val size: DpSize, val font: Float)
        val variants = listOf(
            Variant("phone", DpSize(390.dp, 844.dp), 1f),
            Variant("small-large-text", DpSize(320.dp, 720.dp), 1.6f),
            Variant("tablet", DpSize(800.dp, 1000.dp), 1f),
            Variant("landscape", DpSize(720.dp, 390.dp), 1.3f))
        val screens = listOf("welcome", "login", "register", "farms", "setup", "today",
            "my-farm", "record-menu", "crops", "tasks", "water", "supplies", "inspect", "harvest", "weather", "sync", "inventory", "weather-expanded", "tasks-help", "harvest-history", "farms-create")
        var screen by mutableStateOf(screens.first())
        var variant by mutableStateOf(variants.first())
        var dark by mutableStateOf(false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE)
        val previous = preferences.getBoolean("dark", false)
        compose.setContent {
            val current = LocalConfiguration.current
            val configuration = remember(variant, current) {
                Configuration(current).apply {
                    screenWidthDp = variant.size.width.value.toInt()
                    screenHeightDp = variant.size.height.value.toInt()
                    smallestScreenWidthDp = minOf(screenWidthDp, screenHeightDp)
                }
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(variant.size)) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(variant.font)) {
                        key(variant.name, dark) {
                            SmartFarmTheme { key(screen) { Screen(screen) } }
                        }
                    }
                }
            }
        }
        val output = File(context.filesDir, "phase3-review").apply { mkdirs() }
        try { for (v in variants) for (mode in listOf(false, true)) for (name in screens) {
            preferences.edit().putBoolean("dark", mode).commit()
            compose.runOnIdle { variant = v; dark = mode; screen = name }
            compose.waitForIdle()
            when (name) {
                "weather-expanded" -> compose.onNodeWithText("Show daily forecast").performScrollTo().performClick()
                "tasks-help" -> compose.onNodeWithText("About task updates").performScrollTo().performClick()
                "harvest-history" -> compose.onNodeWithText("History & totals").performScrollTo().performClick()
                "farms-create" -> compose.onNodeWithText("Add a farm").performScrollTo().performClick()
            }
            compose.waitForIdle()
            val file = File(output, "$name-${if (mode) "dark" else "light"}-${v.name}.png")
            // Native screenshot includes system windows and insets, including the record sheet.
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
                requireNotNull(bitmap) { "Native screenshot unavailable" }
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        } } finally { preferences.edit().putBoolean("dark", previous).commit() }
    }

    @Composable private fun Screen(name: String) {
        val authenticated = name !in listOf("welcome", "login", "register")
        CompositionLocalProvider(LocalFarmHeaderNavigation provides if (authenticated)
            FarmHeaderNavigation(farm.name, {}, {}) else null) {
            if (name in listOf("today", "my-farm", "record-menu")) {
                val expanded = LocalConfiguration.current.screenWidthDp >= 600
                val route = if (name == "today") "farm/{farmId}" else "myfarm/{farmId}"
                Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), bottomBar = {
                    if (!expanded) FarmWorkspaceBar(route) {}
                }) { padding ->
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        if (expanded) FarmWorkspaceBar(route, expanded = true) {}
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) { SurfaceContent(name) }
                    }
                }
            } else SurfaceContent(name)
        }
    }

    @Composable private fun SurfaceContent(name: String) {
        when (name) {
            "welcome" -> WelcomeScreen(onLogin = {}, onRegister = {})
            "login" -> LoginScreen(AuthUiState(), { _, _ -> }, {})
            "register" -> RegisterScreen(AuthUiState(), { _, _, _ -> }, {})
            "farms", "farms-create" -> FarmListScreen(FarmUiState(farms = listOf(farm)), {}, { _, _, _, _, _ -> }, {}, {})
            "setup" -> RecordPage { item { FarmSetupGuide("farm", false, null, {}, {}) } }
            "today" -> FarmDetailScreen(farmState, {}, { _, _, _, _, _ -> }, {}, { _, _ -> },
                { _, _, _ -> }, {}, {}, {}, { _, _ -> }, { _, _ -> }, {},
                homeTasks = TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila"),
                homeCrops = crops, actorId = "owner")
            "my-farm" -> FarmMoreScreen(farm.name, "FieldWorker", {}, pendingSync = 120, reviewSync = 110)
            "record-menu" -> {
                FarmMoreScreen(farm.name, "Owner", {})
                FarmQuickRecordSheet("farm", farm.name, crops, {}, { _, _ -> }, {}, {})
            }
            "crops" -> CropScreen(crops, true, {}, { _, _, _, _ -> }, { _, _, _, _ -> },
                { _, _, _ -> }, {}, {}, { _, _, _, _ -> })
            "tasks", "tasks-help" -> TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila"), true,
                "owner", {}, {}, {}, {})
            "water", "supplies", "inspect" -> MonitoringScreen(
                MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true), {}, { _, _ -> },
                { _, _, _, _ -> }, { _, _ -> }, {}, { byteArrayOf() }, {},
                quick = when (name) { "water" -> "watering"; "supplies" -> "fertilizing"; else -> "inspection" },
                cropLabel = "Tomato · North growing area")
            "harvest", "harvest-history" -> HarvestScreen(HarvestUiState(farmId = "farm", cycleId = "crop",
                overview = HarvestOverviewDto("farm", "crop", crop.cropTypeName, plot.name, "kg", 1,
                    true, true, BigDecimal("3.750"), 2, 0, emptyList())), {}, { _, _ -> }, { _, _ -> })
            "weather", "weather-expanded" -> FarmDetailScreen(farmState, {}, { _, _, _, _, _ -> }, {},
                { _, _ -> }, { _, _, _ -> }, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, section = "Weather")
            "sync" -> SyncStatusScreen(emptyList(), emptyList(), null, null, {}, {}, {}, {}, {}, {}, {}, {}, {})
            "inventory" -> InventoryListScreen(InventoryUiState(farmId = "farm",
                workspace = InventoryWorkspaceDto(emptyList(), 0)), true, {}, {}, { _, _, _, _, _ -> })
        }
    }
}
