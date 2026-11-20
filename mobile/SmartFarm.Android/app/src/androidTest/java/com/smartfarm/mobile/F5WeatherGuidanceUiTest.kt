package com.smartfarm.mobile

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.smartfarm.mobile.data.remote.dto.GuidanceMessageDto
import com.smartfarm.mobile.data.remote.dto.WeatherAdvisoryDto
import com.smartfarm.mobile.data.remote.dto.WeatherGuidanceDto
import com.smartfarm.mobile.data.remote.dto.WeatherOverviewDto
import com.smartfarm.mobile.features.farms.FarmUiState
import com.smartfarm.mobile.features.farms.FarmWeatherDetails
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Rule
import org.junit.Test

class F5WeatherGuidanceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun activeAdvisoryGuidanceIsOnDemandAndDisplaysSafeFallback() {
        val id = "b8792926-3b0f-4dc8-9433-68ca8199a2d5"
        val overview = WeatherOverviewDto(
            farmId = "77fba155-e043-42b2-aa33-e501da462e27",
            hasCoordinates = true, timeZoneId = "Asia/Manila",
            days = emptyList(),
            advisories = listOf(WeatherAdvisoryDto(id, "2026-11-20", 1, 3, 0,
                "Saved forecast advisory", "2026-11-20T02:00:00Z"))
        )
        var requested: String? = null
        val state = FarmUiState(
            weather = overview,
            guidanceAdvisoryId = id,
            weatherGuidance = WeatherGuidanceDto(
                advisoryId = id, cropCycleId = null, language = "en",
                severity = 3, forecastDate = "2026-11-20",
                weatherProvider = "OpenMeteo", retrievedAtUtc = "2026-11-20T02:00:00Z",
                isStale = true, isSynthetic = false,
                generationSource = "deterministic", model = null,
                explanations = listOf(GuidanceMessageDto("rain.roots",
                    "Forecast heavy rain may leave soil waterlogged and affect roots. Check actual field conditions before deciding what to do.")),
                recommendations = listOf(GuidanceMessageDto("drainage.check",
                    "Check drainage and areas that may collect water when it is safe to do so.")),
                limitations = listOf("weather_stale_or_unavailable")
            )
        )
        compose.setContent {
            SmartFarmTheme {
                FarmWeatherDetails(state, onGuidance = { advisoryId, crop ->
                    requested = advisoryId
                    check(crop == null)
                })
            }
        }
        compose.onNodeWithText("Rule-based guidance", substring = true).assertExists()
        compose.onNodeWithText("The forecast is old.", substring = true).assertExists()
        compose.onNodeWithText("Advice only.", substring = true).assertExists()
        compose.onNodeWithText("Explain this advisory").performClick()
        compose.onNodeWithText("General farm guidance").performClick()
        compose.runOnIdle { check(requested == id) }
    }
}
