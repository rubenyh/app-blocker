package com.example.appblocker.detect

import com.example.appblocker.service.YOUTUBE_PACKAGE

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
