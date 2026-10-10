package com.example.starterapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.starterapp.connectivity.ConnectivityState

/**
 * A persistent, honest online/offline/syncing indicator: the app never silently degrades.
 * Shown when offline, or while queued actions are waiting to sync.
 */
@Composable
fun ConnectivityBanner(
    connectivityState: ConnectivityState,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val visible = connectivityState == ConnectivityState.OFFLINE || pendingCount > 0
    val noun = if (pendingCount == 1) "action" else "actions"
    AnimatedVisibility(visible = visible, modifier = modifier) {
        val (background, label) =
            when {
                connectivityState == ConnectivityState.OFFLINE && pendingCount > 0 ->
                    MaterialTheme.colorScheme.errorContainer to
                        "Offline — $pendingCount $noun queued"
                connectivityState == ConnectivityState.OFFLINE ->
                    MaterialTheme.colorScheme.errorContainer to "Offline"
                pendingCount > 0 ->
                    MaterialTheme.colorScheme.tertiaryContainer to
                        "Syncing $pendingCount queued $noun…"
                else -> MaterialTheme.colorScheme.surface to ""
            }
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(background)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
