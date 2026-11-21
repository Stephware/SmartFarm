package com.smartfarm.mobile

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.features.auth.*
import com.smartfarm.mobile.features.monitoring.*
import com.smartfarm.mobile.data.remote.dto.AccountResponse
import com.smartfarm.mobile.ui.components.*
import com.smartfarm.mobile.ui.theme.*
import org.junit.Rule
import org.junit.Test

/** Actual production Composables, synthetic records; no network or business writes. */
@OptIn(ExperimentalTestApi::class)
class BackNavigationVisualTest {
    @get:Rule val compose = createComposeRule()
    @Test fun pinnedHeadersRemainReadableInBothThemesAtPhoneAndTabletSizes() {
        var dark by mutableStateOf(false)
        var width by mutableStateOf(320)
        var screen by mutableStateOf("monitor")
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(if (width == 320) 1.6f else 1f)) {
                    SmartFarmTheme {
                        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
                            val exit = rememberExitGuard()
                            CompositionLocalProvider(
                                LocalFarmAppearance provides FarmAppearance(dark) {},
                                LocalExitGuard provides exit,
                                LocalPageNavigation provides PageNavigation(when (screen) {
                                    "login" -> "Log in"; "account" -> "My account"; else -> "Record inspection"
                                }, "Back to crops", {}),
                                LocalFarmHeaderNavigation provides FarmHeaderNavigation("Green Valley Farm", {}, {}, 3, 1, {})
                            ) {
                                when (screen) {
                                    "login" -> LoginScreen(AuthUiState(), { _, _ -> }, {})
                                    "account" -> AccountScreen(AuthUiState(account = AccountResponse("fixture", "Test farmer", "farmer@example.test")), {}, {}, {}, pendingSyncCount = 3)
                                    else -> MonitoringScreen(MonitoringUiState(farmId = "farm", cycleId = "crop", canRecord = true),
                                        onBack = {}, onActivity = { _, _ -> }, onObservation = { _, _, _, _ -> },
                                        onUpload = { _, _ -> }, onPreview = {}, onThumbnail = { byteArrayOf() }, onDismissPreview = {},
                                        quick = "inspection", cropLabel = "Rice · North plot")
                                }
                            }
                        }
                    }
                }
            }
        }
        for (theme in listOf(false, true)) for (size in listOf(320, 768)) for (page in listOf("monitor", "account", "login")) {
            compose.runOnIdle { dark = theme; width = size; screen = page }
            compose.onNodeWithTag("page-back").assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            capture("$page-$size-${if (theme) "dark" else "light"}")
        }
    }
    private fun capture(name: String) {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val folder = Environment.DIRECTORY_PICTURES + "/AniGoNavigationAudit/"
        resolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?", arrayOf("$name.png", folder))
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
        }))
        resolver.openOutputStream(uri)!!.use { output ->
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
    }
}
