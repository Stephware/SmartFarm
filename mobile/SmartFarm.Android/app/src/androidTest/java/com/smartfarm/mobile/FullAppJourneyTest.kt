package com.smartfarm.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * Runs the real Activity, navigation, ViewModels, repositories and Retrofit
 * against a deterministic local API. This is not a live SQL/server test.
 */
class FullAppJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun farmerSignsInOpensFarmCompletesTaskAndSignsOut() {
        // Avoid sending a developer's real account requests to the test server.
        // CI sets this property explicitly; normal local emulator runs skip it.
        assumeTrue("Set -PsmartfarmDebugApiUrl=http://127.0.0.1:5081/",
            BuildConfig.API_BASE_URL == "http://127.0.0.1:5081/")

        val journeyUserId = "journey-${java.util.UUID.randomUUID()}"
        val syncOnline = AtomicBoolean(false)
        val waterSent = AtomicBoolean(false)
        val supplySent = AtomicBoolean(false)
        val harvestTotal = java.util.concurrent.atomic.AtomicReference(java.math.BigDecimal.ZERO)
        val supplyId = "33333333-3333-4333-8333-333333333333"
        val completed = AtomicBoolean(false)
        val due = LocalDate.now(ZoneId.of("Asia/Manila")).toString()
        val farm = """{"id":"farm","name":"Green Valley Farm","locationText":"Angeles","timeZoneId":"Asia/Manila","role":"Owner","rowVersion":"version"}"""
        val secondFarm = farm.replace("\"farm\"", "\"second\"").replace("Green Valley Farm", "Second Farm")
        val manualDue = LocalDate.parse(due).minusDays(1).toString()
        fun task(): String = """{"id":"task","farmId":"farm","cropCycleId":null,"assignedToUserId":"$journeyUserId","taskType":2,"dueDate":"$manualDue","status":${if (completed.get()) 1 else 0},"description":"Water north plot","rowVersion":"version"}"""
        val linkedTaskId = "11111111-1111-4111-8111-111111111111"
        val linkedTask = """{"id":"$linkedTaskId","farmId":"farm","cropCycleId":"crop","assignedToUserId":"$journeyUserId","taskType":2,"dueDate":"$due","status":0,"description":"Water this exact crop task","rowVersion":"version"}"""
        val fertilizerTaskId = "22222222-2222-4222-8222-222222222222"
        val fertilizerTask = linkedTask.replace(linkedTaskId, fertilizerTaskId)
            .replace("\"taskType\":2", "\"taskType\":3").replace("Water this exact crop task", "Feed this exact crop task")
        val dao = (compose.activity.application as SmartFarmApplication).container.database.pendingOperationDao()
        val priorIds = runBlocking { dao.watch(journeyUserId).first().map { it.operationId }.toSet() }
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                val body = when {
                    path == "/api/auth/login" && request.method == "POST" ->
                        """{"accessToken":"journey-token","refreshToken":"journey-refresh","expiresIn":3600,"tokenType":"Bearer"}"""
                    path == "/api/auth/me" ->
                        """{"id":"$journeyUserId","fullName":"Test Farmer","email":"farmer@example.test"}"""
                    path == "/api/farms" -> "[$farm,$secondFarm]"
                    path == "/api/farm-join-requests" -> "[]"
                    path == "/api/farms/second" -> secondFarm
                    path == "/api/farms/second/weather" ->
                        """{"farmId":"second","hasCoordinates":false,"timeZoneId":"Asia/Manila","days":[],"advisories":[]}"""
                    path == "/api/farms/second/join-requests" ->
                        """{"joinCode":"WXYZ12345678","requests":[],"history":[]}"""
                    path.startsWith("/api/farms/second/") -> "[]"
                    path == "/api/farms/farm" -> farm
                    path == "/api/farms/farm/plots" ->
                        """[{"id":"plot","farmId":"farm","name":"North plot","areaSqMeters":100,"rowVersion":"version"}]"""
                    path == "/api/farms/farm/members" -> "[]"
                    path == "/api/farms/farm/join-requests" ->
                        """{"joinCode":"ABCD12345678","requests":[],"history":[]}"""
                    path == "/api/farms/farm/weather" ->
                        """{"farmId":"farm","hasCoordinates":false,"timeZoneId":"Asia/Manila","days":[],"advisories":[]}"""
                    path == "/api/farms/farm/crop-types" -> "[]"
                    path == "/api/farms/farm/crop-cycles" ->
                        """[{"id":"crop","farmId":"farm","plotId":"plot","plotName":"North plot","cropTypeId":"type","cropTypeName":"Tomato","status":1,"plannedPlantingDate":"2026-10-01","actualPlantingDate":"2026-10-01","harvestUnit":"kg","wateringIntervalDays":2,"rowVersion":"version"}]"""
                    path == "/api/farms/farm/crop-cycles/crop/monitoring" ->
                        """{"farmId":"farm","cycleId":"crop","canRecord":true,"activities":${if (supplySent.get()) """[{"id":"44444444-4444-4444-8444-444444444444","activityType":2,"occurredAtUtc":"2026-11-14T01:00:00Z","recordedAtUtc":"2026-11-14T01:00:00Z","recordedByUserId":"$journeyUserId","notes":"Confirmed fertilizer after reconnect"}]""" else "[]"},"observations":[]}"""
                    path == "/api/farms/farm/reports/harvests" ->
                        """{"farmId":"farm","farmName":"Green Valley Farm","timeZoneId":"Asia/Manila","fromDate":null,"toDate":null,"validHarvestCount":1,"voidedHarvestCount":0,"unitTotals":[{"unit":"kg","quantity":${harvestTotal.get().toPlainString()},"harvestCount":1}]}"""
                    path == "/api/farms/farm/inventory" ->
                        """{"items":[{"id":"$supplyId","farmId":"farm","name":"Test fertilizer","category":2,"unit":"kg","quantityOnHand":${if (supplySent.get()) "6.750" else "8"},"isLowStock":false,"rowVersion":"AQIDBAUGBwg="}],"lowStockCount":0}"""
                    path == "/api/farms/farm/crop-cycles/crop/harvests" ->
                        """{"farmId":"farm","cycleId":"crop","cropName":"Tomato","plotName":"North plot","unit":"kg","cropStatus":1,"canRecord":true,"canVoid":true,"totalQuantity":${harvestTotal.get()},"validHarvestCount":${if (harvestTotal.get().signum() > 0) 1 else 0},"voidedHarvestCount":0,"entries":[]}"""
                    path == "/api/farms/farm/tasks" -> "[${task()},${if (waterSent.get()) linkedTask.replace("\"status\":0", "\"status\":1") else linkedTask},${if (supplySent.get()) fertilizerTask.replace("\"status\":0", "\"status\":1") else fertilizerTask}]"
                    path.startsWith("/api/sync/operations/") -> {
                        if (!syncOnline.get()) return MockResponse().setResponseCode(503)
                        val payload = com.google.gson.JsonParser.parseString(request.body.readUtf8()).asJsonObject
                        val kind = payload.get("operationType").asString
                        val result = if (kind == "activity_stock") {
                            supplySent.set(true)
                            """{"activity":{"id":"44444444-4444-4444-8444-444444444444"},"inventory":{"id":"$supplyId"}}"""
                        } else if (kind == "harvest") {
                            harvestTotal.set(payload.getAsJsonObject("harvest").get("quantity").asBigDecimal)
                            "{}"
                        } else { waterSent.set(true); "{}" }
                        """{"operationId":"${path.substringAfterLast('/')}","operationType":"$kind","result":$result,"replayed":false}"""
                    }
                    path == "/api/farms/farm/tasks/task/close" && request.method == "PUT" -> {
                        completed.set(true)
                        task()
                    }
                    path == "/api/notifications" -> "[]"
                    else -> return MockResponse().setResponseCode(404)
                }
                return MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start(5081)
        try {
            compose.onNodeWithText("Welcome to AniGo").assertExists()
            compose.onNodeWithText("Log in").performClick()
            compose.onNodeWithText("Email address").performTextInput("farmer@example.test")
            compose.onNodeWithText("Password").performTextInput("Password2026")
            compose.onNodeWithText("Log in to my farms").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Green Valley Farm").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Green Valley Farm").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag("farm-bottom-navigation").fetchSemanticsNodes().isNotEmpty()
            }

            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("My tasks").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("My tasks").performScrollTo().performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Mark completed").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText("Mark completed").onFirst().performScrollTo().performClick()
            compose.onNodeWithText("Complete this task?").assertExists()
            compose.onNodeWithText("Complete task").performClick()
            compose.waitUntil(15_000) {
                completed.get() &&
                    compose.onAllNodesWithText("Task completed.").fetchSemanticsNodes().isNotEmpty()
            }

            // Use app navigation, not a device Back press that may only dismiss an IME/dialog.
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Record watering").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Record watering").performScrollTo().performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasText("Save activity") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Tomato · North plot").assertExists()
            compose.onNodeWithText("Save activity").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { dao.watch(journeyUserId).first().any { it.operationId !in priorIds } }
            }
            val queued = runBlocking { dao.watch(journeyUserId).first().single { it.operationId !in priorIds } }
            val payload = com.google.gson.JsonParser.parseString(queued.payloadJson).asJsonObject
            assertEquals("activity", queued.operationType)
            assertEquals(linkedTaskId, payload.getAsJsonObject("activity").get("farmTaskId")?.asString)
            // Use app navigation, not a device Back press that may only dismiss an IME/dialog.
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Record fertilizing").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Record fertilizing").performScrollTo().performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodes(hasText("Deduct supply from farm inventory") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Deduct supply from farm inventory").performScrollTo().performClick()
            compose.onNodeWithText("Choose supply").performScrollTo().performClick()
            compose.onNodeWithText("Test fertilizer", substring = true).performClick()
            compose.onNodeWithText("Quantity used (kg)").performScrollTo().performTextInput("1.250")
            compose.onNodeWithText("Save activity and supply use").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                runBlocking { dao.watch(journeyUserId).first().any { it.operationId !in priorIds && it.operationType == "activity_stock" } }
            }
            val combined = runBlocking { dao.watch(journeyUserId).first().single { it.operationId !in priorIds && it.operationType == "activity_stock" } }
            val combinedPayload = com.google.gson.JsonParser.parseString(combined.payloadJson).asJsonObject.getAsJsonObject("activityStock")
            assertEquals(fertilizerTaskId, combinedPayload.getAsJsonObject("activity").get("farmTaskId").asString)
            assertEquals(supplyId, combinedPayload.get("inventoryItemId").asString)
            assertEquals("1.250", combinedPayload.get("quantityUsed").asString)
            syncOnline.set(true)
            val container = (compose.activity.application as SmartFarmApplication).container
            assertTrue(runBlocking { container.syncRepository.uploadPending() })
            val monitoring = androidx.lifecycle.ViewModelProvider(compose.activity)[com.smartfarm.mobile.features.monitoring.MonitoringViewModel::class.java]
            compose.waitUntil(5000) {
                monitoring.state.value.activities.any { it.notes == "Confirmed fertilizer after reconnect" } &&
                    monitoring.state.value.supplies.singleOrNull()?.quantityOnHand == java.math.BigDecimal("6.750")
            }
            compose.onNodeWithText("History").performScrollTo().performClick()
            compose.onNodeWithText("Confirmed fertilizer after reconnect").performScrollTo().assertExists()
            // Hardware Back can dismiss the quantity keyboard without leaving the form.
            // Use the explicit screen action, then wait for the workspace navigation.
            compose.onNodeWithText("Back").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("farm-bottom-navigation").fetchSemanticsNodes().isNotEmpty()
            }
            val tasks = androidx.lifecycle.ViewModelProvider(compose.activity)[com.smartfarm.mobile.features.tasks.TaskViewModel::class.java]
            compose.waitUntil(5000) {
                tasks.state.value.tasks.filter { it.id == linkedTaskId || it.id == fertilizerTaskId }.let {
                    it.size == 2 && it.all { task -> task.status == 1 }
                }
            }
            syncOnline.set(false)
            compose.onNodeWithText("Record").performClick()
            compose.onNodeWithText("Save an actual harvest quantity").performClick()
            compose.onNodeWithText("Tomato").performClick()
            val harvests = androidx.lifecycle.ViewModelProvider(compose.activity)[com.smartfarm.mobile.features.harvests.HarvestViewModel::class.java]
            compose.waitUntil(5000) { harvests.state.value.overview != null && !harvests.state.value.busy }
            compose.onNodeWithText("Harvested quantity (kg)").performScrollTo().performTextInput("2.500")
            compose.onNodeWithText("Save harvested amount").performScrollTo().performClick()
            compose.waitUntil(5000) {
                runBlocking { dao.watch(journeyUserId).first().any { it.operationType == "harvest" } }
            }
            assertEquals(java.math.BigDecimal.ZERO, harvests.state.value.overview?.totalQuantity)
            syncOnline.set(true)
            assertTrue(runBlocking { container.syncRepository.uploadPending() })
            compose.waitUntil(5000) { harvests.state.value.overview?.totalQuantity == java.math.BigDecimal("2.500") }
            compose.onNodeWithText("Back").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithTag("farm-bottom-navigation").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("My Farm").performClick()
            compose.onNodeWithTag("record-list").performScrollToNode(hasText("Harvest summary"))
            compose.onNodeWithText("Harvest summary").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("2.500 kg").fetchSemanticsNodes().isNotEmpty() }
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Switch farm: Green Valley Farm").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Farm time zone unavailable. Only unfiltered reports can be shown.").assertDoesNotExist()
            compose.onNodeWithText("Back").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Farm records").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("record-list").performScrollToNode(hasText("Farm settings"))
            compose.onNodeWithText("Farm settings").performClick()
            compose.onNodeWithText("Name").performScrollTo().performTextReplacement("Unsaved farm name")
            compose.onNodeWithContentDescription("Switch farm: Green Valley Farm").performClick()
            compose.onNodeWithText("Second Farm").performClick()
            compose.onNodeWithText("Leave this screen?").assertExists()
            compose.onNodeWithText("Keep working").performClick()
            compose.onNodeWithText("Unsaved farm name").assertExists()
            compose.onNodeWithContentDescription("Switch farm: Green Valley Farm").performClick()
            compose.onNodeWithText("Second Farm").performClick()
            compose.onNodeWithText("Leave screen").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithContentDescription("Switch farm: Second Farm").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Today · Second Farm").assertExists()
            compose.onNodeWithContentDescription("Switch farm: Second Farm").performClick()
            compose.onNodeWithText("Green Valley Farm").performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithContentDescription("Switch farm: Green Valley Farm").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Record").performClick()
            compose.onNodeWithText("What did you do on the farm?").assertIsDisplayed()
            val device = androidx.test.uiautomator.UiDevice.getInstance(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
            device.waitForIdle()
            device.pressBack()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("What did you do on the farm?").fetchSemanticsNodes().isEmpty()
            }
            compose.onNodeWithText("What did you do on the farm?").assertDoesNotExist()
            compose.onNodeWithText("My Farm").performClick()
            compose.onNodeWithTag("record-list").performScrollToNode(hasText("My account"))
            compose.onNodeWithText("My account").performClick()
            compose.onNodeWithTag("request-sign-out").performScrollTo().performClick()
            compose.onNodeWithText("Sign out of AniGo?").assertExists()
            compose.onNodeWithText("Keep working").performClick()
            compose.onNodeWithTag("request-sign-out").performClick()
            compose.onNodeWithTag("confirm-sign-out").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Welcome to AniGo").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue("Server should receive task completion", completed.get())
        } finally {
            runBlocking { dao.watch(journeyUserId).first().filter { it.operationId !in priorIds }
                .forEach { dao.remove(it.operationId, journeyUserId) } }
            server.shutdown()
        }
    }
}
