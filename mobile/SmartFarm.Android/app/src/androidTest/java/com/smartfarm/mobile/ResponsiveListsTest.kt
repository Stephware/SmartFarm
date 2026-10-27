package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import com.smartfarm.mobile.ui.theme.DarkColors
import com.smartfarm.mobile.features.auth.RegisterScreen
import com.smartfarm.mobile.features.auth.AuthUiState
import com.smartfarm.mobile.data.remote.dto.FarmDto
import com.smartfarm.mobile.features.farms.FarmListScreen
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.features.inventory.InventoryListScreen
import com.smartfarm.mobile.features.inventory.InventoryUiState
import com.smartfarm.mobile.data.remote.dto.InventoryItemDto
import com.smartfarm.mobile.data.remote.dto.InventoryWorkspaceDto
import java.math.BigDecimal
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResponsiveListsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun farmCreationOpensBeforeExistingRecords() {
        val farms = (1..200).map { FarmDto("farm-$it", "Farm $it", "Location", "Asia/Manila", null, null, "Owner") }
        compose.setContent { SmartFarmTheme { FarmListScreen(FarmUiState(farms = farms), {}, { _, _, _, _, _ -> }, {}, {}) } }
        compose.onNodeWithText("Add a farm").performClick()
        compose.onNodeWithText("Name").assertIsDisplayed()
    }

    @Test fun inventoryCreationOpensBeforeExistingRecords() {
        val items = (1..200).map { InventoryItemDto("item-$it", "farm", "Item $it", 1, "kg", null,
            BigDecimal.TEN, null, false, null, "version") }
        val state = InventoryUiState(farmId = "farm", workspace = InventoryWorkspaceDto(items, 0))
        compose.setContent { SmartFarmTheme { InventoryListScreen(state, true, {}, {}, { _, _, _, _, _ -> }) } }
        compose.onNodeWithText("Add stock item").performClick()
        compose.onNodeWithText("Name").assertIsDisplayed()
    }

    @Test fun longFarmListsComposeOnlyNearbyRecordsAndReachTheEnd() {
        val farms = (1..200).map { FarmDto("farm-$it", "Farm $it", "Location", "Asia/Manila", null, null, "Owner") }
        compose.setContent { SmartFarmTheme { FarmListScreen(FarmUiState(farms = farms), {}, { _, _, _, _, _ -> }, {}, {}) } }
        assertTrue(compose.onAllNodesWithTag("record-card").fetchSemanticsNodes().size < 30)
        compose.onNodeWithText("Farm 200").assertDoesNotExist()
        compose.onAllNodesWithTag("record-list")[0].performScrollToIndex(200)
        compose.onNodeWithText("Farm 200").assertIsDisplayed()
    }

    @Test fun narrowDarkRegistrationSupportsLargeText() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(375.dp, 667.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    MaterialTheme(colorScheme = DarkColors) { RegisterScreen(AuthUiState(), { _, _, _, _ -> }, {}) }
                }
            }
        }
        compose.onNodeWithText("Register").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Full name").performScrollTo().assertIsDisplayed()
        savePreview("registration-dark-large-text.png")
    }

    @Test fun landscapeRegistrationKeepsTheSubmitControlReachable() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(667.dp, 375.dp))) {
                SmartFarmTheme { RegisterScreen(AuthUiState(), { _, _, _, _ -> }, {}) }
            }
        }
        compose.onNodeWithText("Register").performScrollTo().assertIsDisplayed()
        savePreview("registration-landscape.png")
    }

    private fun savePreview(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
