package com.example.starterapp.data.repository

import com.example.starterapp.connectivity.ConnectivityObserver
import com.example.starterapp.connectivity.ConnectivityState
import com.example.starterapp.data.local.dao.PendingActionDao
import com.example.starterapp.data.local.entity.PendingActionEntity
import com.example.starterapp.data.remote.StarterApi
import com.example.starterapp.data.remote.dto.ActionRequestDto
import com.example.starterapp.data.remote.dto.ActionResponseDto
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

sealed interface SubmitOutcome {
    /** The server accepted the action while online. */
    data class Done(val response: ActionResponseDto) : SubmitOutcome

    /** Offline or a transient failure: the action is queued for `SyncQueueWorker` to replay. */
    data class Queued(val pending: PendingActionEntity) : SubmitOutcome

    /** A permanent HTTP error (see [ActionRepository.isPermanentFailure]); never queued. */
    data class Failed(val httpCode: Int) : SubmitOutcome
}

/** Outcome of one [ActionRepository.replayPending] attempt. */
enum class ReplayResult {
    /** The server accepted it; the entry was dequeued. */
    SUCCESS,

    /** The call failed but attempts remain; the entry stays queued for a later retry. */
    RETRYING,

    /** The call failed for good (permanent error or [ActionRepository.MAX_SYNC_ATTEMPTS]). */
    PERMANENTLY_FAILED
}

/**
 * Submit, offline-queue and replay lifecycle for one kind of write. Copy this class as the
 * pattern for each write operation that must survive being offline.
 */
class ActionRepository(
    private val pendingActionDao: PendingActionDao,
    private val connectivityObserver: ConnectivityObserver,
    private val apiProvider: () -> StarterApi,
    private val onActionQueued: () -> Unit = {}
) {
    fun observePendingCount(): Flow<Int> = pendingActionDao.observeCount()

    /**
     * Submits an action. Online: calls the API directly. Offline, or when the live call fails
     * with a network error or a transient HTTP error (429/5xx): queues it and schedules a sync.
     * A permanent HTTP error (401/403/422) is reported as [SubmitOutcome.Failed] instead of
     * queued, since retrying can never succeed.
     */
    suspend fun submit(payload: String): SubmitOutcome {
        val isOnline = connectivityObserver.observe().first() == ConnectivityState.ONLINE
        if (isOnline) {
            try {
                return SubmitOutcome.Done(apiProvider().submitAction(ActionRequestDto(payload)))
            } catch (_: IOException) {
                // Timeout, DNS or connection reset despite the transport reporting "online":
                // fall through to queueing.
            } catch (exc: HttpException) {
                // Retrofit throws HttpException (not IOException) for any non-2xx response.
                if (isPermanentFailure(exc)) {
                    return SubmitOutcome.Failed(exc.code())
                }
            }
        }
        return queueOffline(payload)
    }

    private suspend fun queueOffline(payload: String): SubmitOutcome.Queued {
        val pending =
            PendingActionEntity(
                localId = UUID.randomUUID().toString(),
                payload = payload,
                createdAt = System.currentTimeMillis()
            )
        pendingActionDao.insert(pending)
        onActionQueued()
        return SubmitOutcome.Queued(pending)
    }

    /** Replays one queued action. See [ReplayResult] for what each outcome means. */
    suspend fun replayPending(pending: PendingActionEntity): ReplayResult {
        val api = apiProvider()
        return try {
            api.submitAction(ActionRequestDto(pending.payload))
            pendingActionDao.delete(pending)
            ReplayResult.SUCCESS
        } catch (exc: IOException) {
            handleReplayFailure(pending, exc.message ?: "network error")
        } catch (exc: HttpException) {
            if (isPermanentFailure(exc)) {
                // Never going to succeed no matter how often WorkManager retries.
                pendingActionDao.delete(pending)
                ReplayResult.PERMANENTLY_FAILED
            } else {
                handleReplayFailure(pending, exc.message ?: "server error")
            }
        }
    }

    private suspend fun handleReplayFailure(
        pending: PendingActionEntity,
        lastError: String
    ): ReplayResult {
        val attempts = pending.attempts + 1
        return if (attempts >= MAX_SYNC_ATTEMPTS) {
            pendingActionDao.delete(pending)
            ReplayResult.PERMANENTLY_FAILED
        } else {
            pendingActionDao.update(pending.copy(attempts = attempts, lastError = lastError))
            ReplayResult.RETRYING
        }
    }

    companion object {
        /**
         * Cap on replay attempts before an action is given up on. WorkManager applies
         * exponential backoff between drain passes, so five attempts already spans a long
         * window.
         */
        const val MAX_SYNC_ATTEMPTS = 5

        /**
         * HTTP status codes that will never succeed on retry: 401/403 (auth missing or
         * rejected) and 422 (the server rejected the request body itself). 429 and 5xx are
         * transient and go through the normal retry path.
         */
        private val PERMANENT_HTTP_STATUS_CODES = setOf(401, 403, 422)

        private fun isPermanentFailure(exc: HttpException): Boolean =
            exc.code() in PERMANENT_HTTP_STATUS_CODES
    }
}
