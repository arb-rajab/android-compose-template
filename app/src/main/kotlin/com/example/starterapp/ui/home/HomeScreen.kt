package com.example.starterapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starterapp.ui.components.ConnectivityBanner
import com.example.starterapp.ui.components.EmptyState
import com.example.starterapp.ui.components.PrimaryButton

/** Placeholder home screen. Replace with your app's first screen. */
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val connectivity by viewModel.connectivity.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Home") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConnectivityBanner(connectivityState = connectivity, pendingCount = pendingCount)
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryButton(text = "Submit a sample action", onClick = viewModel::submitSample)
                if (state.lastResult.isNotEmpty()) {
                    Text(state.lastResult)
                } else {
                    EmptyState(
                        title = "No actions yet",
                        message = "Submit a sample action to see the result here."
                    )
                }
                OutlinedButton(onClick = onOpenSettings) { Text("Server settings") }
            }
        }
    }
}
