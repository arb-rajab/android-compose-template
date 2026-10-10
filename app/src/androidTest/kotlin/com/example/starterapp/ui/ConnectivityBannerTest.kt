package com.example.starterapp.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.example.starterapp.connectivity.ConnectivityState
import com.example.starterapp.ui.components.ConnectivityBanner
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented Compose UI test, run on a hosted emulator in CI (see the
 * `instrumented-tests` job in `.github/workflows/android-ci.yml`).
 */
class ConnectivityBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun banner_showsOfflineMessage_whenOffline() {
        composeTestRule.setContent {
            ConnectivityBanner(connectivityState = ConnectivityState.OFFLINE, pendingCount = 0)
        }

        composeTestRule.onNodeWithText("Offline").assertIsDisplayed()
    }

    @Test
    fun banner_showsQueuedCount_whenOfflineWithPendingActions() {
        composeTestRule.setContent {
            ConnectivityBanner(connectivityState = ConnectivityState.OFFLINE, pendingCount = 3)
        }

        composeTestRule.onNodeWithText("Offline — 3 actions queued").assertIsDisplayed()
    }

    @Test
    fun banner_isHidden_whenOnlineWithNothingPending() {
        composeTestRule.setContent {
            ConnectivityBanner(connectivityState = ConnectivityState.ONLINE, pendingCount = 0)
        }

        composeTestRule.onAllNodesWithText("Offline").assertCountEquals(0)
    }
}
