package com.smartfarm.mobile

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.smartfarm.mobile.data.remote.dto.*
import com.smartfarm.mobile.features.farms.*
import com.smartfarm.mobile.features.harvests.*
import com.smartfarm.mobile.features.inventory.*
import com.smartfarm.mobile.features.tasks.*
import com.smartfarm.mobile.features.crops.*
import com.smartfarm.mobile.data.remote.api.TaskDto
import com.smartfarm.mobile.ui.theme.SmartFarmTheme
import com.smartfarm.mobile.ui.components.DateInput
import com.smartfarm.mobile.ui.components.RecordPage
import com.smartfarm.mobile.ui.components.FarmSectionCard
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real form components and callbacks; all records here are synthetic and test-only. */
class Phase5FormWorkflowTest {
    @get:Rule val compose = createComposeRule()
    private var hideKeyboard: () -> Unit = {}
    private var view: android.view.View? = null
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val appearance = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val wasDark = appearance.getBoolean("dark", false)
    @After fun restoreAppearance() { appearance.edit().putBoolean("dark", wasDark).commit() }
    private val crop = CropCycleDto("cycle", "farm", "plot", "North field", "rice", "Rice", 1,
        "2026-11-01", "2026-11-02", "2027-01-01", "kg", 2, null, null, "v")

