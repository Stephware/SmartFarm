package com.smartfarm.mobile

import android.graphics.Bitmap
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.composable
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.features.crops.*
import com.smartfarm.mobile.features.auth.AccountScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.features.farms.*
import com.smartfarm.mobile.features.tasks.TaskUiState
import com.smartfarm.mobile.navigation.FarmWorkspaceBar
import com.smartfarm.mobile.ui.theme.*
import java.io.File
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Visual fixtures are test-only; the application continues to use API state. */
class ReferenceWorkspaceVisualTest {
    @get:Rule val compose = createComposeRule()
    private val names = listOf("Tomato", "Eggplant", "Rice", "Pechay")
    private val plots = names.mapIndexed { i, _ -> PlotDto("plot-$i", "farm", "Plot ${i + 1}", BigDecimal("250"), "version") }
    private val cycles = names.mapIndexed { i, name -> CropCycleDto("crop-$i", "farm", "plot-$i", "Plot ${i + 1}",
        "type-$i", name, 1, "2026-10-10", "2026-10-10", "2026-12-10", "kg", 2, null, null, "version") }

    @Test fun cropGridOpensTheExistingActionsForTheChosenCycle() {
        var monitoring: String? = null
        compose.setContent { SmartFarmTheme {
            CropScreen(CropUiState(farmId = "farm", cycles = cycles), false, {},
                { _, _, _, _ -> }, { _, _, _, _ -> }, { _, _, _ -> },
                { monitoring = it.id }, {}, { _, _, _, _ -> })
        } }
        compose.onNodeWithTag("crop-summary-crop-0").performScrollTo().performClick()
        compose.onNodeWithText("Activities, inspections & photos").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("crop-0", monitoring) }
    }

    @Test fun phoneHomeReference() {
        compose.setContent { SmartFarmTheme { Home() } }
        compose.onNodeWithText("My farm plots").performScrollTo().assertIsDisplayed()
        capture("reference-home-light.png")
    }

    @Test fun phoneHomeDarkReference() {
        compose.setContent { SmartFarmTheme { MaterialTheme(colorScheme = DarkColors) { Home() } } }
        compose.onNodeWithText("My farm plots").performScrollTo().assertIsDisplayed()
        capture("reference-home-dark.png")
    }

    @Test fun cropGridReference() {
        compose.setContent { SmartFarmTheme {
            CropScreen(CropUiState(farmId = "farm", cycles = cycles), false, {},
                { _, _, _, _ -> }, { _, _, _, _ -> }, { _, _, _ -> }, {}, {}, { _, _, _, _ -> })
        } }
        compose.onNodeWithTag("crop-summary-crop-0").performScrollTo().assertIsDisplayed()
        capture("reference-crops-light.png")
    }

    @Test fun accountAppearanceChangesAndPersistsTheSelectedTheme() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("appearance", android.content.Context.MODE_PRIVATE)
        val previous = preferences.getBoolean("dark", false)
        preferences.edit().putBoolean("dark", false).commit()
        try {
            compose.setContent { SmartFarmTheme {
                val navigation = androidx.navigation.compose.rememberNavController()
                androidx.navigation.compose.NavHost(navigation, startDestination = "account") {
                    composable("account") { AccountScreen(AuthUiState(), {}, {}, {}) }
                }
            } }
            compose.onNodeWithContentDescription("Dark theme").performScrollTo().performClick()
            compose.waitUntil { preferences.getBoolean("dark", false) }
            compose.onNodeWithContentDescription("Dark theme").assertIsOn()
            compose.onNodeWithContentDescription("Dark theme").performClick()
            compose.waitUntil { !preferences.getBoolean("dark", true) }
            compose.onNodeWithContentDescription("Dark theme").assertIsOff()
        } finally {
            preferences.edit().putBoolean("dark", previous).commit()
        }
    }

    @Composable private fun Home() {
        Scaffold(bottomBar = { FarmWorkspaceBar("farm/{farmId}") {} }) { padding ->
            androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                FarmDetailScreen(FarmUiState(selected = FarmDto("farm", "Green Valley Farm", "Laguna, Philippines",
                    "Asia/Manila", role = "Manager"), plots = plots), {}, { _, _, _, _, _ -> }, {},
                    { _, _ -> }, { _, _, _ -> }, {}, {}, {}, { _, _ -> }, { _, _ -> }, {},
                    homeTasks = TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila"),
                    homeCrops = CropUiState(farmId = "farm", cycles = cycles), actorId = "manager")
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
