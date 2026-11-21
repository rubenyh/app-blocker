package com.example.appblocker.detect

import com.example.appblocker.service.INSTAGRAM_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Test

class InstagramEntryPointsTest {
  @Test fun home() {
    assertEquals(listOf(Bounds(216, 2211, 432, 2337), Bounds(0, 1481, 1080, 2211)),
      instagramEntryPoints(loadInstagramFixture("home")))
  }
  @Test fun explore() = assertTab("explore")
  @Test fun directInbox() = assertTab("direct_inbox")
  @Test fun ownProfile() = assertTab("own_profile")
  @Test fun reelsTab() = assertTab("reels_tab")
  @Test fun otherProfileHasNoBottomBar() = assertNoTab("other_profile")
  @Test fun otherProfilePostsHasNoBottomBar() = assertNoTab("other_profile_posts")
  @Test fun storyViewerHasNoTab() = assertNoTab("story_viewer")
  @Test fun reelFromFeedHasNoTab() = assertNoTab("reel_from_feed")
  @Test fun reelFromExploreHasNoTab() = assertNoTab("reel_from_explore")
  @Test fun reelFromProfileHasNoTab() = assertNoTab("reel_from_profile")

  @Test fun anotherPackageHasNoMask() {
    val snapshot = loadInstagramFixture("home").copy(packageName = "other.app")
    assertEquals(emptyList<Bounds>(), instagramEntryPoints(snapshot))
  }

  @Test fun emptyHasNoMask() {
    assertEquals(emptyList<Bounds>(), instagramEntryPoints(ScreenSnapshot(INSTAGRAM_PACKAGE, emptyList())))
  }

  @Test fun invalidBoundsHaveNoMask() {
    for (bounds in listOf(Bounds(1, 0, 1, 10), Bounds(0, 1, 10, 1), Bounds(10, 10, 0, 0))) {
      val snapshot = ScreenSnapshot(INSTAGRAM_PACKAGE, listOf(
        SnapshotNode("$INSTAGRAM_PACKAGE:id/clips_tab", null, false, bounds, -1),
      ))
      assertEquals(emptyList<Bounds>(), instagramEntryPoints(snapshot))
    }
  }

  private fun assertTab(name: String) {
    val snapshot = loadInstagramFixture(name)
    val capturedBounds = snapshot.nodes.single { it.viewId == "$INSTAGRAM_PACKAGE:id/clips_tab" }.bounds
    assertEquals(Bounds(216, 2211, 432, 2337), capturedBounds)
    assertEquals(listOf(capturedBounds), instagramEntryPoints(snapshot))
  }

  private fun assertNoTab(name: String) {
    assertEquals(emptyList<Bounds>(), instagramEntryPoints(loadInstagramFixture(name)))
  }
}
