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
    .filter { it.right > it.left && it.bottom > it.top }
}