    private fun content(dark: Boolean = false, block: @Composable () -> Unit) {
        appearance.edit().putBoolean("dark", dark).commit()
        compose.setContent {
            val keyboard = LocalSoftwareKeyboardController.current
            val localView = LocalView.current
            SideEffect { hideKeyboard = { keyboard?.hide() }; view = localView }
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 720.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(1.6f)) {
                    SmartFarmTheme { block() }
                }
            }
        }
    }

    private fun field(label: String): SemanticsNodeInteraction {
        val matcher = hasText(label) and hasSetTextAction()
        compose.onNodeWithTag("record-list").performScrollToNode(matcher)
        return compose.onNode(matcher).performScrollTo()
    }

    private fun action(label: String): SemanticsNodeInteraction {
        compose.runOnIdle { hideKeyboard() }
        compose.waitUntil(5_000) { view?.let { ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime()) } == false }
        val matcher = hasText(label) and hasClickAction()
        compose.onNodeWithTag("record-list").performScrollToNode(matcher)
        val target = compose.onNode(matcher).performScrollTo()
        // A form is one tall lazy item: use real swipes if the descendant is still outside the viewport.
        for (attempt in 0 until 6) {
            if (target.isDisplayed()) break
            compose.onNodeWithTag("record-list").performTouchInput { swipeUp() }
        }
        return target.assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(context.filesDir, "phase5-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun supplyValidationRecoversWithoutLosingInputsOrSendingTwice() {
        val state = mutableStateOf(InventoryUiState(farmId = "farm"))
        var sent: List<String>? = null
        var calls = 0
        content { InventoryListScreen(state.value, true, {}, {}, { name, _, unit, opening, reorder ->
            calls++; sent = listOf(name, unit, opening, reorder); state.value = state.value.copy(busy = true)
        }) }
        action("Add supply").performClick()
        field("Supply name").performTextInput(" Rice seed ")
        field("Unit (kg, L, pcs)").performTextReplacement(" kg ")
        field("Opening quantity").performTextReplacement("1.0001")
        action("Create supply").assertIsNotEnabled()
        compose.onNodeWithText("Enter a nonnegative quantity with up to 3 decimal places.").assertExists()
        capture("supplies-invalid-small-large-text")
        field("Opening quantity").performTextReplacement(" 1.500 ")
        field("Low-stock threshold (optional)").performTextReplacement("-1")
        action("Create supply").assertIsNotEnabled()
        field("Low-stock threshold (optional)").performTextReplacement(" 2.000 ")
        action("Create supply").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls); assertEquals(listOf("Rice seed", "kg", "1.500", "2.000"), sent) }
        action("Create supply").assertIsNotEnabled()
    }

    @Test fun harvestNotesStayCompactAndRecoverToOnePartialHarvest() {
        val state = mutableStateOf(HarvestUiState(farmId = "farm", cycleId = "cycle",
            overview = HarvestOverviewDto("farm", "cycle", "Tomato", "North plot", "kg", 1, true, false,
                BigDecimal("2"), 1, 0, emptyList())))
        var sent: Pair<String, String>? = null
        var calls = 0
        content(dark = true) { HarvestScreen(state.value, {}, { quantity, note ->
            calls++; sent = quantity to note; state.value = state.value.copy(busy = true)
        }, { _, _ -> }) }
        field("Harvested quantity (kg)").performTextInput("1.500")
        val notes = field("Notes (optional)")
        notes.performTextInput("Harvest notes ".repeat(78))
        val density = context.resources.displayMetrics.density
        assertTrue("Long notes must scroll inside a compact field", notes.fetchSemanticsNode().size.height <= 250 * density)
        action("Save harvested amount").assertIsNotEnabled()
        compose.onNodeWithText("Harvest notes must be 1,000 characters or fewer.").assertExists()
        capture("harvest-invalid-dark-small-large-text")
        field("Notes (optional)").performTextReplacement("First picking")
        action("Save harvested amount").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls); assertEquals("1.500" to "First picking", sent) }
        action("Save harvested amount").assertIsNotEnabled()
        // Confirmed totals are a separate lazy item from the harvest form.
        compose.onNodeWithTag("record-list").performScrollToNode(hasText("2 kg"))
        compose.onNodeWithText("2 kg").assertExists()
    }

    @Test fun taskInstructionsStayCompactAndKeepTheExistingCreateContract() {
        val state = mutableStateOf(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila", choicesReady = true))
        var sent: String? = null
        var calls = 0
        content { TaskScreen(state.value, true, "manager", {}, {}, {}, {}, onCreate = {
            calls++; sent = it.description; state.value = state.value.copy(busy = true)
        }) }
        action("Create and assign task").performClick()
        val notes = field("Instructions (optional)")
        notes.performTextReplacement("Check drainage ".repeat(36))
        assertTrue("Long instructions must not make an oversized form", notes.fetchSemanticsNode().size.height <= 250 * context.resources.displayMetrics.density)
        action("Create task").assertIsNotEnabled()
        compose.onNodeWithText("Instructions must be 500 characters or fewer.").assertExists()
        capture("tasks-invalid-small-large-text")
        field("Instructions (optional)").performTextReplacement(" Check drainage ")
        action("Create task").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls); assertEquals("Check drainage", sent) }
        action("Create task").assertIsNotEnabled()
    }

    @Test fun busyTaskFormDoesNotAllowChangingTheSubmittedSelection() {
        val state = mutableStateOf(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila", choicesReady = true))
        content { TaskScreen(state.value, true, "manager", {}, {}, {}, {}) }
        action("Create and assign task").performClick()
        field("Instructions (optional)").performTextReplacement("Keep this unsent draft")
        compose.runOnIdle { state.value = state.value.copy(busy = true) }
        action("Other task types").assertIsNotEnabled()
        action("No crop cycle").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(busy = false, error = "Could not save task.", choicesReady = false) }
        field("Instructions (optional)").assertTextContains("Keep this unsent draft")
        action("Create task").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(error = null, choicesReady = true) }
        action("Create task").assertIsEnabled()
        capture("task-draft-after-retry")
    }

    @Test fun dateAndChooseActionDoNotOverlapAtLargeText() {
        content { RecordPage { item { FarmSectionCard("New task") {
            DateInput("2026-11-16", "Due date", {})
        } } } }
        val label = LocalDate.of(2026, 11, 16).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        val date = compose.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val choose = compose.onNodeWithText("Choose date", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val button = compose.onNodeWithContentDescription("Due date:", substring = true).getUnclippedBoundsInRoot()
        assertTrue("Date/action need separate space and must fit the button: $date / $choose / $button",
            (date.right + 8.dp <= choose.left || date.bottom + 4.dp <= choose.top) &&
                date.right <= button.right && choose.right <= button.right)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals("The date must fit one readable line in this narrow layout", 1, layouts.single().lineCount)
        capture("date-input-small-large-text")
    }

    @Test fun unavailableNewTaskSelectionsNeedExplicitReviewWithoutLosingTheDraft() {
        val state = mutableStateOf(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila", choicesReady = true,
            cycles = listOf(crop), members = listOf(FarmMemberDto("member", "worker", "Sam Farmer", "sam@example.test", "FieldWorker"))))
        var calls = 0
        content { TaskScreen(state.value, true, "manager", {}, {}, {}, {}, onCreate = {
            calls++; assertNull(it.cropCycleId); assertNull(it.assignedToUserId)
        }) }
        action("Create and assign task").performClick()
        action("No crop cycle").performClick()
        compose.onNodeWithText("Rice · North field").performClick()
        action("Unassigned (owner receives reminder)").performClick()
        compose.onNodeWithText("Sam Farmer", substring = true).performClick()
        field("Instructions (optional)").performTextReplacement("Keep this crop-specific work")
        compose.runOnIdle { state.value = state.value.copy(cycles = listOf(crop.copy(status = 2)), members = emptyList()) }
        action("Create task").assertIsNotEnabled()
        action("Selected crop not listed").performClick()
        compose.onNodeWithText("No crop cycle").performClick()
        action("Person not in current choices").performClick()
        compose.onNodeWithText("Unassigned (owner receives reminder)").performClick()
        field("Instructions (optional)").assertTextContains("Keep this crop-specific work")
        action("Create task").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun existingOwnerAssignmentStaysValidAndInstructionsCannotChangeDuringSave() {
        val task = TaskDto("task", "farm", null, "owner", 4, "2026-11-20", 0, "Check drainage", "version")
        val state = mutableStateOf(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila", choicesReady = true, tasks = listOf(task)))
        var calls = 0
        content { TaskScreen(state.value, true, "owner", {}, {}, {}, {}, isOwner = true, onUpdate = { _, input ->
            calls++; assertEquals("owner", input.assignedToUserId); assertEquals("version", input.rowVersion)
            assertEquals("Keep this edit", input.description); state.value = state.value.copy(busy = true)
        }) }
        action("All farm tasks").performClick()
        action("Edit assignment").performClick()
        field("Instructions").performTextReplacement("Keep this edit")
        action("Save changes").assertIsEnabled().performClick()
        compose.onNodeWithText("Instructions").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(1, calls); state.value = state.value.copy(busy = false, error = "Could not save task.") }
        field("Instructions").assertTextContains("Keep this edit")
        action("Save changes").assertIsEnabled()
        capture("task-edit-draft-after-error")
    }

    @Test fun editedTaskExplainsOverlongInstructionsAndKeepsThemCompact() {
        val task = TaskDto("task", "farm", null, null, 4, "2026-11-20", 0, "Check drainage", "version")
        content { TaskScreen(TaskUiState(farmId = "farm", timeZoneId = "Asia/Manila", choicesReady = true, tasks = listOf(task)),
            true, "manager", {}, {}, {}, {}) }
        action("All farm tasks").performClick()
        action("Edit assignment").performClick()
        val input = field("Instructions")
        input.performTextReplacement("Check drainage ".repeat(36))
        assertTrue("Task editing needs the same compact text area as creation", input.fetchSemanticsNode().size.height <= 250 * context.resources.displayMetrics.density)
        action("Save changes").assertIsNotEnabled()
        compose.onNodeWithText("Instructions must be 500 characters or fewer.").assertExists()
        capture("task-edit-invalid-small-large-text")
    }

    @Test fun reversedPlantingPlanDatesExplainWhyUpdatingIsBlocked() {
        var calls = 0
        val planned = crop.copy(status = 0, plantedDate = null, plannedPlantingDate = "2026-11-20", expectedHarvestDate = "2026-11-19")
        content { CropScreen(CropUiState(farmId = "farm", cycles = listOf(planned)), true,
            {}, { _, _, _, _ -> }, { _, _, _, _ -> }, { _, _, _ -> calls++ }, {}, {}, { _, _, _, _ -> }) }
        action("Planned").performClick()
        compose.onNodeWithTag("crop-summary-cycle").performClick()
        action("More crop controls").performClick()
        action("Update planting plan").assertIsNotEnabled()
        compose.onNodeWithText("Expected harvest cannot be before planting date.").assertExists()
        compose.runOnIdle { assertEquals(0, calls) }
        capture("planting-edit-date-error")
    }
}
