package com.smartfarm.mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartfarm.mobile.ui.components.*
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class UnsavedNavigationTest {
    @get:Rule val compose = createComposeRule()
    @Test fun defaultsAreCleanButEditedTextRequiresExplicitDiscard() {
        var left = 0
        compose.setContent { SmartFarmTheme {
            val exit = rememberExitGuard()
            CompositionLocalProvider(LocalExitGuard provides exit) {
                var text by remember { mutableStateOf("") }
                UnsavedChanges("form", text.isNotEmpty())
                Column {
                    OutlinedTextField(text, { text = it }, label = { Text("Notes") })
                    Button(onClick = { exit.request { left++ } }) { Text("Leave") }
                }
                DiscardChangesDialog(exit)
            }
        } }
        compose.onNodeWithText("Leave").performClick()
        compose.runOnIdle { assertEquals(1, left) }
        compose.onNodeWithText("Notes").performTextInput("Field concern")
        compose.onNodeWithText("Leave").performClick()
        compose.onNodeWithText("Discard unsaved changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Field concern").assertExists()
        compose.runOnIdle { assertEquals(1, left) }
        compose.onNodeWithText("Leave").performClick()
        compose.onNodeWithText("Discard changes").performClick()
        compose.runOnIdle { assertEquals(2, left) }
    }
    @Test fun confirmedSaveIsCleanAndAChangedDraftSurvivesRestoration() {
        var leave = 0
        var save: () -> Unit = {}
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent { SmartFarmTheme {
            val exit = rememberExitGuard()
            CompositionLocalProvider(LocalExitGuard provides exit) {
                var text by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
                UnsavedChanges("form", text.isNotEmpty(), text)
                SideEffect { save = exit.acceptAfterSave("form") }
                Column {
                    OutlinedTextField(text, { text = it }, label = { Text("Notes") })
                    Button(onClick = { exit.request { leave++ } }) { Text("Leave") }
                }
                DiscardChangesDialog(exit)
            }
        } }
        compose.onNodeWithText("Notes").performTextInput("Saved on phone")
        compose.runOnIdle { save() }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Leave").performClick()
        compose.runOnIdle { assertEquals(1, leave) }
        compose.onNodeWithText("Notes").performTextReplacement("New unfinished work")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Leave").performClick()
        compose.onNodeWithText("Discard unsaved changes?").assertIsDisplayed()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("New unfinished work").assertExists()
        compose.runOnIdle { assertEquals(1, leave) }
    }

}
