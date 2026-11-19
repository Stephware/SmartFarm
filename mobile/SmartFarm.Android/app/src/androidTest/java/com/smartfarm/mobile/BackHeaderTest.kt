package com.smartfarm.mobile

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class BackHeaderTest {
    @get:Rule val compose = createComposeRule()
    @Test fun secondaryBackStaysPinnedWhileTheListScrolls() {
        var returned = false
        compose.setContent { SmartFarmTheme {
            RecordPage(title = "Supplies", onBack = { returned = true }, backLabel = "Back to My Farm") {
                items((1..40).toList()) { Text("Supply $it") }
            }
        } }
        val back = compose.onNodeWithContentDescription("Back to My Farm")
        back.assertIsDisplayed().assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("Supply 40"))
        back.assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(returned) }
    }
    @Test fun rootListDoesNotInventABackArrow() {
        compose.setContent { SmartFarmTheme { RecordPage { item { Text("My farms") } } } }
        compose.onNodeWithTag("page-back").assertDoesNotExist()
    }
}
