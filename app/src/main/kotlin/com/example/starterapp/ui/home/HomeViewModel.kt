package com.example.starterapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.starterapp.connectivity.ConnectivityObserver
import com.example.starterapp.connectivity.ConnectivityState
import com.example.starterapp.data.repository.ActionRepository
import com.example.starterapp.data.repository.SubmitOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(val lastResult: String = "")

/** Example ViewModel: exposes connectivity and the pending count, and submits one action. */
class HomeViewModel(
    private val repository: ActionRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val connectivity: StateFlow<ConnectivityState> =
        connectivityObserver.observe().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ConnectivityState.ONLINE
        )

    val pendingCount: StateFlow<Int> =
        repository.observePendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun submitSample() {
        viewModelScope.launch {
            val outcome = repository.submit("sample")
            _uiState.value =
                HomeUiState(
                    lastResult =
                    when (outcome) {
                        is SubmitOutcome.Done -> "Submitted (${outcome.response.id})"
                        is SubmitOutcome.Queued -> "Queued for sync"
                        is SubmitOutcome.Failed -> "Rejected by the server (${outcome.httpCode})"
                    }
                )
        }
    }

    class Factory(
        private val repository: ActionRepository,
        private val connectivityObserver: ConnectivityObserver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository, connectivityObserver) as T
    }
}
