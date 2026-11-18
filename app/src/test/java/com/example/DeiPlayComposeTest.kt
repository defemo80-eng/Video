package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeiPlayComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `auth register flow navigates tabs creates channel and opens studio`() {
        composeTestRule.waitForIdle()

        // Initially on AuthScreen
        composeTestRule.onNodeWithTag("tab_register").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("auth_email_input").performScrollTo().performTextInput("ui.tester@deiplay.app")
        composeTestRule.onNodeWithTag("auth_username_input").performScrollTo().performTextInput("uitester")
        composeTestRule.onNodeWithTag("auth_displayname_input").performScrollTo().performTextInput("UI Tester")
        composeTestRule.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("pass1234")
        composeTestRule.onNodeWithTag("auth_submit_btn").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 10_000L) {
            composeTestRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()

        // Search input is visible on HomeScreen
        composeTestRule.onNodeWithTag("home_search_input").assertIsDisplayed()

        // Navigate across main bottom tabs: Shorts -> Subscriptions -> Profile
        composeTestRule.onNodeWithTag("nav_tab_shorts").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_subscriptions").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_profile").performClick()
        composeTestRule.waitForIdle()

        // Create Channel CTA visible
        composeTestRule.onNodeWithTag("create_or_view_channel_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("channel_name_input").performScrollTo().performTextReplacement("DeiPlay UI Canal")
        composeTestRule.onNodeWithTag("channel_handle_input").performScrollTo().performTextReplacement("deiplayui")
        composeTestRule.onNodeWithTag("save_channel_btn").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("channel_subscribe_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("channel_subscribe_btn").assertIsDisplayed().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("channel_unsubscribe_btn").fetchSemanticsNodes().isNotEmpty()
        }

        // Go back to Profile and open DeiPlay Studio
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onAllNodesWithTag("deiplay_studio_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("deiplay_studio_btn").performClick()
        composeTestRule.waitForIdle()
    }
}
