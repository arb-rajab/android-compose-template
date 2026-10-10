package com.example.starterapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.starterapp.data.local.entity.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingActionEntity>>

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pending: PendingActionEntity)

    @Update
    suspend fun update(pending: PendingActionEntity)

    @Delete
    suspend fun delete(pending: PendingActionEntity)

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun observeCount(): Flow<Int>
}
