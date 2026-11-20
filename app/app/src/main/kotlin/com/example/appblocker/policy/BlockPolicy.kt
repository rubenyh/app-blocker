package com.example.appblocker.policy

import com.example.appblocker.detect.Detection
import com.example.appblocker.service.YOUTUBE_PACKAGE

sealed interface Action {
  data object Back : Action
  data object Nothing : Action
}

class BlockPolicy {
  private var episodePackage: String? = null
  private var backsInEpisode = 0
  private var lastBack = 0L

  val retryLimitReached: Boolean get() = backsInEpisode >= MAX_BACKS_PER_EPISODE

  fun onTargetScreen(pkg: String, detection: Detection, blockEnabled: Boolean, now: Long): Action {
    if (pkg != episodePackage || !blockEnabled) onContextExit()
    if (pkg != YOUTUBE_PACKAGE || !blockEnabled) return Action.Nothing
    when (detection) {
      Detection.None -> onContextExit()
      Detection.Unknown -> Unit
      is Detection.Viewer -> {
        episodePackage = pkg
        if (!retryLimitReached && (backsInEpisode == 0 || now - lastBack >= RETRY_DELAY_MS)) {
          backsInEpisode++
          lastBack = now
          return Action.Back
        }
      }
    }
    return Action.Nothing
  }

  fun onContextExit() {
    episodePackage = null
    backsInEpisode = 0
    lastBack = 0L
  }

  companion object {
    const val RETRY_DELAY_MS = 1500L
    const val MAX_BACKS_PER_EPISODE = 3
  }
}
