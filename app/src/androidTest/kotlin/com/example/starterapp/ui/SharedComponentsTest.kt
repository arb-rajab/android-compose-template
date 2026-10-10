package com.example.starterapp.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.starterapp.ui.components.AppTextField
import com.example.starterapp.ui.components.EmptyState
import com.example.starterapp.ui.components.ErrorState
import com.example.starterapp.ui.components.LOADING_INDICATOR_TAG
import com.example.starterapp.ui.components.LoadingIndicator
import com.example.starterapp.ui.components.PRIMARY_BUTTON_PROGRESS_TAG
import com.example.starterapp.ui.components.PrimaryButton
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Instrumented Compose tests for the shared components, run on the CI emulator. */
class SharedComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingIndicator_showsSpinnerAndMessage() {
        composeTestRule.setContent { LoadingIndicator(message = "Loading items") }

        composeTestRule.onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("Loading items").assertIsDisplayed()
    }

    @Test
    fun errorState_retryInvokesCallback() {
        var retries = 0
        composeTestRule.setContent { ErrorState(message = "Boom", onRetry = { retries++ }) }

        composeTestRule.onNodeWithText("Boom").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun errorState_hidesRetry_withoutCallback() {
        composeTestRule.setContent { ErrorState(message = "Boom") }

        composeTestRule.onAllNodesWithText("Retry").assertCountEquals(0)
    }

    @Test
    fun emptyState_actionInvokesCallback() {
        var actions = 0
        composeTestRule.setContent {
            EmptyState(
                title = "Nothing yet",
                message = "Add your first item",
                actionLabel = "Add",
                onAction = { actions++ }
            )
        }

        composeTestRule.onNodeWithText("Nothing yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add your first item").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add").performClick()

        assertEquals(1, actions)
    }

    @Test
    fun emptyState_hidesAction_withoutCallback() {
        composeTestRule.setContent { EmptyState(title = "Nothing yet", actionLabel = "Add") }

        composeTestRule.onAllNodesWithText("Add").assertCountEquals(0)
    }

    @Test
    fun primaryButton_clickInvokesCallback() {
        var clicks = 0
        composeTestRule.setContent { PrimaryButton(text = "Save", onClick = { clicks++ }) }

        composeTestRule.onNodeWithText("Save").assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun primaryButton_isDisabledAndShowsSpinner_whileLoading() {
        var clicks = 0
        composeTestRule.setContent {
            PrimaryButton(
                text = "Save",
                onClick = { clicks++ },
                loading = true,
                modifier = Modifier.testTag("save")
            )
        }

        composeTestRule.onNodeWithTag("save").assertIsNotEnabled()
        composeTestRule.onNodeWithTag(PRIMARY_BUTTON_PROGRESS_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Save").assertCountEquals(0)
        assertEquals(0, clicks)
    }

    @Test
    fun appTextField_reportsInputAndShowsError() {
        composeTestRule.setContent {
            var text by remember { mutableStateOf("") }
            AppTextField(
                value = text,
                onValueChange = { text = it },
                label = "Name",
                errorText = if (text.isEmpty()) "Required" else null,
                modifier = Modifier.testTag("name")
            )
        }

        composeTestRule.onNodeWithText("Required").assertIsDisplayed()
        composeTestRule.onNodeWithTag("name").performTextInput("Ada")
        composeTestRule.onAllNodesWithText("Required").assertCountEquals(0)
    }
}
