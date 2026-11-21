package com.example.appblocker.service

import com.example.appblocker.settings.BlockerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetAppsTest {
  @Test
  fun eachAppRequiresAtLeastOneOfItsOwnToggles() {
    for (viewer in listOf(false, true)) {
      for (mask in listOf(false, true)) {
        val youtube = BlockerSettings(
          youtubeBlockViewer = viewer,
          youtubeMaskEntryPoints = mask,
          instagramBlockViewer = false,
        )
        assertEquals(viewer || mask, isEnabledFor(YOUTUBE_PACKAGE, youtube))
        assertEquals(viewer, youtube.viewerEnabledFor(YOUTUBE_PACKAGE))
        assertEquals(mask, youtube.maskEnabledFor(YOUTUBE_PACKAGE))
        assertFalse(youtube.viewerEnabledFor(INSTAGRAM_PACKAGE))
        assertFalse(youtube.maskEnabledFor(INSTAGRAM_PACKAGE))
        assertFalse(isEnabledFor(INSTAGRAM_PACKAGE, youtube))

        val instagram = BlockerSettings(
          youtubeBlockViewer = false,
          instagramBlockViewer = viewer,
          instagramMaskEntryPoints = mask,
        )
        assertEquals(viewer || mask, isEnabledFor(INSTAGRAM_PACKAGE, instagram))
        assertEquals(viewer, instagram.viewerEnabledFor(INSTAGRAM_PACKAGE))
        assertEquals(mask, instagram.maskEnabledFor(INSTAGRAM_PACKAGE))
        assertFalse(instagram.viewerEnabledFor(YOUTUBE_PACKAGE))
        assertFalse(instagram.maskEnabledFor(YOUTUBE_PACKAGE))
        assertFalse(isEnabledFor(YOUTUBE_PACKAGE, instagram))
      }
    }
  }

  @Test
  fun nullUnknownAndSimilarPackagesAreRejected() {
    for (pkg in listOf(null, "", "com.android.settings", "$YOUTUBE_PACKAGE.extra", "COM.INSTAGRAM.ANDROID")) {
      assertFalse(isEnabledFor(pkg, BlockerSettings()))
      assertFalse(BlockerSettings().viewerEnabledFor(pkg))
      assertFalse(BlockerSettings().maskEnabledFor(pkg))
    }
  }

  @Test
  fun nonStringPackageNamesAreMatchedByContent() {
    assertTrue(isEnabledFor(StringBuilder(YOUTUBE_PACKAGE), BlockerSettings()))
    assertTrue(isEnabledFor(StringBuilder(INSTAGRAM_PACKAGE), BlockerSettings()))
  }

  @Test
  fun diagnosticsDoesNotEnableDisabledApps() {
    val settings = BlockerSettings(
      youtubeBlockViewer = false,
      instagramBlockViewer = false,
      diagnosticsEnabled = true,
    )
    assertFalse(isEnabledFor(YOUTUBE_PACKAGE, settings))
    assertFalse(isEnabledFor(INSTAGRAM_PACKAGE, settings))
  }
}
