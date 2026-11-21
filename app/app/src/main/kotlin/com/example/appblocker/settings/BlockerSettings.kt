package com.example.appblocker.settings

data class BlockerSettings(
  val youtubeBlockViewer: Boolean = true,
  val youtubeMaskEntryPoints: Boolean = true,
  val instagramBlockViewer: Boolean = true,
  val instagramMaskEntryPoints: Boolean = true,
  val diagnosticsEnabled: Boolean = false,
)
