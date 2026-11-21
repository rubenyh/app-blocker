package com.example.appblocker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun emptyStoreEmitsDefaults() = runTest {
    val file = tempFolder.newFile("defaults.preferences_pb")
    val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
    assertEquals(BlockerSettings(), SettingsRepository(store).settings.first())
  }

  @Test
  fun updateIsEmittedAndPersistsWhenStoreIsReopened() = runTest {
    val file = tempFolder.newFile("persist.preferences_pb")
    // Android's JVM stubs select renameTo, which cannot replace an existing file on Windows.
    check(file.delete())
    val storeJob = Job()
    val storeScope = CoroutineScope(backgroundScope.coroutineContext + storeJob)
    val expected = BlockerSettings(diagnosticsEnabled = true, instagramFeedMode = InstagramFeedMode.MASK_ALL_MEDIA)
    try {
      val repository = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = storeScope) { file },
      )
      repository.update { expected }
      assertEquals(expected, repository.settings.first())
    } finally {
      storeScope.cancel()
      storeJob.join()
    }
    val reopened = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
    assertEquals(expected, SettingsRepository(reopened).settings.first())
  }

  @Test
  fun eachFieldRoundTripsIndependently() = runTest {
    val defaults = BlockerSettings()
    val cases = listOf(
      defaults.copy(youtubeBlockViewer = false),
      defaults.copy(youtubeMaskEntryPoints = false),
      defaults.copy(instagramBlockViewer = false),
      defaults.copy(instagramMaskEntryPoints = false),
      defaults.copy(diagnosticsEnabled = true),
      *InstagramFeedMode.entries.map { defaults.copy(instagramFeedMode = it) }.toTypedArray(),
    )
    for ((index, expected) in cases.withIndex()) {
      val file = tempFolder.newFile("field-$index.preferences_pb")
      check(file.delete())
      val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
      val repository = SettingsRepository(store)
      repository.update { expected }
      assertEquals(expected, repository.settings.first())
    }
  }

  @Test
  fun ioReadFailureEmitsDefaults() = runTest {
    assertEquals(BlockerSettings(), SettingsRepository(failingStore(IOException())).settings.first())
  }

  @Test(expected = IllegalStateException::class)
  fun programmingErrorsAreNotSwallowed() = runTest {
    SettingsRepository(failingStore(IllegalStateException())).settings.first()
  }

  @Test fun missingAndUnknownModeUseDefault() = runTest {
    val file = tempFolder.newFile("unknown.preferences_pb")
    check(file.delete())
    val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
    val repository = SettingsRepository(store)
    assertEquals(InstagramFeedMode.FOLLOWING_MASK_VIDEOS, repository.settings.first().instagramFeedMode)
    store.edit { it[stringPreferencesKey("instagram_feed_mode")] = "FUTURE_MODE" }
    assertEquals(InstagramFeedMode.FOLLOWING_MASK_VIDEOS, repository.settings.first().instagramFeedMode)
  }

  private fun failingStore(error: Exception) = object : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw error }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
      throw error
    }
  }
}
