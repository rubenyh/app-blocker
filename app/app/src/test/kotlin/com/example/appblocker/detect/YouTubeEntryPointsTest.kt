package com.example.appblocker.detect

import com.example.appblocker.service.YOUTUBE_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeEntryPointsTest {
  private val shortsBounds = Bounds(270, 2211, 540, 2337)

  @Test fun home() = assertTab("home")
  @Test fun channel() = assertTab("channel")
  @Test fun searchResults() = assertTab("search_results")
  @Test fun viewerTab() = assertTab("shorts_viewer_tab")

  @Test fun normalVideoHasNoTab() {
    assertEquals(emptyList<Bounds>(), youTubeEntryPoints(loadYouTubeFixture("normal_video")))
  }

  @Test fun threeButtonsAreUnknown() {
    assertEquals(emptyList<Bounds>(), youTubeEntryPoints(bar(3)))
  }

  @Test fun fiveButtonsUseSecondButton() {
    assertEquals(listOf(shortsBounds), youTubeEntryPoints(bar(5)))
  }

  @Test fun anotherPackageHasNoMask() {
    assertEquals(emptyList<Bounds>(), youTubeEntryPoints(bar(4).copy(packageName = "other.app")))
  }

  @Test fun emptyIsUnknown() {
    assertEquals(emptyList<Bounds>(), youTubeEntryPoints(ScreenSnapshot(YOUTUBE_PACKAGE, emptyList())))
  }

  @Test fun nestedButtonsDoNotCount() {
    val snapshot = bar(3)
    val nested = SnapshotNode(null, "android.widget.Button", false, shortsBounds, 2)
    assertEquals(emptyList<Bounds>(), youTubeEntryPoints(snapshot.copy(nodes = snapshot.nodes + nested)))
  }

  @Test fun invalidBoundsHaveNoMask() {
    val snapshot = bar(4)
    val nodes = snapshot.nodes.toMutableList()
    nodes[3] = nodes[3].copy(bounds = Bounds(270, 2211, 270, 2337))
    assertEquals(emptyList<Bounds>(), youTubeEntryPoints(snapshot.copy(nodes = nodes)))
  }

  private fun assertTab(name: String) {
    assertEquals(listOf(shortsBounds), youTubeEntryPoints(loadYouTubeFixture(name)))
  }

  private fun bar(buttonCount: Int): ScreenSnapshot {
    val bounds = Bounds(0, 2211, 1080, 2337)
    return ScreenSnapshot(YOUTUBE_PACKAGE, listOf(
      SnapshotNode("$YOUTUBE_PACKAGE:id/pivot_bar", "android.widget.HorizontalScrollView", false, bounds, -1),
      SnapshotNode(null, "android.widget.LinearLayout", false, bounds, 0),
    ) + List(buttonCount) { index ->
      SnapshotNode(null, "android.widget.Button", false,
        Bounds(index * 270, 2211, (index + 1) * 270, 2337), 1)
    })
  }
}
