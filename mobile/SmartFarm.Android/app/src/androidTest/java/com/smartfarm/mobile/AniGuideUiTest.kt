package com.smartfarm.mobile

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.features.farms.FarmSetupGuide
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AniGuideUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun laterPersistsPerActorFarmAndReplayReopensWithoutMutation() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("ani_guide_v1", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        var opened = 0
        var actor by mutableStateOf("owner-one")
        compose.setContent { SmartFarmTheme { FarmSetupGuide("new-farm", false, null, { opened++ }, {}, actor, {}) } }
        compose.onAllNodesWithText("Add your first plot").assertCountEquals(2)
        compose.onNodeWithText("Not now").performClick()
        assertTrue(prefs.getBoolean("owner-one:new-farm:AddPlot", false))
        assertEquals(0, opened)
        compose.onNodeWithText("Help from Ani").performClick()
        compose.onNode(hasText("Add your first plot") and hasClickAction()).performClick()
        assertEquals(1, opened)
        compose.runOnIdle { actor = "owner-two" }
        compose.onNodeWithText("Not now").assertIsDisplayed()
        assertFalse(prefs.getBoolean("owner-two:new-farm:AddPlot", false))
        prefs.edit().clear().commit()
    }
}
