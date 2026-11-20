package com.example.appblocker.detect

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.example.appblocker.service.YOUTUBE_PACKAGE

object SnapshotExtractor {
  const val MAX_NODES = 400
  const val MAX_DEPTH = 40

  fun extract(root: AccessibilityNodeInfo?): ScreenSnapshot? {
    if (root == null) return null
    val obtained = mutableListOf(root)
    try {
      val pkg = root.packageName?.toString() ?: return null
      // A queued YouTube event can outlive its window; never traverse another app.
      if (pkg != YOUTUBE_PACKAGE) return ScreenSnapshot(pkg, emptyList())
      val queue = ArrayDeque<Pending>()
      queue.add(Pending(root, -1, 0))
      val nodes = mutableListOf<SnapshotNode>()
      val rect = Rect()
      while (queue.isNotEmpty()) {
        val (node, parent, depth) = queue.removeFirst()
        if (!node.isVisibleToUser) continue
        node.getBoundsInScreen(rect)
        val index = nodes.size
        nodes.add(SnapshotNode(
          node.viewIdResourceName, node.className?.toString(), node.isSelected,
          Bounds(rect.left, rect.top, rect.right, rect.bottom), parent,
        ))
        val childCount = node.childCount
        // ponytail: incomplete trees are Unknown; revisit limits with captured evidence.
        if (depth == MAX_DEPTH && childCount > 0) return null
        for (i in 0 until childCount) {
          if (obtained.size == MAX_NODES) return null
          val child = node.getChild(i) ?: continue
          obtained.add(child)
          queue.add(Pending(child, index, depth + 1))
        }
      }
      return ScreenSnapshot(pkg, nodes)
    } catch (_: RuntimeException) {
      return null
    } finally {
      if (Build.VERSION.SDK_INT < 33) {
        @Suppress("DEPRECATION")
        for (node in obtained) node.recycle()
      }
    }
  }

  private data class Pending(val node: AccessibilityNodeInfo, val parent: Int, val depth: Int)
}
