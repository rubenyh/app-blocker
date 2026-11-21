package com.example.appblocker.policy

import com.example.appblocker.detect.Bounds
import com.example.appblocker.detect.IgScreen

sealed interface FeedSwitchAction {
  data class TapTitle(val bounds: Bounds) : FeedSwitchAction
  data class TapFollowing(val bounds: Bounds) : FeedSwitchAction
}

class FeedSwitchPolicy(private val clock: () -> Long) {
  private var attempts = 0
  private var lastAttempt: Long? = null
  private var menuTapped = false

  fun onScreen(screen: IgScreen, target: Bounds?): FeedSwitchAction? {
    when (screen) {
      IgScreen.FOLLOWING, IgScreen.OTHER -> reset()
      IgScreen.HOME_FOR_YOU -> {
        menuTapped = false
        val now = clock()
        if (target != null && target.right > target.left && target.bottom > target.top &&
          attempts < 3 && (lastAttempt == null || now - lastAttempt!! >= 1500)) {
          attempts++
          lastAttempt = now
          return FeedSwitchAction.TapTitle(target)
        }
      }
      IgScreen.FEED_MENU -> if (!menuTapped && target != null &&
        target.right > target.left && target.bottom > target.top) {
        menuTapped = true
        return FeedSwitchAction.TapFollowing(target)
      }
    }
    return null
  }

  fun reset() {
    attempts = 0
    lastAttempt = null
    menuTapped = false
  }
}
