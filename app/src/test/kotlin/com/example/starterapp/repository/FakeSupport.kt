package com.example.starterapp.repository

import com.example.starterapp.connectivity.ConnectivityObserver
import com.example.starterapp.connectivity.ConnectivityState
import com.example.starterapp.data.local.dao.PendingActionDao
import com.example.starterapp.data.local.entity.PendingActionEntity
import com.example.starterapp.data.remote.StarterApi
import com.example.starterapp.data.remote.dto.ActionRequestDto
import com.example.starterapp.data.remote.dto.ActionResponseDto
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class FakeConnectivityObserver(initial: ConnectivityState) : ConnectivityObserver {
    val state = MutableStateFlow(initial)

    override fun observe(): StateFlow<ConnectivityState> = state
}

/**
 * A fake [StarterApi] whose behavior is driven per test: return a canned response, or throw
 * [IOException] to simulate the live call failing despite the transport reporting online.
 */
class FakeStarterApi(
    var shouldFail: Boolean = false,
    var result: (payload: String) -> ActionResponseDto = { payload ->
        ActionResponseDto(id = "id-$payload", createdAt = "2026-01-01T00:00:00Z")
    }
) : StarterApi {
    var callCount = 0
        private set

    override suspend fun submitAction(request: ActionRequestDto): ActionResponseDto {
        callCount++
        if (shouldFail) throw IOException("simulated network failure")
        return result(request.payload)
    }
}

/** In-memory fake standing in for Room: fast, no Robolectric/SQLite needed for logic tests. */
class FakePendingActionDao : PendingActionDao {
    private val flow = MutableStateFlow<List<PendingActionEntity>>(emptyList())

    override fun observeAll(): StateFlow<List<PendingActionEntity>> = flow

    override suspend fun getAll(): List<PendingActionEntity> = flow.value

    override suspend fun insert(pending: PendingActionEntity) {
        flow.value = (flow.value.filterNot { it.localId == pending.localId } + pending)
            .sortedBy { it.createdAt }
    }

    override suspend fun update(pending: PendingActionEntity) {
        flow.value = flow.value.map { if (it.localId == pending.localId) pending else it }
    }

    override suspend fun delete(pending: PendingActionEntity) {
        flow.value = flow.value.filterNot { it.localId == pending.localId }
    }

    override fun observeCount(): Flow<Int> = flow.map { it.size }
}
