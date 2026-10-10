package com.example.starterapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The offline queue: one row per action submitted while offline (or while the live call
 * failed), waiting for `SyncQueueWorker` to replay it once connectivity returns.
 * Replace [payload] and the table with your own domain shape.
 */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val localId: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null
)
