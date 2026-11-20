package com.example.appblocker.detect

import com.example.appblocker.service.YOUTUBE_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeRulesTest {
  @Test fun viewerFromTab() = assertViewer("shorts_viewer_tab")
  @Test fun viewerFromSearch() = assertViewer("shorts_viewer_from_search")
  @Test fun home() = assertNone("home")
  @Test fun searchResults() = assertNone("search_results")
  @Test fun normalVideo() = assertNone("normal_video")
  @Test fun channelWithMiniplayer() = assertNone("channel")

  @Test fun emptyIsUnknown() {
    assertEquals(Detection.Unknown, detectYouTube(ScreenSnapshot(YOUTUBE_PACKAGE, emptyList())))
  }

  @Test fun zeroHeightRootIsUnknown() {
    assertEquals(Detection.Unknown, detectYouTube(synthetic(rootHeight = 0)))
  }

  @Test fun containerNeedsRecyclerAncestor() {
    assertEquals(Detection.None, detectYouTube(synthetic(containerParent = 0)))
  }

  @Test fun previewTileIsNotViewer() {
    assertEquals(Detection.None, detectYouTube(synthetic(containerHeight = 499)))
  }

  @Test fun exactlyHalfHeightIsViewer() {
    assertEquals(Detection.Viewer("yt-reel-player-v1"), detectYouTube(synthetic(containerHeight = 500)))
  }

  @Test fun anotherPackageIsNotViewer() {
    assertEquals(Detection.None, detectYouTube(synthetic().copy(packageName = "other.app")))
  }

  @Test fun fixtureParentsPreserveAncestryAndMetadata() {
    val snapshot = loadYouTubeFixture("shorts_viewer_tab")
    assertEquals(YOUTUBE_PACKAGE, snapshot.packageName)
    assertEquals(-1, snapshot.nodes.first().parent)
    assertEquals(Bounds(0, 0, 1080, 2400), snapshot.nodes.first().bounds)
    assertTrue(snapshot.nodes.withIndex().drop(1).all { (index, node) -> node.parent in 0 until index })
    val container = snapshot.nodes.first { it.viewId == "$YOUTUBE_PACKAGE:id/reel_player_page_container" }
    assertEquals("$YOUTUBE_PACKAGE:id/reel_recycler", snapshot.nodes[container.parent].viewId)
  }

  private fun assertViewer(name: String) {
    assertEquals(Detection.Viewer("yt-reel-player-v1"), detectYouTube(loadYouTubeFixture(name)))
  }

  private fun assertNone(name: String) {
    assertEquals(Detection.None, detectYouTube(loadYouTubeFixture(name)))
  }

  private fun synthetic(rootHeight: Int = 1000, containerHeight: Int = 1000, containerParent: Int = 1) =
    ScreenSnapshot(YOUTUBE_PACKAGE, listOf(
      SnapshotNode(null, null, false, Bounds(0, 0, 1080, rootHeight), -1),
      SnapshotNode("$YOUTUBE_PACKAGE:id/reel_recycler", null, false, Bounds(0, 0, 1080, 1000), 0),
      SnapshotNode("$YOUTUBE_PACKAGE:id/reel_player_page_container", null, false,
        Bounds(0, 0, 1080, containerHeight), containerParent),
    ))
}
