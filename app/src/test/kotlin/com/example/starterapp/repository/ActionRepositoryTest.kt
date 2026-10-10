package com.example.starterapp.repository

import com.example.starterapp.connectivity.ConnectivityState
import com.example.starterapp.data.repository.ActionRepository
import com.example.starterapp.data.repository.ReplayResult
import com.example.starterapp.data.repository.SubmitOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActionRepositoryTest {
    private lateinit var dao: FakePendingActionDao
    private lateinit var connectivity: FakeConnectivityObserver
    private lateinit var api: FakeStarterApi
    private lateinit var repository: ActionRepository
    private var queuedCallbackFired = false

    @Before
    fun setUp() {
        dao = FakePendingActionDao()
        connectivity = FakeConnectivityObserver(ConnectivityState.ONLINE)
        api = FakeStarterApi()
        queuedCallbackFired = false
        repository =
            ActionRepository(
                pendingActionDao = dao,
                connectivityObserver = connectivity,
                apiProvider = { api },
                onActionQueued = { queuedCallbackFired = true }
            )
    }

    @Test
    fun `submit online calls the API and does not queue`() = runTest {
        val outcome = repository.submit("a")

        check(outcome is SubmitOutcome.Done)
        assertEquals("id-a", outcome.response.id)
        assertTrue(dao.getAll().isEmpty())
        assertFalse(queuedCallbackFired)
    }

    @Test
    fun `submit offline queues the action without calling the API`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE

        val outcome = repository.submit("a")

        check(outcome is SubmitOutcome.Queued)
        assertEquals(0, api.callCount)
        assertEquals(1, dao.getAll().size)
        assertEquals("a", dao.getAll().first().payload)
        assertTrue(queuedCallbackFired)
    }

    @Test
    fun `submit online falls back to queueing when the live call hits a network error`() = runTest {
        api.shouldFail = true

        val outcome = repository.submit("a")

        check(outcome is SubmitOutcome.Queued)
        assertEquals(1, api.callCount)
        assertEquals(1, dao.getAll().size)
        assertTrue(queuedCallbackFired)
    }

    @Test
    fun `replayPending success dequeues the entry`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE
        val queued = repository.submit("a") as SubmitOutcome.Queued

        val result = repository.replayPending(queued.pending)

        assertEquals(ReplayResult.SUCCESS, result)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `replayPending keeps retrying a network failure until the attempt cap`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE
        val queued = repository.submit("a") as SubmitOutcome.Queued
        api.shouldFail = true

        var pending = queued.pending
        repeat(ActionRepository.MAX_SYNC_ATTEMPTS - 1) {
            assertEquals(ReplayResult.RETRYING, repository.replayPending(pending))
            pending = dao.getAll().single()
        }
        assertEquals(ActionRepository.MAX_SYNC_ATTEMPTS - 1, pending.attempts)

        val last = repository.replayPending(pending)

        assertEquals(ReplayResult.PERMANENTLY_FAILED, last)
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `pending count follows the queue`() = runTest {
        connectivity.state.value = ConnectivityState.OFFLINE
        repository.submit("a")
        repository.submit("b")

        val count = repository.observePendingCount()
        assertEquals(2, count.first())
    }
}
