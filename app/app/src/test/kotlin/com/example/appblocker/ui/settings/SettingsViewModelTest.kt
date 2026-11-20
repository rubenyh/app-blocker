package com.example.appblocker.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.example.appblocker.diagnostics.DiagnosticEntry
import com.example.appblocker.diagnostics.Diagnostics
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.settings.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
  @Test
  fun togglesPreserveOtherFieldsAndFailedSavesCanBeRetried() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val lifecycle = ViewModelStore()
    try {
      var failWrites = false
      val store = object : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
          if (failWrites) throw IOException("test write failure")
          return transform(data.value).also { data.value = it }
        }
      }
      val viewModel = SettingsViewModel(SettingsRepository(store))
      lifecycle.put("settings", viewModel)
      assertNull(viewModel.settings.value)
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.settings.collect {} }
      runCurrent()
      assertEquals(BlockerSettings(), viewModel.settings.value)

      viewModel.toggleYoutubeBlockViewer()
      viewModel.toggleYoutubeMaskEntryPoints()
      viewModel.toggleInstagramBlockViewer()
      viewModel.toggleInstagramMaskEntryPoints()
      viewModel.toggleDiagnostics()
      runCurrent()
      val toggled = BlockerSettings(false, true, false, true, true)
      assertEquals(toggled, viewModel.settings.value)
      val entry = DiagnosticEntry(1L, "com.google.android.youtube", null, null, "None", "Nothing", 0, false)
      Diagnostics.record(entry)
      assertEquals(listOf(entry), viewModel.diagnosticEntries.value)
      viewModel.clearDiagnostics()
      assertTrue(viewModel.diagnosticEntries.value.isEmpty())
      Diagnostics.record(entry)

      failWrites = true
      viewModel.toggleDiagnostics()
      runCurrent()
      assertTrue(viewModel.saveFailed.value)
      assertEquals(toggled, viewModel.settings.value)
      assertEquals(listOf(entry), viewModel.diagnosticEntries.value)

      failWrites = false
      viewModel.toggleDiagnostics()
      runCurrent()
      assertFalse(viewModel.saveFailed.value)
      assertEquals(toggled.copy(diagnosticsEnabled = false), viewModel.settings.value)
      assertTrue(viewModel.diagnosticEntries.value.isEmpty())
    } finally {
      Diagnostics.setEnabled(false)
      lifecycle.clear()
      Dispatchers.resetMain()
    }
  }
}
