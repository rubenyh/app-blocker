package com.example.appblocker.detect

import com.example.appblocker.service.YOUTUBE_PACKAGE

fun youTubeEntryPoints(s: ScreenSnapshot): List<Bounds> {
  if (s.packageName != YOUTUBE_PACKAGE) return emptyList()
  val bars = s.nodes.withIndex().filter {
    it.value.viewId == "$YOUTUBE_PACKAGE:id/pivot_bar" &&
      it.value.className in listOf("HorizontalScrollView", "android.widget.HorizontalScrollView")
  }
  if (bars.size != 1) return emptyList()
  val layouts = s.nodes.withIndex().filter {
    it.value.parent == bars.single().index &&
      it.value.className in listOf("LinearLayout", "android.widget.LinearLayout")
  }
  if (layouts.size != 1) return emptyList()
  val buttons = s.nodes.filter {
    it.parent == layouts.single().index &&
      it.className in listOf("Button", "android.widget.Button")
  }
  if (buttons.size !in 4..5) return emptyList()
  // shortcut: yt-shorts-tab-v1 assumes tab order; recapture evidence if YouTube reorders tabs.
  val rect = buttons[1].bounds
  if (rect.right <= rect.left || rect.bottom <= rect.top) return emptyList()
  return listOf(rect)
}

fun detectYouTube(s: ScreenSnapshot): Detection {
  val root = s.nodes.firstOrNull() ?: return Detection.Unknown
  val rootHeight = root.bounds.bottom.toLong() - root.bounds.top
  if (rootHeight <= 0) return Detection.Unknown
  if (s.packageName != YOUTUBE_PACKAGE) return Detection.None
  for (node in s.nodes) {
    if (node.viewId != "$YOUTUBE_PACKAGE:id/reel_player_page_container") continue
    if ((node.bounds.bottom.toLong() - node.bounds.top) * 2 < rootHeight) continue
    var parent = node.parent
    var remaining = s.nodes.size
    while (parent in s.nodes.indices && remaining-- > 0) {
      val ancestor = s.nodes[parent]
      if (ancestor.viewId == "$YOUTUBE_PACKAGE:id/reel_recycler") {
        return Detection.Viewer("yt-reel-player-v1")
      }
      parent = ancestor.parent
    }
  }
  return Detection.None
}
