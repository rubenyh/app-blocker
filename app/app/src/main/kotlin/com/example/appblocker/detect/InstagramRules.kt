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

fun instagramEntryPoints(s: ScreenSnapshot): List<Bounds> {
  if (s.packageName != INSTAGRAM_PACKAGE) return emptyList()
  return s.nodes.filter { it.viewId == "$INSTAGRAM_PACKAGE:id/clips_tab" }
    .map { it.bounds }
    .filter { it.right > it.left && it.bottom > it.top } + instagramFeedVideoPosts(s)
}

fun instagramFeedVideoPosts(s: ScreenSnapshot): List<Bounds> {
  if (s.packageName != INSTAGRAM_PACKAGE || detectInstagram(s) != Detection.None) return emptyList()

  fun hasAncestor(index: Int, matches: (Int) -> Boolean): Boolean {
    var parent = s.nodes[index].parent
    var remaining = s.nodes.size
    while (parent in s.nodes.indices && remaining-- > 0) {
      if (matches(parent)) return true
      parent = s.nodes[parent].parent
    }
    return false
  }

  val pager = s.nodes.indexOfFirst { it.viewId == "$INSTAGRAM_PACKAGE:id/swipeable_tab_view_pager" }
  if (pager < 0) return emptyList()
  val list = s.nodes.indices.firstOrNull {
    s.nodes[it].viewId == "android:id/list" && hasAncestor(it) { parent -> parent == pager }
  } ?: pager
  val area = s.nodes[list].bounds
  val actionBarIds = listOf("action_bar_left_button", "action_bar_title_view", "notification")
    .map { "$INSTAGRAM_PACKAGE:id/$it" }
  val clipTop = maxOf(area.top,
    s.nodes.filter { it.viewId in actionBarIds }.maxOfOrNull { it.bounds.bottom } ?: area.top)
  val videoId = "$INSTAGRAM_PACKAGE:id/video_container"
  // ponytail: carousels and sponsored formats are unverified; recapture evidence before extending this rule.
  return s.nodes.indices.filter { index ->
    s.nodes[index].viewId == videoId &&
      hasAncestor(index) { it == list } &&
      !hasAncestor(index) { s.nodes[it].viewId == videoId }
  }.mapNotNull { index ->
    val descendants = s.nodes.indices.filter { child -> hasAncestor(child) { it == index } }
    if (descendants.none { s.nodes[it].viewId == "$INSTAGRAM_PACKAGE:id/media_group" } ||
      descendants.any { s.nodes[it].viewId == "$INSTAGRAM_PACKAGE:id/row_feed_photo_imageview" }) {
      return@mapNotNull null
    }
    val post = s.nodes[index].bounds
    val clipped = Bounds(maxOf(post.left, area.left), maxOf(post.top, clipTop),
      minOf(post.right, area.right), minOf(post.bottom, area.bottom))
    clipped.takeIf { it.right > it.left && it.bottom.toLong() - it.top >= 24 }
  }
}
