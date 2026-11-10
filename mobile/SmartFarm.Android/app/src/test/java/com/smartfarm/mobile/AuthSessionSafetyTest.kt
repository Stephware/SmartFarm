package com.smartfarm.mobile

import com.smartfarm.mobile.data.remote.api.ApiClientFactory
import com.smartfarm.mobile.data.remote.api.AuthApi
import com.smartfarm.mobile.data.remote.api.FarmApi
import com.smartfarm.mobile.data.repository.AuthRepository
import com.smartfarm.mobile.data.repository.FarmRepository
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionSafetyTest {
    private fun json(body: String) = MockResponse().setResponseCode(200)
        .addHeader("Content-Type", "application/json").setBody(body)

    private fun tokens(access: String, refresh: String) =
        """{"tokenType":"Bearer","accessToken":"$access","expiresIn":1800,"refreshToken":"$refresh"}"""

    private val account =
        """{"id":"d56b3ae2-08ac-4662-9d7f-9b9582bd76be","fullName":"Test Farmer","email":"farmer@example.test"}"""

    @Test fun simultaneousExpiredRequestsOnlyRotateRefreshTokenOnce() = runBlocking {
        val refreshCount = AtomicInteger()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/auth/login" -> json(tokens("old-access", "old-refresh"))
                "/api/auth/me" -> json(account)
                "/api/auth/refresh" -> {
                    refreshCount.incrementAndGet()
                    json(tokens("new-access", "new-refresh"))
                }
                "/api/farms" -> if (request.getHeader("Authorization") == "Bearer old-access")
                    MockResponse().setResponseCode(401)
                else json("[]")
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            val farms = FarmRepository(auth) { retrofit.create(FarmApi::class.java) }
            auth.login("farmer@example.test", "ExamplePassword1")

            val first = async(Dispatchers.IO) { farms.list() }
            val second = async(Dispatchers.IO) { farms.list() }
            assertTrue(first.await().isEmpty())
            assertTrue(second.await().isEmpty())
            assertEquals("Only one request should rotate the refresh token", 1, refreshCount.get())
        } finally {
            server.shutdown()
        }
    }

    @Test fun logoutDuringRefreshCannotRestoreSession() = runBlocking {
        val refreshStarted = CountDownLatch(1)
        val allowRefresh = CountDownLatch(1)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/auth/login" -> json(tokens("old-access", "old-refresh"))
                "/api/auth/me" -> json(account)
                "/api/farms" -> MockResponse().setResponseCode(401)
                "/api/auth/refresh" -> {
                    refreshStarted.countDown()
                    if (!allowRefresh.await(8, TimeUnit.SECONDS))
                        MockResponse().setResponseCode(503)
                    else json(tokens("new-access", "new-refresh"))
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            val farms = FarmRepository(auth) { retrofit.create(FarmApi::class.java) }
            auth.login("farmer@example.test", "ExamplePassword1")
            val pending = async(Dispatchers.IO) {
                runCatching { farms.list() }
            }
            try {
                assertTrue("Refresh did not start", withContext(Dispatchers.IO) {
                    refreshStarted.await(5, TimeUnit.SECONDS)
                })
                auth.logout()
            } finally {
                allowRefresh.countDown()
            }
            val result = pending.await()
            assertTrue("A late refresh must be rejected", result.exceptionOrNull() is IllegalStateException)
            assertEquals(null, auth.currentUserId)
            assertTrue(runCatching { auth.authorized { "should-not-run" } }.isFailure)
        } finally {
            allowRefresh.countDown()
            server.shutdown()
        }
    }

    @Test fun logoutDuringLoginCannotSignInAnOldRequest() = runBlocking {
        val accountStarted = CountDownLatch(1)
        val allowAccount = CountDownLatch(1)
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/auth/login" -> json(tokens("access", "refresh"))
                "/api/auth/me" -> {
                    accountStarted.countDown()
                    if (!allowAccount.await(8, TimeUnit.SECONDS))
                        MockResponse().setResponseCode(503)
                    else json(account)
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        try {
            val retrofit = ApiClientFactory.create(server.url("/").toString(), true)
            val auth = AuthRepository { retrofit.create(AuthApi::class.java) }
            val pending = async(Dispatchers.IO) {
                runCatching { auth.login("farmer@example.test", "ExamplePassword1") }
            }
            try {
                assertTrue(withContext(Dispatchers.IO) {
                    accountStarted.await(5, TimeUnit.SECONDS)
                })
                auth.logout()
            } finally {
                allowAccount.countDown()
            }
            assertTrue(pending.await().exceptionOrNull() is IllegalStateException)
            assertEquals(null, auth.currentUserId)
        } finally {
            allowAccount.countDown()
            server.shutdown()
        }
    }
}
