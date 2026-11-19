package com.smartfarm.mobile

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/** Follow first-run navigation without pre-setting or clearing the completion preference. */
internal fun ComposeContentTestRule.skipFirstRunGuideIfShown() {
    waitForIdle()
    if (onAllNodesWithText("Skip intro").fetchSemanticsNodes().isNotEmpty()) {
        onNodeWithText("Skip intro").performClick()
        waitForIdle()
    }
    val exitText = when {
        onAllNodesWithText("Skip").fetchSemanticsNodes().isNotEmpty() -> "Skip"
        onAllNodesWithText("Close guide").fetchSemanticsNodes().isNotEmpty() -> "Close guide"
        else -> null
    }
    if (exitText != null) {
        onNodeWithText(exitText).performClick()
        waitForIdle()
    }
}
