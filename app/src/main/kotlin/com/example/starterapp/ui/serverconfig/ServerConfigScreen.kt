package com.example.starterapp.ui.serverconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starterapp.ui.components.AppTextField
import com.example.starterapp.ui.components.PrimaryButton

/**
 * Collects a server URL and one static auth header. Replace with a real login screen if your
 * backend has a login endpoint.
 */
@Composable
fun ServerConfigScreen(viewModel: ServerConfigViewModel, onContinue: () -> Unit) {
    val config by viewModel.uiState.collectAsState()
    val saved by viewModel.saved.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Connect to server") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Enter the server URL and, if your deployment requires one, an auth header.",
                style = MaterialTheme.typography.bodyMedium
            )
            AppTextField(
                value = config.baseUrl,
                onValueChange = viewModel::onBaseUrlChanged,
                label = "Server URL",
                placeholder = "https://api.example.com"
            )
            AppTextField(
                value = config.authHeaderName,
                onValueChange = viewModel::onHeaderNameChanged,
                label = "Auth header name",
                placeholder = "Authorization"
            )
            AppTextField(
                value = config.authHeaderValue,
                onValueChange = viewModel::onHeaderValueChanged,
                label = "Auth header value",
                placeholder = "Bearer …"
            )
            PrimaryButton(
                text = if (saved) "Saved — continue" else "Save and continue",
                onClick = {
                    viewModel.save()
                    onContinue()
                },
                enabled = config.baseUrl.isNotBlank()
            )
        }
    }
}
