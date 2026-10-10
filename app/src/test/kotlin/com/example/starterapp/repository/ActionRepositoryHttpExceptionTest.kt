package com.example.starterapp.repository

import com.example.starterapp.connectivity.ConnectivityState
import com.example.starterapp.data.ServerConfig
import com.example.starterapp.data.repository.ActionRepository
import com.example.starterapp.data.repository.ReplayResult
import com.example.starterapp.data.repository.SubmitOutcome
import com.example.starterapp.di.ApiClientFactory
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Retrofit throws [retrofit2.HttpException] (not [java.io.IOException]) for any non-2xx
 * response. This wires the repository to a real Retrofit client talking to a [MockWebServer]
 * instead of a fake API, so it exercises a genuine HttpException the way production traffic
 * would.
 */
class ActionRepositoryHttpExceptionTest {
    private lateinit var server: MockWebServer
    private lateinit var dao: FakePendingActionDao
    private lateinit var connectivity: FakeConnectivityObserver
    private lateinit var repository: ActionRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        dao = FakePendingActionDao()
        connectivity = FakeConnectivityObserver(ConnectivityState.ONLINE)
        val api = ApiClientFactory.create(ServerConfig(baseUrl = server.url("/").toString()))
        repository =
            ActionRepository(
                pendingActionDao = dao,
                connectivityObserver = connectivity,
                apiProvider = { api }
            )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `submit does not crash on a real 429 response and queues instead`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("rate limited"))

        val outcome = repository.submit("a")

        check(outcome is SubmitOutcome.Queued)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `submit on a real 422 response fails immediately, not queued for retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":"invalid"}"""))

        val outcome = repository.submit("a")

        check(outcome is SubmitOutcome.Failed)
        assertEquals(422, outcome.httpCode)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `submit on a real 401 response fails immediately, not queued for retry`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("unauthorized"))

        val outcome = repository.submit("a")

        check(outcome is SubmitOutcome.Failed)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `replayPending does not crash on a real 5xx response and keeps retrying`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE
        val queued = repository.submit("a") as SubmitOutcome.Queued
        server.enqueue(MockResponse().setResponseCode(503).setBody("service unavailable"))

        val result = repository.replayPending(queued.pending)

        assertEquals(ReplayResult.RETRYING, result)
        val remaining = dao.getAll()
        assertEquals(1, remaining.size)
        assertEquals(1, remaining.first().attempts)
    }

    @Test
    fun `replayPending gives up after MAX_SYNC_ATTEMPTS of real 5xx responses`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE
        val queued = repository.submit("a") as SubmitOutcome.Queued

        var pending = queued.pending
        var lastResult: ReplayResult? = null
        repeat(ActionRepository.MAX_SYNC_ATTEMPTS) {
            server.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
            lastResult = repository.replayPending(pending)
            pending = dao.getAll().firstOrNull() ?: pending
        }

        assertEquals(ReplayResult.PERMANENTLY_FAILED, lastResult)
        assertTrue("pending entry should be dequeued for good", dao.getAll().isEmpty())
    }

    @Test
    fun `replayPending gives up immediately on a 403, spending no attempts`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE
        val queued = repository.submit("a") as SubmitOutcome.Queued
        server.enqueue(MockResponse().setResponseCode(403).setBody("forbidden"))

        val result = repository.replayPending(queued.pending)

        assertEquals(ReplayResult.PERMANENTLY_FAILED, result)
        assertTrue("pending entry should be dequeued for good", dao.getAll().isEmpty())
    }
}
