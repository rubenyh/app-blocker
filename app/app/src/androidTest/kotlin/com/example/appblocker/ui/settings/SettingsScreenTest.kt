package com.example.appblocker.ui.settings

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotEnabled
import com.example.appblocker.settings.InstagramFeedMode
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.ui.theme.AppBlockerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun sectionsAndSwitchRowsInvokeTheirCallbacks() {
    val clicks = IntArray(5)
    compose.setContent {
      AppBlockerTheme {
        SettingsScreen(
          settings = BlockerSettings(),
          onYoutubeBlockViewerToggle = { clicks[0]++ },
          onYoutubeMaskEntryPointsToggle = { clicks[1]++ },
          onInstagramBlockViewerToggle = { clicks[2]++ },
          onInstagramMaskEntryPointsToggle = { clicks[3]++ },
          onDiagnosticsToggle = { clicks[4]++ },
        )
      }
    }
    compose.onNodeWithText("YouTube").assertExists()
    compose.onNodeWithText("Instagram").assertExists()
    compose.onNodeWithText("Block Shorts viewer").assertIsOn().performClick()
    compose.onAllNodesWithText("Hide entry points (masking)")[0]
      .assertIsOn().performClick()
    compose.onNodeWithText("Block Reels viewer").performScrollTo().assertIsOn().performClick()
    compose.onAllNodesWithText("Hide entry points (masking)")[1]
      .performScrollTo().assertIsOn().performClick()
    compose.onNodeWithText("Local diagnostics (opt-in)")
      .performScrollTo().assertIsOff().performClick()
    compose.runOnIdle { assertEquals(listOf(1, 1, 1, 1, 1), clicks.toList()) }
  }
  @Test fun feedModesSelectAndDisableWithMasking() {
    var chosen: InstagramFeedMode? = null
    compose.setContent {
      AppBlockerTheme {
        SettingsScreen(
          settings = BlockerSettings(),
          onYoutubeBlockViewerToggle = {}, onYoutubeMaskEntryPointsToggle = {},
          onInstagramBlockViewerToggle = {}, onInstagramMaskEntryPointsToggle = {},
          onDiagnosticsToggle = {}, onInstagramFeedModeChange = { chosen = it },
        )
      }
    }
    compose.onNodeWithText("Following feed, hide videos").performScrollTo().assertIsSelected()
    compose.onNodeWithText("Following feed, show everything").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(InstagramFeedMode.FOLLOWING_SHOW_ALL, chosen) }
    compose.onNodeWithText("Hide all feed media").performScrollTo().performClick()
    compose.runOnIdle { assertEquals(InstagramFeedMode.MASK_ALL_MEDIA, chosen) }
  }

  @Test fun feedModesDisabledWhenMaskingOff() {
    compose.setContent {
      AppBlockerTheme {
        SettingsScreen(
          settings = BlockerSettings(instagramMaskEntryPoints = false),
          onYoutubeBlockViewerToggle = {}, onYoutubeMaskEntryPointsToggle = {},
          onInstagramBlockViewerToggle = {}, onInstagramMaskEntryPointsToggle = {},
          onDiagnosticsToggle = {},
        )
      }
    }
    for (label in listOf("Following feed, hide videos", "Following feed, show everything", "Hide all feed media")) {
      compose.onNodeWithText(label).performScrollTo().assertIsNotEnabled()
    }
  }

}
