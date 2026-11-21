package com.example.appblocker.policy

import com.example.appblocker.detect.Bounds
import com.example.appblocker.detect.IgScreen
import org.junit.Assert.*
import org.junit.Test

class FeedSwitchPolicyTest {
  private var now = 0L
  private val policy = FeedSwitchPolicy { now }
  private val target = Bounds(0, 0, 100, 100)
  private fun home() = policy.onScreen(IgScreen.HOME_FOR_YOU, target)

  @Test fun rateLimitAndThreeAttemptCap() {
    assertEquals(FeedSwitchAction.TapTitle(target), home())
    now = 1499
    assertNull(home())
    now = 1500
    assertNotNull(home())
    now = 3000
    assertNotNull(home())
    now = 10000
    assertNull(home())
  }

  @Test fun followingOtherAndExitResetTheHomeEpisode() {
    for (screen in listOf(IgScreen.FOLLOWING, IgScreen.OTHER)) {
      repeat(3) { assertNotNull(home()); now += 1500 }
      assertNull(home())
      assertNull(policy.onScreen(screen, null))
      assertNotNull(home())
      policy.reset()
    }
    assertNotNull(home())
    policy.reset()
    assertNotNull(home())
  }

  @Test fun menuTapOncePerVisitWithoutResettingHomeCap() {
    repeat(3) {
      assertNotNull(home())
      assertEquals(FeedSwitchAction.TapFollowing(target), policy.onScreen(IgScreen.FEED_MENU, target))
      assertNull(policy.onScreen(IgScreen.FEED_MENU, target))
      now += 1500
    }
    assertNull(home())
    policy.reset()
    assertNotNull(policy.onScreen(IgScreen.FEED_MENU, target))
  }

  @Test fun absentAndEmptyTargetsDoNotSpendAttempts() {
    for (screen in listOf(IgScreen.HOME_FOR_YOU, IgScreen.FEED_MENU)) {
      assertNull(policy.onScreen(screen, null))
      assertNull(policy.onScreen(screen, Bounds(0, 0, 0, 1)))
      assertNull(policy.onScreen(screen, Bounds(0, 1, 1, 0)))
      assertNotNull(policy.onScreen(screen, target))
      policy.reset()
    }
  }
}
