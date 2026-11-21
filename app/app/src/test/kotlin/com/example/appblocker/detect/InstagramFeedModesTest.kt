package com.example.appblocker.detect

import com.example.appblocker.service.INSTAGRAM_PACKAGE
import org.junit.Assert.*
import org.junit.Test

internal fun labelledPosts(name: String, label: String): List<Bounds> {
  val text = requireNotNull(ScreenSnapshot::class.java.getResource("/fixtures/instagram/$name.txt")).readText()
  return Regex("""#   $label \[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]""")
    .findAll(text).map {
      val v = it.groupValues.drop(1).map(String::toInt)
      Bounds(v[0], v[1], v[2], v[3])
    }.toList()
}

internal fun clipPost(s: ScreenSnapshot, post: Bounds): Bounds? {
  val area = s.nodes.single { it.viewId == "android:id/list" }.bounds
  val bars = setOf("main_feed_action_bar", "action_bar_container", "action_bar",
    "action_bar_left_button", "action_bar_title_view", "notification")
  val top = maxOf(area.top, s.nodes.filter { it.viewId?.substringAfter(":id/") in bars }
    .maxOfOrNull { it.bounds.bottom } ?: area.top)
  val bottom = minOf(area.bottom, s.nodes.filter {
    it.viewId?.substringAfter(":id/") in setOf("tab_bar", "feed_tab")
  }.minOfOrNull { it.bounds.top } ?: area.bottom)
  return Bounds(maxOf(area.left, post.left), maxOf(top, post.top),
    minOf(area.right, post.right), minOf(bottom, post.bottom)).takeIf {
      it.right > it.left && it.bottom - it.top >= 24
    }
}

class InstagramFeedModesTest {
  private val homes = listOf("home_for_you") + (0..12).map { "feed_scroll_$it" }
  private val following = (1..6).map { "following_feed_$it" }

  @Test fun classifiesCapturedScreens() {
    for (name in homes) assertEquals(name, IgScreen.HOME_FOR_YOU, instagramScreen(loadInstagramFixture(name)))
    for (name in following) assertEquals(name, IgScreen.FOLLOWING, instagramScreen(loadInstagramFixture(name)))
    assertEquals(IgScreen.FEED_MENU, instagramScreen(loadInstagramFixture("home_feed_menu")))
    for (name in listOf("reels_tab", "explore", "direct_inbox", "own_profile", "story_viewer")) {
      assertEquals(name, IgScreen.OTHER, instagramScreen(loadInstagramFixture(name)))
    }
  }

  @Test fun positiveVideosMatchAllUncompressedLabels() {
    for (name in homes + following + "home_feed_menu") {
      val s = loadInstagramFixture(name)
      val expected = labelledPosts(name, "VIDEO").mapNotNull { clipPost(s, it) }
      assertEquals(name, expected, instagramFeedVideoPosts(s))
      for (label in listOf("PHOTO", "OTHER")) {
        assertTrue(name, labelledPosts(name, label).mapNotNull { clipPost(s, it) }
          .none { it in instagramFeedVideoPosts(s) })
      }
    }
  }

  @Test fun mediaCoversEveryLabelAndStaysOutsideBars() {
    for (name in homes + following) {
      val s = loadInstagramFixture(name)
      val media = instagramFeedMediaPosts(s)
      for (post in listOf("VIDEO", "PHOTO", "OTHER").flatMap { labelledPosts(name, it) }) {
        clipPost(s, post)?.let { assertTrue("$name missing $it", it in media) }
      }
      assertTrue(name, media.all { clipPost(s, it) == it })
    }
  }

  @Test fun carouselUsesOutermostMediaOnly() {
    val s = loadInstagramFixture("following_feed_4")
    val outer = s.nodes.filter { it.viewId == "$INSTAGRAM_PACKAGE:id/zoomable_view_container" }
      .mapNotNull { clipPost(s, it.bounds) }
    assertEquals(2, outer.size)
    assertEquals(outer, instagramFeedMediaPosts(s))
  }

  @Test fun menuAndLogoTargetsUseOnlyTheirKnownScreens() {
    assertEquals(Bounds(309, 201, 770, 350), followingMenuItem(loadInstagramFixture("home_feed_menu")))
    val home = loadInstagramFixture("home_for_you")
    assertEquals(home.nodes.single { it.viewId == "$INSTAGRAM_PACKAGE:id/title_logo" }.bounds, titleLogo(home))
    assertNull(followingMenuItem(home))
    assertNull(titleLogo(loadInstagramFixture("home_feed_menu")))
    assertNull(titleLogo(home.copy(packageName = "another.app")))
  }
}
