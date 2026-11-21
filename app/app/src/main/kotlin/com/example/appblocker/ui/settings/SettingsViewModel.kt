package com.example.appblocker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appblocker.diagnostics.Diagnostics
import com.example.appblocker.settings.InstagramFeedMode
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.settings.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
  val settings: StateFlow<BlockerSettings?> = repository.settings
    .onEach { Diagnostics.setEnabled(it.diagnosticsEnabled) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val diagnosticEntries = Diagnostics.entries

  fun clearDiagnostics() = Diagnostics.clear()

  private val _saveFailed = MutableStateFlow(false)
  val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

  fun toggleYoutubeBlockViewer() = update { it.copy(youtubeBlockViewer = !it.youtubeBlockViewer) }
  fun toggleYoutubeMaskEntryPoints() = update {
    it.copy(youtubeMaskEntryPoints = !it.youtubeMaskEntryPoints)
  }
  fun toggleInstagramBlockViewer() = update { it.copy(instagramBlockViewer = !it.instagramBlockViewer) }
  fun toggleInstagramMaskEntryPoints() = update {
    it.copy(instagramMaskEntryPoints = !it.instagramMaskEntryPoints)
  }
  fun setInstagramFeedMode(mode: InstagramFeedMode) = update { it.copy(instagramFeedMode = mode) }

  fun toggleDiagnostics() = update { it.copy(diagnosticsEnabled = !it.diagnosticsEnabled) }

  private fun update(transform: (BlockerSettings) -> BlockerSettings) {
    viewModelScope.launch {
      try {
        repository.update(transform)
        _saveFailed.value = false
      } catch (_: IOException) {
        _saveFailed.value = true
      }
    }
  }
}
