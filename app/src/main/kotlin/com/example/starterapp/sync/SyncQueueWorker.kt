package com.example.starterapp.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.starterapp.StarterApplication
import com.example.starterapp.data.repository.ReplayResult

/**
 * Drains the pending-action queue (oldest first) as soon as the network constraint is
 * satisfied. Enqueued under one fixed unique name so a burst of offline submissions doesn't
 * spawn a worker per tap; one drain pass handles everything queued so far.
 */
class SyncQueueWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as StarterApplication
        val pending = app.pendingActionDao.getAll()
        if (pending.isEmpty()) return Result.success()

        // A permanently failed item is dequeued for good and must not keep this worker
        // retrying; only an item that still has attempts left does.
        var needsRetry = false
        for (item in pending) {
            if (app.actionRepository.replayPending(item) == ReplayResult.RETRYING) {
                needsRetry = true
            }
        }
        return if (needsRetry) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "sync_pending_actions"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder().setRequiredNetworkType(
                NetworkType.CONNECTED
            ).build()
            val request =
                OneTimeWorkRequestBuilder<SyncQueueWorker>()
                    .setConstraints(constraints)
                    .build()
            // REPLACE restarts a running pass so it also picks up rows queued after it started.
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
