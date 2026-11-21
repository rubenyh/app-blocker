package com.example.appblocker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import com.example.appblocker.settings.InstagramFeedMode
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appblocker.R
import com.example.appblocker.diagnostics.DiagnosticEntry
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.settings.SettingsRepository
import com.example.appblocker.settings.blockerSettingsDataStore
import com.example.appblocker.ui.onboarding.isBlockerServiceEnabled
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
  onOpenOnboarding: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val viewModel = viewModel { SettingsViewModel(SettingsRepository(context.applicationContext.blockerSettingsDataStore)) }
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
  val diagnosticEntries by viewModel.diagnosticEntries.collectAsStateWithLifecycle()

  var isServiceEnabled by remember { mutableStateOf(isBlockerServiceEnabled(context)) }
  LifecycleResumeEffect(Unit) {
    isServiceEnabled = isBlockerServiceEnabled(context)
    onPauseOrDispose {}
  }

  SettingsScreen(
    settings = settings,
    isServiceEnabled = isServiceEnabled,
    onOpenOnboarding = onOpenOnboarding,
    onYoutubeBlockViewerToggle = viewModel::toggleYoutubeBlockViewer,
    onYoutubeMaskEntryPointsToggle = viewModel::toggleYoutubeMaskEntryPoints,
    onInstagramBlockViewerToggle = viewModel::toggleInstagramBlockViewer,
    onInstagramMaskEntryPointsToggle = viewModel::toggleInstagramMaskEntryPoints,
    onDiagnosticsToggle = viewModel::toggleDiagnostics,
    onInstagramFeedModeChange = viewModel::setInstagramFeedMode,
    modifier = modifier,
    saveFailed = saveFailed,
    diagnosticEntries = diagnosticEntries,
    onClearDiagnostics = viewModel::clearDiagnostics,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
  settings: BlockerSettings?,
  onYoutubeBlockViewerToggle: () -> Unit,
  onYoutubeMaskEntryPointsToggle: () -> Unit,
  onInstagramBlockViewerToggle: () -> Unit,
  onInstagramMaskEntryPointsToggle: () -> Unit,
  onDiagnosticsToggle: () -> Unit,
  modifier: Modifier = Modifier,
  isServiceEnabled: Boolean = false,
  onOpenOnboarding: () -> Unit = {},
  saveFailed: Boolean = false,
  diagnosticEntries: List<DiagnosticEntry> = emptyList(),
  onClearDiagnostics: () -> Unit = {},
  onInstagramFeedModeChange: (InstagramFeedMode) -> Unit = {},
) {
  Scaffold(
    modifier = modifier,
    topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
  ) { innerPadding ->
    if (settings == null) {
      Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    } else {
      Column(Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
        if (saveFailed) {
          Text(
            stringResource(R.string.settings_save_failed),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error,
          )
        }
        ServiceStatusRow(
          isServiceEnabled = isServiceEnabled,
          onClick = {
            if (!isServiceEnabled) {
              onOpenOnboarding()
            }
          },
        )
        SectionTitle(stringResource(R.string.youtube))
        SettingSwitchRow(
          stringResource(R.string.block_shorts_viewer),
          settings.youtubeBlockViewer,
          onYoutubeBlockViewerToggle,
        )
        SettingSwitchRow(
          stringResource(R.string.hide_entry_points),
          settings.youtubeMaskEntryPoints,
          onYoutubeMaskEntryPointsToggle,
          stringResource(R.string.masking_description),
        )
        SectionTitle(stringResource(R.string.instagram))
        SettingSwitchRow(
          stringResource(R.string.block_reels_viewer),
          settings.instagramBlockViewer,
          onInstagramBlockViewerToggle,
        )
        SettingSwitchRow(
          stringResource(R.string.hide_entry_points),
          settings.instagramMaskEntryPoints,
          onInstagramMaskEntryPointsToggle,
          stringResource(R.string.masking_description),
        )
        Column(Modifier.selectableGroup()) {
          for ((mode, label) in listOf(
            InstagramFeedMode.FOLLOWING_MASK_VIDEOS to R.string.instagram_feed_hide_videos,
            InstagramFeedMode.FOLLOWING_SHOW_ALL to R.string.instagram_feed_show_all,
            InstagramFeedMode.MASK_ALL_MEDIA to R.string.instagram_feed_hide_all,
          )) {
            Row(
              Modifier.fillMaxWidth().selectable(
                selected = settings.instagramFeedMode == mode,
                enabled = settings.instagramMaskEntryPoints,
                role = Role.RadioButton,
                onClick = { onInstagramFeedModeChange(mode) },
              ).padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              RadioButton(selected = settings.instagramFeedMode == mode, onClick = null,
                enabled = settings.instagramMaskEntryPoints)
              Text(stringResource(label), color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (settings.instagramMaskEntryPoints) 1f else 0.38f))
            }
          }
        }
        SettingSwitchRow(
          stringResource(R.string.local_diagnostics),
          settings.diagnosticsEnabled,
          onDiagnosticsToggle,
        )
        if (settings.diagnosticsEnabled) {
          SectionTitle(stringResource(R.string.diagnostics_title))
          Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              stringResource(R.string.diagnostics_count, diagnosticEntries.size),
              Modifier.weight(1f),
            )
            TextButton(onClick = onClearDiagnostics) { Text(stringResource(R.string.diagnostics_clear)) }
          }
          val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
          for (entry in diagnosticEntries.takeLast(50).asReversed()) {
            Text(
              stringResource(
                R.string.diagnostics_entry,
                timeFormat.format(Date(entry.timeMillis)),
                entry.packageName.substringAfterLast('.'),
                entry.appVersion ?: stringResource(R.string.diagnostics_unknown_version),
                entry.detection + (entry.ruleId?.let { "/$it" } ?: ""),
                entry.action,
                entry.masks,
                if (entry.capHit) " " + stringResource(R.string.diagnostics_cap) else "",
              ),
              Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ServiceStatusRow(
  isServiceEnabled: Boolean,
  onClick: () -> Unit,
) {
  val clickableModifier = if (!isServiceEnabled) {
    Modifier.clickable(role = Role.Button, onClick = onClick)
  } else {
    Modifier
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .then(clickableModifier)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(Modifier.weight(1f)) {
      val statusText = stringResource(if (isServiceEnabled) R.string.status_on else R.string.status_off)
      Text(
        text = stringResource(R.string.protection_service_status, statusText),
        style = MaterialTheme.typography.bodyLarge,
        color = if (isServiceEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
      )
      if (!isServiceEnabled) {
        Text(
          text = stringResource(R.string.protection_service_tap_to_enable),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(title, Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SettingSwitchRow(
  title: String,
  checked: Boolean,
  onToggle: () -> Unit,
  supportingText: String? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth()
      .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (supportingText != null) {
        Text(supportingText, style = MaterialTheme.typography.bodyMedium)
      }
    }
    Switch(checked = checked, onCheckedChange = null)
  }
}
