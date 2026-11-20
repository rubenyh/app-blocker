package com.example.appblocker.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.appblocker.R

@Composable
fun OnboardingScreen(
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var isServiceEnabled by remember { mutableStateOf(isBlockerServiceEnabled(context)) }

  LifecycleResumeEffect(Unit) {
    isServiceEnabled = isBlockerServiceEnabled(context)
    onPauseOrDispose {}
  }

  OnboardingContent(
    isServiceEnabled = isServiceEnabled,
    showRestrictedSettingsHelp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
    onAgreeAndOpenSettings = {
      val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
      context.startActivity(intent)
    },
    onNotNow = onNavigateToSettings,
    onOpenAppInfo = {
      val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
      )
      context.startActivity(intent)
    },
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingContent(
  isServiceEnabled: Boolean,
  showRestrictedSettingsHelp: Boolean,
  onAgreeAndOpenSettings: () -> Unit,
  onNotNow: () -> Unit,
  onOpenAppInfo: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.onboarding_title),
            modifier = Modifier.semantics { heading() },
          )
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // 1. What this app does
      SectionHeading(stringResource(R.string.onboarding_what_it_does_title))
      Text(
        text = stringResource(R.string.onboarding_what_it_does_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // 2. Prominent disclosure
      SectionHeading(stringResource(R.string.onboarding_disclosure_title))
      Text(
        text = stringResource(R.string.onboarding_disclosure_intro),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      DisclosurePoint(stringResource(R.string.onboarding_disclosure_point_target_apps))
      DisclosurePoint(stringResource(R.string.onboarding_disclosure_point_leave_detection))
      DisclosurePoint(stringResource(R.string.onboarding_disclosure_point_structure))
      DisclosurePoint(stringResource(R.string.onboarding_disclosure_point_actions))
      DisclosurePoint(stringResource(R.string.onboarding_disclosure_point_privacy))
      DisclosurePoint(stringResource(R.string.onboarding_disclosure_point_settings))

      // 3. Affirmative consent
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onAgreeAndOpenSettings,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.onboarding_consent_agree_button),
            style = MaterialTheme.typography.labelLarge,
          )
        }
        OutlinedButton(
          onClick = onNotNow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.onboarding_consent_not_now_button),
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      // 4. Restricted settings help (Android 13+)
      if (showRestrictedSettingsHelp) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
          ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = stringResource(R.string.onboarding_restricted_settings_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.semantics { heading() },
            )
            Text(
              text = stringResource(R.string.onboarding_restricted_settings_text),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
              onClick = onOpenAppInfo,
              modifier = Modifier.align(Alignment.End),
            ) {
              Text(stringResource(R.string.onboarding_restricted_settings_button))
            }
          }
        }
      }

      // 5. Status
      val statusText = stringResource(if (isServiceEnabled) R.string.status_on else R.string.status_off)
      Text(
        text = stringResource(R.string.onboarding_status_label, statusText),
        style = MaterialTheme.typography.titleSmall,
        color = if (isServiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(vertical = 4.dp),
      )
    }
  }
}

@Composable
private fun SectionHeading(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.semantics { heading() },
  )
}

@Composable
private fun DisclosurePoint(text: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = "•",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
