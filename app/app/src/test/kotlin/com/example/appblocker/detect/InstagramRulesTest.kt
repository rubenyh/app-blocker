package com.example.appblocker.detect

import com.example.appblocker.service.INSTAGRAM_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramRulesTest {
  @Test fun viewerFromTab() = assertViewer("reels_tab")
  @Test fun viewerFromFeed() = assertViewer("reel_from_feed")
  @Test fun viewerFromExplore() = assertViewer("reel_from_explore")
  @Test fun viewerFromProfile() = assertViewer("reel_from_profile")
  @Test fun home() = assertNone("home")
  @Test fun explore() = assertNone("explore")
  @Test fun directInbox() = assertNone("direct_inbox")
  @Test fun ownProfile() = assertNone("own_profile")
  @Test fun otherProfile() = assertNone("other_profile")
  @Test fun otherProfilePosts() = assertNone("other_profile_posts")
  @Test fun storyViewer() = assertNone("story_viewer")

  @Test fun emptyIsUnknown() {
    assertEquals(Detection.Unknown, detectInstagram(ScreenSnapshot(INSTAGRAM_PACKAGE, emptyList())))
  }

  @Test fun zeroHeightRootIsUnknown() {
    assertEquals(Detection.Unknown, detectInstagram(synthetic(rootHeight = 0)))
  }

  @Test fun smallVideoIsNotViewer() {
    assertEquals(Detection.None, detectInstagram(synthetic(containerHeight = 499)))
  }

  @Test fun exactlyHalfHeightIsViewer() {
    assertEquals(Detection.Viewer("ig-clips-viewer-v1"), detectInstagram(synthetic(containerHeight = 500)))
  }

  @Test fun containerNeedsPagerAncestor() {
    assertEquals(Detection.None, detectInstagram(synthetic(containerParent = 0)))
  }

  @Test fun fullScreenStoryIsNotViewer() {
    val story = synthetic().copy(nodes = listOf(
      SnapshotNode("$INSTAGRAM_PACKAGE:id/reel_viewer_root", null, false, Bounds(0, 0, 1080, 1000), -1),
    ))
    assertEquals(Detection.None, detectInstagram(story))
  }

  @Test fun anotherPackageIsNotViewer() {
    assertEquals(Detection.None, detectInstagram(synthetic().copy(packageName = "other.app")))
  }

  @Test fun fixturePreservesMissingClassesAndParentIndices() {
    val snapshot = loadInstagramFixture("own_profile")
    assertEquals(INSTAGRAM_PACKAGE, snapshot.packageName)
    assertEquals(-1, snapshot.nodes.first().parent)
    assertTrue(snapshot.nodes.any { it.viewId != null && it.className == null })
    assertTrue(snapshot.nodes.withIndex().drop(1).all { (index, node) -> node.parent in 0 until index })
  }

  private fun assertViewer(name: String) {
    assertEquals(Detection.Viewer("ig-clips-viewer-v1"), detectInstagram(loadInstagramFixture(name)))
  }

  private fun assertNone(name: String) {
    assertEquals(Detection.None, detectInstagram(loadInstagramFixture(name)))
  }

  private fun synthetic(rootHeight: Int = 1000, containerHeight: Int = 1000, containerParent: Int = 1) =
    ScreenSnapshot(INSTAGRAM_PACKAGE, listOf(
      SnapshotNode(null, null, false, Bounds(0, 0, 1080, rootHeight), -1),
      SnapshotNode("$INSTAGRAM_PACKAGE:id/clips_viewer_view_pager", null, false,
        Bounds(0, 0, 1080, 1000), 0),
      SnapshotNode("$INSTAGRAM_PACKAGE:id/clips_video_container", null, false,
        Bounds(0, 0, 1080, containerHeight), containerParent),
    ))
}
