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

        val completed = AtomicBoolean(false)
        val due = LocalDate.now(ZoneId.of("Asia/Manila")).toString()
        val farm = """{"id":"farm","name":"Green Valley Farm","locationText":"Angeles","timeZoneId":"Asia/Manila","role":"Owner","rowVersion":"version"}"""
        val secondFarm = farm.replace("\"farm\"", "\"second\"").replace("Green Valley Farm", "Second Farm")
        fun task(): String = """{"id":"task","farmId":"farm","cropCycleId":null,"assignedToUserId":"journey-user","taskType":2,"dueDate":"$due","status":${if (completed.get()) 1 else 0},"description":"Water north plot","rowVersion":"version"}"""
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                val body = when {
                    path == "/api/auth/login" && request.method == "POST" ->
                        """{"accessToken":"journey-token","refreshToken":"journey-refresh","expiresIn":3600,"tokenType":"Bearer"}"""
                    path == "/api/auth/me" ->
                        """{"id":"journey-user","fullName":"Test Farmer","email":"farmer@example.test"}"""
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
                    path == "/api/farms/farm/crop-types" ||
                        path == "/api/farms/farm/crop-cycles" -> "[]"
                    path == "/api/farms/farm/tasks" -> "[${task()}]"
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
            compose.onNodeWithText("Mark completed").performScrollTo().performClick()
            compose.onNodeWithText("Complete this task?").assertExists()
            compose.onNodeWithText("Complete task").performClick()
            compose.waitUntil(15_000) {
                completed.get() &&
                    compose.onAllNodesWithText("Task completed.").fetchSemanticsNodes().isNotEmpty()
            }

            androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()).pressBack()
            compose.onNodeWithText("My Farm").performClick()
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
            compose.onNodeWithText("What did you do on the farm?").assertExists()
            androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()).pressBack()
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
            server.shutdown()
        }
    }
}
