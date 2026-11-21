package com.example.appblocker.detect

import com.example.appblocker.service.INSTAGRAM_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramFeedVideoPostsTest {
  @Test fun scroll1() = assertCapturedVideos(1)
  @Test fun scroll2() = assertCapturedVideos(2)
  @Test fun scroll3() = assertCapturedVideos(3)
  @Test fun scroll4() = assertCapturedVideos(4)
  @Test fun scroll5() = assertCapturedVideos(5)
  @Test fun scroll6() = assertCapturedVideos(6)
  @Test fun scroll7() = assertCapturedVideos(7)
  @Test fun scroll8() = assertCapturedVideos(8)
  @Test fun scroll9() = assertCapturedVideos(9)
  @Test fun scroll10() = assertCapturedVideos(10)

  @Test fun homeVideo() {
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(loadInstagramFixture("home")))
  }

  @Test fun otherScreensHaveNoFeedMasks() {
    for (name in listOf("explore", "direct_inbox", "own_profile", "other_profile",
      "other_profile_posts", "reels_tab", "reel_from_feed", "reel_from_explore",
      "reel_from_profile", "story_viewer")) {
      assertEquals(name, emptyList<Bounds>(), instagramFeedVideoPosts(loadInstagramFixture(name)))
    }
  }

  @Test fun clipsToFeedOnAllSides() {
    assertEquals(listOf(Bounds(0, 63, 1080, 2211)),
      instagramFeedVideoPosts(feed(Bounds(-10, -20, 1100, 2400))))
  }

  @Test fun clipsBelowAllTopActions() {
    val s = feed(Bounds(0, 63, 1080, 500))
    val nodes = s.nodes + listOf(
      node("action_bar_left_button", Bounds(0, 63, 100, 210), 1),
      node("action_bar_title_view", Bounds(100, 63, 900, 190), 1),
      node("notification", Bounds(900, 63, 1080, 220), 1),
    )
    assertEquals(listOf(Bounds(0, 220, 1080, 500)), instagramFeedVideoPosts(s.copy(nodes = nodes)))
  }

  @Test fun dropsLessThan24PixelsAfterClipping() {
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(feed(Bounds(0, 0, 1080, 86))))
    assertEquals(listOf(Bounds(0, 63, 1080, 87)),
      instagramFeedVideoPosts(feed(Bounds(0, 0, 1080, 87))))
  }

  @Test fun posterDoesNotSuppressPlaybackSignal() {
    val s = feed(Bounds(0, 63, 1080, 500))
    assertEquals(listOf(Bounds(0, 63, 1080, 500)), instagramFeedVideoPosts(s.copy(nodes = s.nodes + listOf(
      node(null, Bounds(0, 63, 1080, 500), 4),
      node("row_feed_photo_imageview", Bounds(0, 63, 1080, 500), 5),
    ))))
  }

  @Test fun missingPlaybackSignalIsNotVideo() {
    val s = feed(Bounds(0, 63, 1080, 500))
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(s.copy(nodes = s.nodes.dropLast(1))))
  }

  @Test fun nestedVideoContainersReturnOnlyOuterBounds() {
    val s = feed(Bounds(0, 63, 1080, 500))
    val nodes = s.nodes + listOf(
      node("video_container", Bounds(100, 100, 900, 400), 3),
      node("media_group", Bounds(100, 100, 900, 400), 5),
    )
    assertEquals(listOf(Bounds(0, 63, 1080, 500)), instagramFeedVideoPosts(s.copy(nodes = nodes)))
  }

  @Test fun missingListHasNoMasks() {
    val s = feed(Bounds(0, 0, 1080, 2400))
    val nodes = s.nodes.toMutableList().apply { this[2] = this[2].copy(viewId = null) }
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(s.copy(nodes = nodes)))
  }

  @Test fun noFeedAreaOrWrongPackageHasNoMasks() {
    val s = feed(Bounds(0, 63, 1080, 500))
    val nodes = s.nodes.toMutableList().apply { this[2] = this[2].copy(viewId = null) }
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(s.copy(nodes = nodes)))
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(s.copy(packageName = "other.app")))
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(s.copy(nodes = emptyList())))
  }

  @Test fun mediaOutsideFeedIsIgnored() {
    val s = feed(Bounds(0, 63, 1080, 500))
    val nodes = s.nodes.toMutableList().apply { this[3] = this[3].copy(parent = 0) }
    assertEquals(emptyList<Bounds>(), instagramFeedVideoPosts(s.copy(nodes = nodes)))
  }

  private fun assertCapturedVideos(number: Int) {
    val name = "feed_scroll_$number"
    val s = loadInstagramFixture(name)
    assertEquals(name, labelledPosts(name, "VIDEO").mapNotNull { clipPost(s, it) },
      instagramFeedVideoPosts(s))
    assertEquals(Detection.None, detectInstagram(s))
  }

  private fun feed(post: Bounds) = ScreenSnapshot(INSTAGRAM_PACKAGE, listOf(
    node(null, Bounds(0, 0, 1080, 2400), -1),
    node("swipeable_tab_view_pager", Bounds(0, 63, 1080, 2211), 0),
    SnapshotNode("android:id/list", "RecyclerView", false, Bounds(0, 63, 1080, 2211), 1),
    node("video_container", post, 2),
    node("video_states", post, 3),
  ))

  private fun node(id: String?, bounds: Bounds, parent: Int) = SnapshotNode(
    id?.let { "$INSTAGRAM_PACKAGE:id/$it" }, null, false, bounds, parent,
  )
}
