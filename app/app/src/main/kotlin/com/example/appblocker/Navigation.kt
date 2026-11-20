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

  LifecycleResumeEffect(Unit) {
    val enabled = isBlockerServiceEnabled(context)
    if (enabled) {
      if (backStack.lastOrNull() != Settings) {
        backStack.clear()
        backStack.add(Settings)
      }
    } else {
      if (backStack.lastOrNull() != Onboarding) {
        backStack.clear()
        backStack.add(Onboarding)
      }
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
