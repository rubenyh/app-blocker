package com.example.appblocker.settings

enum class InstagramFeedMode { FOLLOWING_MASK_VIDEOS, FOLLOWING_SHOW_ALL, MASK_ALL_MEDIA }

data class BlockerSettings(
  val youtubeBlockViewer: Boolean = true,
  val youtubeMaskEntryPoints: Boolean = true,
  val instagramBlockViewer: Boolean = true,
  val instagramMaskEntryPoints: Boolean = true,
  val diagnosticsEnabled: Boolean = false,
  val instagramFeedMode: InstagramFeedMode = InstagramFeedMode.FOLLOWING_MASK_VIDEOS,
)
