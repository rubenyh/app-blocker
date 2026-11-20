package com.example.appblocker.settings

data class BlockerSettings(
  val youtubeBlockViewer: Boolean = true,
  val youtubeMaskEntryPoints: Boolean = false,
  val instagramBlockViewer: Boolean = true,
  val instagramMaskEntryPoints: Boolean = false,
  val diagnosticsEnabled: Boolean = false,
)
