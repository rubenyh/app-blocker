package com.example.appblocker.detect

data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

data class SnapshotNode(
  val viewId: String?,
  val className: String?,
  val selected: Boolean,
  val bounds: Bounds,
  val parent: Int,
)

data class ScreenSnapshot(val packageName: String, val nodes: List<SnapshotNode>)
