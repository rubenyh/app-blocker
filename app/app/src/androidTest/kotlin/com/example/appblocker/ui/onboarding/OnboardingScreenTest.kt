package com.example.appblocker.ui.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.appblocker.ui.theme.AppBlockerTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun prominentDisclosureAndConsentButtonExist() {
    compose.setContent {
      AppBlockerTheme {
        OnboardingContent(
          isServiceEnabled = false,
          showRestrictedSettingsHelp = true,
          onAgreeAndOpenSettings = {},
          onNotNow = {},
          onOpenAppInfo = {},
        )
      }
    }

    compose.onNodeWithText("Prominent disclosure").assertExists()
    compose.onNodeWithText("Agree and open Accessibility settings").assertExists()
    compose.onNodeWithText("Not now").assertExists()
    compose.onNodeWithText("What this app does").assertExists()
    compose.onNodeWithText("Android 13+ restricted settings").assertExists()
  }
}
