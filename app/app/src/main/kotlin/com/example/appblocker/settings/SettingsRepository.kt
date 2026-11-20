package com.example.appblocker.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.blockerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "blocker_settings",
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
  val settings: Flow<BlockerSettings> = dataStore.data
    .catch { error ->
      if (error is IOException) emit(emptyPreferences()) else throw error
    }
    .map { it.toSettings() }

  suspend fun update(transform: (BlockerSettings) -> BlockerSettings) {
    dataStore.edit { preferences ->
      val settings = transform(preferences.toSettings())
      preferences[youtubeBlockViewer] = settings.youtubeBlockViewer
      preferences[youtubeMaskEntryPoints] = settings.youtubeMaskEntryPoints
      preferences[instagramBlockViewer] = settings.instagramBlockViewer
      preferences[instagramMaskEntryPoints] = settings.instagramMaskEntryPoints
      preferences[diagnosticsEnabled] = settings.diagnosticsEnabled
    }
  }

  private fun Preferences.toSettings(): BlockerSettings = BlockerSettings(
    youtubeBlockViewer = this[youtubeBlockViewer] ?: true,
    youtubeMaskEntryPoints = this[youtubeMaskEntryPoints] ?: false,
    instagramBlockViewer = this[instagramBlockViewer] ?: true,
    instagramMaskEntryPoints = this[instagramMaskEntryPoints] ?: false,
    diagnosticsEnabled = this[diagnosticsEnabled] ?: false,
  )

  private companion object {
    val youtubeBlockViewer = booleanPreferencesKey("youtube_block_viewer")
    val youtubeMaskEntryPoints = booleanPreferencesKey("youtube_mask_entry_points")
    val instagramBlockViewer = booleanPreferencesKey("instagram_block_viewer")
    val instagramMaskEntryPoints = booleanPreferencesKey("instagram_mask_entry_points")
    val diagnosticsEnabled = booleanPreferencesKey("diagnostics_enabled")
  }
}
