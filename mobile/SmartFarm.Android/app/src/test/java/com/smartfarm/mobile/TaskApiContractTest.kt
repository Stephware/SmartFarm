package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.TaskApi
import com.smartfarm.mobile.data.remote.api.TaskCloseInput
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskApiContractTest {
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
