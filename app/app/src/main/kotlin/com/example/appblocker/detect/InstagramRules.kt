package com.example.appblocker.detect

import com.example.appblocker.service.INSTAGRAM_PACKAGE

fun detectInstagram(s: ScreenSnapshot): Detection {
  val root = s.nodes.firstOrNull() ?: return Detection.Unknown
  val rootHeight = root.bounds.bottom.toLong() - root.bounds.top
  if (rootHeight <= 0) return Detection.Unknown
  if (s.packageName != INSTAGRAM_PACKAGE) return Detection.None
  for (node in s.nodes) {
    if (node.viewId != "$INSTAGRAM_PACKAGE:id/clips_video_container") continue
    if ((node.bounds.bottom.toLong() - node.bounds.top) * 2 < rootHeight) continue
    var parent = node.parent
    var remaining = s.nodes.size
    while (parent in s.nodes.indices && remaining-- > 0) {
      val ancestor = s.nodes[parent]
      if (ancestor.viewId == "$INSTAGRAM_PACKAGE:id/clips_viewer_view_pager") {
        return Detection.Viewer("ig-clips-viewer-v1")
      }
      parent = ancestor.parent
    }
  }
  return Detection.None
}

enum class IgScreen { HOME_FOR_YOU, FEED_MENU, FOLLOWING, OTHER }

private fun igId(name: String) = "$INSTAGRAM_PACKAGE:id/$name"

private fun ScreenSnapshot.hasAncestor(index: Int, matches: (Int) -> Boolean): Boolean {
  var parent = nodes[index].parent
  var remaining = nodes.size
  while (parent in nodes.indices && remaining-- > 0) {
    if (matches(parent)) return true
    parent = nodes[parent].parent
  }
  return false
}

fun instagramScreen(s: ScreenSnapshot): IgScreen {
  if (s.packageName != INSTAGRAM_PACKAGE || detectInstagram(s) != Detection.None) return IgScreen.OTHER
  fun has(id: String) = s.nodes.any { it.viewId == igId(id) }
  if (has("context_menu_options_list") && s.nodes.indices.count { index ->
      s.nodes[index].viewId == igId("context_menu_item") &&
        s.hasAncestor(index) { s.nodes[it].viewId == igId("context_menu_options_list") }
    } == 2) return IgScreen.FEED_MENU
  val hasFeedPosts = s.nodes.indices.any { index ->
    s.nodes[index].viewId?.startsWith(igId("row_feed_")) == true &&
      s.hasAncestor(index) { s.nodes[it].viewId == "android:id/list" }
  }
  // Scrolled home captures hide the logo/action bar but retain the feed list and bottom tabs.
  if (has("feed_tab") && (has("main_feed_action_bar") || has("title_logo") || hasFeedPosts)) {
    return IgScreen.HOME_FOR_YOU
  }
  if (!has("feed_tab") && has("action_bar_button_back") && has("action_bar_title") && hasFeedPosts) {
    return IgScreen.FOLLOWING
  }
  return IgScreen.OTHER
}

fun titleLogo(s: ScreenSnapshot): Bounds? =
  if (instagramScreen(s) == IgScreen.HOME_FOR_YOU) {
    s.nodes.firstOrNull { it.viewId == igId("title_logo") }?.bounds?.takeIf { it.isNotEmpty() }
  } else null

fun followingMenuItem(s: ScreenSnapshot): Bounds? =
  if (instagramScreen(s) == IgScreen.FEED_MENU) {
    s.nodes.filter { it.viewId == igId("context_menu_item") && it.bounds.isNotEmpty() }
      .minByOrNull { it.bounds.top }?.bounds
  } else null

fun instagramEntryPoints(s: ScreenSnapshot): List<Bounds> {
  if (s.packageName != INSTAGRAM_PACKAGE) return emptyList()
  return s.nodes.filter { it.viewId == igId("clips_tab") }
    .map { it.bounds }.filter { it.isNotEmpty() }
}

fun instagramFeedVideoPosts(s: ScreenSnapshot): List<Bounds> = instagramFeedPosts(s, videosOnly = true)

fun instagramFeedMediaPosts(s: ScreenSnapshot): List<Bounds> = instagramFeedPosts(s, videosOnly = false)

private fun instagramFeedPosts(s: ScreenSnapshot, videosOnly: Boolean): List<Bounds> {
  if (s.packageName != INSTAGRAM_PACKAGE || detectInstagram(s) != Detection.None) return emptyList()
  val list = s.nodes.indexOfFirst { it.viewId == "android:id/list" }
  if (list < 0) return emptyList()
  val area = s.nodes[list].bounds
  val actionBarIds = setOf("main_feed_action_bar", "action_bar_container", "action_bar",
    "action_bar_left_button", "action_bar_title_view", "notification").map(::igId)
  val clipTop = maxOf(area.top,
    s.nodes.filter { it.viewId in actionBarIds }.maxOfOrNull { it.bounds.bottom } ?: area.top)
  val clipBottom = minOf(area.bottom,
    s.nodes.filter { it.viewId in setOf(igId("tab_bar"), igId("feed_tab")) }
      .minOfOrNull { it.bounds.top } ?: area.bottom)
  val mediaIds = if (videosOnly) setOf(igId("video_container")) else
    setOf(igId("video_container"), igId("zoomable_view_container"), igId("carousel_image_media_group"))
  return s.nodes.indices.filter { index ->
    s.nodes[index].viewId in mediaIds && s.hasAncestor(index) { it == list } &&
      !s.hasAncestor(index) { s.nodes[it].viewId in mediaIds }
  }.mapNotNull { index ->
    // shortcut: playing signals arrive late and music photos may match; recapture if Instagram adds a stable video ID.
    if (videosOnly && s.nodes.indices.none { child ->
        s.nodes[child].viewId in setOf(igId("video_states"), igId("slideout_iconview_icon")) &&
          s.hasAncestor(child) { it == index }
      }) return@mapNotNull null
    val post = s.nodes[index].bounds
    val clipped = Bounds(maxOf(post.left, area.left), maxOf(post.top, clipTop),
      minOf(post.right, area.right), minOf(post.bottom, clipBottom))
    clipped.takeIf { it.isNotEmpty() && it.bottom.toLong() - it.top >= 24 }
  }.distinct()
}

private fun Bounds.isNotEmpty() = right > left && bottom > top
