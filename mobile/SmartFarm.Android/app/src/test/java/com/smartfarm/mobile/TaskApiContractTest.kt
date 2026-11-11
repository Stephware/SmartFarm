package com.smartfarm.mobile

import com.google.gson.JsonParser
import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.TaskApi
import com.smartfarm.mobile.data.remote.api.TaskCloseInput
import com.smartfarm.mobile.data.remote.api.TaskCreateInput
import com.smartfarm.mobile.data.remote.api.TaskUpdateInput
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskApiContractTest {
    @Test fun managerCreatesAndUpdatesTasksWithExistingServerContracts() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = ApiClientFactory.create(server.url("/").toString(), true)
                .create(TaskApi::class.java)
            val response = """{"id":"task-1","farmId":"farm-1","taskType":4,
                "dueDate":"2026-11-12","status":0,"rowVersion":"version"}"""
            server.enqueue(MockResponse().setResponseCode(200).setBody(response)
                .addHeader("Content-Type", "application/json"))
            api.create("Bearer manager", "farm-1", TaskCreateInput(
                4, "2026-11-12", "Inspect leaves", null, "member-1"
            ))
            val created = server.takeRequest()
            assertEquals("POST", created.method)
            assertEquals("/api/farms/farm-1/tasks", created.path)
            assertEquals("Bearer manager", created.getHeader("Authorization"))
            val createBody = JsonParser.parseString(created.body.readUtf8()).asJsonObject
            assertEquals(4, createBody.get("taskType").asInt)
            assertEquals("member-1", createBody.get("assignedToUserId").asString)

            server.enqueue(MockResponse().setResponseCode(200).setBody(response)
                .addHeader("Content-Type", "application/json"))
            api.update("Bearer manager", "farm-1", "task-1",
                TaskUpdateInput("2026-11-13", "Reassigned inspection", null, "version"))
            val updated = server.takeRequest()
            assertEquals("PUT", updated.method)
            assertEquals("/api/farms/farm-1/tasks/task-1", updated.path)
            assertEquals("Bearer manager", updated.getHeader("Authorization"))
            val updateBody = JsonParser.parseString(updated.body.readUtf8()).asJsonObject
            assertEquals("version", updateBody.get("rowVersion").asString)
            assertEquals("2026-11-13", updateBody.get("dueDate").asString)
            assertEquals(true, updateBody.get("assignedToUserId").isJsonNull)
        } finally {
            server.shutdown()
        }
    }

    @Test fun tasksAndNotificationsUseAuthenticatedExistingEndpoints() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            val api = ApiClientFactory.create(server.url("/").toString(), true).create(TaskApi::class.java)
            val task = """{"id":"task","farmId":"farm","taskType":2,"dueDate":"2026-10-26","status":1,"rowVersion":"version"}"""
            server.enqueue(MockResponse().setBody("[$task]").addHeader("Content-Type", "application/json"))
            assertEquals("task", api.tasks("Bearer test", "farm").single().id)
            server.enqueue(MockResponse().setBody(task).addHeader("Content-Type", "application/json"))
            assertEquals(1, api.close("Bearer test", "farm", "task", TaskCloseInput(1, "version")).status)
            server.enqueue(MockResponse().setBody("[]").addHeader("Content-Type", "application/json"))
            assertEquals(emptyList<Any>(), api.notifications("Bearer test"))
            server.enqueue(MockResponse().setResponseCode(204))
            api.read("Bearer test", "notice")
            for ((method, path) in listOf("GET" to "/api/farms/farm/tasks", "PUT" to "/api/farms/farm/tasks/task/close",
                "GET" to "/api/notifications", "PUT" to "/api/notifications/notice/read")) {
                val request = server.takeRequest()
                assertEquals(method, request.method)
                assertEquals(path, request.path)
                assertEquals("Bearer test", request.getHeader("Authorization"))
                if (path.endsWith("/close")) assertEquals("""{"status":1,"rowVersion":"version"}""", request.body.readUtf8())
            }
        } finally { server.shutdown() }
    }
}
