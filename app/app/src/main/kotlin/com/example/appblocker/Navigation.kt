package com.example.appblocker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.appblocker.ui.onboarding.OnboardingScreen
import com.example.appblocker.ui.onboarding.isBlockerServiceEnabled
import com.example.appblocker.ui.settings.SettingsScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val initialEnabled = remember { isBlockerServiceEnabled(context) }
  val backStack = rememberNavBackStack(if (initialEnabled) Settings else Onboarding)

  // Returning from system settings with the service enabled finishes onboarding.
  // A disabled service never forces onboarding on resume, so "Not now" is respected;
  // the Settings status row offers the way back.
  LifecycleResumeEffect(Unit) {
    if (backStack.lastOrNull() == Onboarding && isBlockerServiceEnabled(context)) {
      backStack.clear()
      backStack.add(Settings)
    }
    onPauseOrDispose {}
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Onboarding> {
          OnboardingScreen(
            onNavigateToSettings = {
              backStack.clear()
              backStack.add(Settings)
            },
          )
        }
        entry<Settings> {
          SettingsScreen(
            onOpenOnboarding = {
              backStack.add(Onboarding)
            },
          )
        }
      },
  )
}
