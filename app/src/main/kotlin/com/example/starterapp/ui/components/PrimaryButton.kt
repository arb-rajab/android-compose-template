package com.example.starterapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

const val PRIMARY_BUTTON_PROGRESS_TAG = "primary_button_progress"

/** A button counts as clickable only when it is enabled and not busy. */
internal fun isButtonClickable(enabled: Boolean, loading: Boolean): Boolean = enabled && !loading

/** Full-width primary button. While [loading] it shows a spinner and ignores clicks. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = isButtonClickable(enabled, loading),
        modifier = modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).testTag(PRIMARY_BUTTON_PROGRESS_TAG),
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}
