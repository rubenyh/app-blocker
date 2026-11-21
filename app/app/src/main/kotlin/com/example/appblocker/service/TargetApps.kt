package com.example.appblocker.service

import com.example.appblocker.settings.BlockerSettings

const val YOUTUBE_PACKAGE = "com.google.android.youtube"
const val INSTAGRAM_PACKAGE = "com.instagram.android"

fun isEnabledFor(pkg: CharSequence?, settings: BlockerSettings): Boolean = when (pkg?.toString()) {
  YOUTUBE_PACKAGE -> settings.youtubeBlockViewer || settings.youtubeMaskEntryPoints
  INSTAGRAM_PACKAGE -> settings.instagramBlockViewer || settings.instagramMaskEntryPoints
  else -> false
}

fun BlockerSettings.viewerEnabledFor(pkg: String?): Boolean = when (pkg) {
  YOUTUBE_PACKAGE -> youtubeBlockViewer
  INSTAGRAM_PACKAGE -> instagramBlockViewer
  else -> false
}

fun BlockerSettings.maskEnabledFor(pkg: String?): Boolean = when (pkg) {
  YOUTUBE_PACKAGE -> youtubeMaskEntryPoints
  INSTAGRAM_PACKAGE -> instagramMaskEntryPoints
  else -> false
}
