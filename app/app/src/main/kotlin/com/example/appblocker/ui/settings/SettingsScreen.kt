package com.example.appblocker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appblocker.R
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.settings.SettingsRepository
import com.example.appblocker.settings.blockerSettingsDataStore

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current.applicationContext
  val viewModel = viewModel { SettingsViewModel(SettingsRepository(context.blockerSettingsDataStore)) }
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val saveFailed by viewModel.saveFailed.collectAsStateWithLifecycle()
  SettingsScreen(
    settings = settings,
    onYoutubeBlockViewerToggle = viewModel::toggleYoutubeBlockViewer,
    onYoutubeMaskEntryPointsToggle = viewModel::toggleYoutubeMaskEntryPoints,
    onInstagramBlockViewerToggle = viewModel::toggleInstagramBlockViewer,
    onInstagramMaskEntryPointsToggle = viewModel::toggleInstagramMaskEntryPoints,
    onDiagnosticsToggle = viewModel::toggleDiagnostics,
    modifier = modifier,
    saveFailed = saveFailed,
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
  saveFailed: Boolean = false,
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
        SettingSwitchRow(
          stringResource(R.string.local_diagnostics),
          settings.diagnosticsEnabled,
          onDiagnosticsToggle,
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
