package com.example.appblocker.policy

import com.example.appblocker.detect.Detection
import com.example.appblocker.service.INSTAGRAM_PACKAGE
import com.example.appblocker.service.YOUTUBE_PACKAGE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockPolicyTest {
  private val policy = BlockPolicy()
  private val viewer = Detection.Viewer("yt-reel-player-v1")

  @Test fun firstViewerSendsBack() {
    assertEquals(Action.Back, screen())
    assertEquals(Action.Nothing, screen())
  }

  @Test fun burstSendsOnlyOneBack() {
    assertEquals(Action.Back, screen(now = 100))
    for (time in 101L..1599L) assertEquals(Action.Nothing, screen(now = time))
  }

  @Test fun retriesAtDelayBoundaryAndStopsAtCap() {
    assertEquals(Action.Back, screen(now = 100))
    assertEquals(Action.Nothing, screen(now = 1599))
    assertEquals(Action.Back, screen(now = 1600))
    assertEquals(Action.Back, screen(now = 3100))
    assertTrue(policy.retryLimitReached)
    assertEquals(Action.Nothing, screen(now = 4600))
    assertEquals(Action.Nothing, screen(now = 100_000))
  }

  @Test fun noneEndsEpisode() {
    screen()
    assertEquals(Action.Nothing, screen(detection = Detection.None))
    assertEquals(Action.Back, screen())
  }

  @Test fun contextExitEndsEpisode() {
    screen()
    policy.onContextExit()
    assertEquals(Action.Back, screen())
  }

  @Test fun packageSwitchEndsEpisode() {
    screen()
    assertEquals(Action.Nothing, screen(pkg = "other.app"))
    assertEquals(Action.Back, screen())
  }

  @Test fun unknownPreservesEpisodeAndCooldown() {
    screen(now = 100)
    assertEquals(Action.Nothing, screen(detection = Detection.Unknown, now = 200))
    assertEquals(Action.Nothing, screen(now = 1599))
    assertEquals(Action.Back, screen(now = 1600))
  }

  @Test fun unknownPreservesRetryCap() {
    screen(now = 0)
    screen(now = 1500)
    screen(now = 3000)
    assertEquals(Action.Nothing, screen(detection = Detection.Unknown, now = 4500))
    assertEquals(Action.Nothing, screen(now = 6000))
  }

  @Test fun disabledDoesNothingAndResets() {
    screen()
    assertEquals(Action.Nothing, screen(enabled = false))
    assertEquals(Action.Back, screen())
  }

  @Test fun instagramDoesNothingAndResets() {
    screen()
    assertEquals(Action.Nothing, screen(pkg = INSTAGRAM_PACKAGE))
    assertEquals(Action.Back, screen())
  }

  private fun screen(
    pkg: String = YOUTUBE_PACKAGE,
    detection: Detection = viewer,
    enabled: Boolean = true,
    now: Long = 0,
  ) = policy.onTargetScreen(pkg, detection, enabled, now)
}
