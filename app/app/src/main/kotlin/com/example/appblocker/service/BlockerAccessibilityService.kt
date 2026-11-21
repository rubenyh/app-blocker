package com.example.appblocker.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.example.appblocker.detect.Bounds
import com.example.appblocker.detect.IgScreen
import com.example.appblocker.detect.instagramScreen
import com.example.appblocker.detect.instagramFeedMediaPosts
import com.example.appblocker.detect.titleLogo
import com.example.appblocker.detect.followingMenuItem
import com.example.appblocker.policy.FeedSwitchPolicy
import com.example.appblocker.policy.FeedSwitchAction
import com.example.appblocker.settings.InstagramFeedMode
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.detect.Detection
import com.example.appblocker.detect.SnapshotExtractor
import com.example.appblocker.detect.detectYouTube
import com.example.appblocker.detect.detectInstagram
import com.example.appblocker.detect.instagramEntryPoints
import com.example.appblocker.detect.instagramFeedVideoPosts
import com.example.appblocker.detect.youTubeEntryPoints
import com.example.appblocker.diagnostics.DiagnosticEntry
import com.example.appblocker.diagnostics.Diagnostics
import com.example.appblocker.overlay.MaskOverlay
import com.example.appblocker.policy.Action
import com.example.appblocker.policy.BlockPolicy
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.settings.SettingsRepository
import com.example.appblocker.settings.blockerSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class BlockerAccessibilityService : AccessibilityService() {
  private var serviceScope: CoroutineScope? = null
  private val policy = BlockPolicy()
  private val feedSwitch = FeedSwitchPolicy(SystemClock::elapsedRealtime)
  private var triggers = Channel<Unit>(Channel.CONFLATED)
  private val masks by lazy { MaskOverlay(this) }
  private val appVersions = mutableMapOf<String, String?>()
  private var activePackage: String? = null

  @Volatile private var settings = BlockerSettings()

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceScope?.cancel()
    triggers.close()
    triggers = Channel(Channel.CONFLATED)
    policy.onContextExit()
    feedSwitch.reset()
    masks.removeAll()
    activePackage = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    serviceScope = scope
    val repository = SettingsRepository(applicationContext.blockerSettingsDataStore)
    scope.launch {
      repository.settings.collect {
        if (settings != it) feedSwitch.reset()
        settings = it
        Diagnostics.setEnabled(it.diagnosticsEnabled)
        if (!it.viewerEnabledFor(activePackage)) policy.onContextExit()
        if (!it.maskEnabledFor(activePackage)) masks.removeAll()
        triggers.trySend(Unit)
      }
    }
    scope.launch {
      for (trigger in triggers) {
        if (!isEnabledFor(YOUTUBE_PACKAGE, settings) && !isEnabledFor(INSTAGRAM_PACKAGE, settings)) {
          policy.onContextExit()
          feedSwitch.reset()
          masks.removeAll()
          continue
        }
        val snapshot = try {
          SnapshotExtractor.extract(rootInActiveWindow)
        } catch (_: RuntimeException) {
          null
        }
        val pkg = snapshot?.packageName ?: activePackage ?: continue
        if (pkg != activePackage) {
          masks.removeAll()
          policy.onContextExit()
          feedSwitch.reset()
        }
        activePackage = pkg
        if (!isEnabledFor(pkg, settings)) {
          masks.removeAll()
          policy.onContextExit()
          feedSwitch.reset()
          continue
        }
        val detection = if (snapshot == null) Detection.Unknown else when (pkg) {
          YOUTUBE_PACKAGE -> detectYouTube(snapshot)
          INSTAGRAM_PACKAGE -> detectInstagram(snapshot)
          else -> Detection.Unknown
        }
        val screen = snapshot?.let { instagramScreen(it) } ?: IgScreen.OTHER
        val feedRects = if (pkg == INSTAGRAM_PACKAGE && settings.instagramMaskEntryPoints &&
          snapshot != null && detection == Detection.None) {
          when (screen) {
            IgScreen.HOME_FOR_YOU -> instagramFeedMediaPosts(snapshot)
            IgScreen.FOLLOWING -> when (settings.instagramFeedMode) {
              InstagramFeedMode.FOLLOWING_MASK_VIDEOS -> instagramFeedVideoPosts(snapshot)
              InstagramFeedMode.FOLLOWING_SHOW_ALL -> emptyList()
              InstagramFeedMode.MASK_ALL_MEDIA -> instagramFeedMediaPosts(snapshot)
            }
            else -> emptyList()
          }
        } else emptyList()
        val switchAction = if (pkg == INSTAGRAM_PACKAGE && settings.instagramMaskEntryPoints &&
          snapshot != null && detection == Detection.None) {
          feedSwitch.onScreen(screen, when (screen) {
            IgScreen.HOME_FOR_YOU -> titleLogo(snapshot)
            IgScreen.FEED_MENU -> followingMenuItem(snapshot)
            else -> null
          })
        } else { feedSwitch.reset(); null }
        val maskRuleIds = mutableListOf<String>()
        val action = policy.onTargetScreen(
          pkg, detection, settings.viewerEnabledFor(pkg), SystemClock.elapsedRealtime(),
        )
        val rects = if (settings.maskEnabledFor(pkg) && snapshot != null && detection == Detection.None) {
          when (pkg) {
            YOUTUBE_PACKAGE -> youTubeEntryPoints(snapshot).also {
              if (it.isNotEmpty()) maskRuleIds.add("yt-shorts-tab-v1")
            }
            INSTAGRAM_PACKAGE -> {
              val tabs = if (screen == IgScreen.HOME_FOR_YOU || screen == IgScreen.OTHER)
                instagramEntryPoints(snapshot) else emptyList()
              if (tabs.isNotEmpty()) maskRuleIds.add("ig-reels-tab-v1")
              if (feedRects.isNotEmpty()) maskRuleIds.add(
                if (screen == IgScreen.FOLLOWING &&
                  settings.instagramFeedMode == InstagramFeedMode.FOLLOWING_MASK_VIDEOS)
                  "ig-feed-video-v1" else "ig-feed-media-v1")
              tabs + feedRects
            }
            else -> emptyList()
          }
        } else emptyList()
        if (rects.isEmpty()) masks.removeAll() else masks.show(
          rects, topInsetPx = if (pkg == YOUTUBE_PACKAGE) MaskOverlay.MASK_TOP_INSET_PX else 0,
          // Feed masks must not swallow scroll gestures (device-verified 2026-11-20); a tap that
          // opens the Reels viewer is caught by viewer blocking instead.
          passThrough = feedRects.toSet(),
        )
        if (switchAction != null) {
          clickFeedSwitch(switchAction)
          maskRuleIds.add("ig-feed-switch-v1")
        }
        val maskRuleId = maskRuleIds.joinToString("+").ifEmpty { null }
        if (action == Action.Back) {
          masks.removeAll()
          // A Reel opened from a DM should return to its thread; device check awaits the owner's DM fixture.
          performGlobalAction(GLOBAL_ACTION_BACK)
        }
        if (settings.diagnosticsEnabled) {
          if (!appVersions.containsKey(pkg)) {
            appVersions[pkg] = try {
              packageManager.getPackageInfo(pkg, 0).versionName
            } catch (_: PackageManager.NameNotFoundException) {
              null
            }
          }
          val entry = DiagnosticEntry(
            timeMillis = System.currentTimeMillis(),
            packageName = pkg,
            appVersion = appVersions[pkg],
            ruleId = (detection as? Detection.Viewer)?.ruleId
              ?: maskRuleId,
            detection = when (detection) {
              is Detection.Viewer -> "Viewer"
              Detection.None -> "None"
              Detection.Unknown -> "Unknown"
            },
            action = if (action == Action.Back) "Back" else "Nothing",
            masks = rects.size,
            capHit = policy.retryLimitReached,
          )
          val previous = Diagnostics.entries.value.lastOrNull()
          if (previous?.packageName != pkg || !entry.hasSameDecisionAs(previous)) Diagnostics.record(entry)
        }
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
          if (settings.maskEnabledFor(pkg)) {
            Log.d("BlockerAccessibility", "ruleId=$maskRuleId rectCount=${rects.size}")
          }
          val ruleId = (detection as? Detection.Viewer)?.ruleId
          Log.d("BlockerAccessibility", "detection=$detection ruleId=$ruleId action=$action capHit=${policy.retryLimitReached}")
        }
      }
    }
  }

  private fun clickFeedSwitch(action: FeedSwitchAction) {
    val expected = when (action) {
      is FeedSwitchAction.TapTitle -> action.bounds
      is FeedSwitchAction.TapFollowing -> action.bounds
    }
    if (expected.right <= expected.left || expected.bottom <= expected.top) return
    val obtained = mutableListOf<AccessibilityNodeInfo>()
    try {
      val root = rootInActiveWindow ?: return
      obtained.add(root)
      if (root.packageName?.toString() != INSTAGRAM_PACKAGE) return
      val id = if (action is FeedSwitchAction.TapTitle) "title_logo" else "context_menu_item"
      val candidates = root.findAccessibilityNodeInfosByViewId("$INSTAGRAM_PACKAGE:id/$id")
      obtained.addAll(candidates)
      val rect = Rect()
      val candidate = candidates.filter { node ->
        node.getBoundsInScreen(rect)
        node.isVisibleToUser && !rect.isEmpty
      }.minByOrNull { node -> node.getBoundsInScreen(rect); rect.top } ?: return
      candidate.getBoundsInScreen(rect)
      // Reject a stale target after a window change; never click another menu or a moved control.
      if (Bounds(rect.left, rect.top, rect.right, rect.bottom) != expected) return
      var node = candidate
      repeat(6) { hop ->
        if (node.isClickable) {
          node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
          return
        }
        if (hop == 5) return
        node = node.parent ?: return
        obtained.add(node)
      }
    } catch (_: RuntimeException) {
      // The window may disappear between the snapshot and click; the bounded policy permits retries.
    } finally {
      if (Build.VERSION.SDK_INT < 33) {
        @Suppress("DEPRECATION")
        for (node in obtained) node.recycle()
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val pkg = event?.packageName?.toString() ?: return
    if (pkg != YOUTUBE_PACKAGE && pkg != INSTAGRAM_PACKAGE) {
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        masks.removeAll()
        policy.onContextExit()
        feedSwitch.reset()
        activePackage = null
      }
      return
    }
    if (pkg != activePackage) {
      masks.removeAll()
      policy.onContextExit()
      feedSwitch.reset()
    }
    activePackage = pkg
    if (isEnabledFor(pkg, settings)) triggers.trySend(Unit)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    masks.removeAll()
    triggers.trySend(Unit)
  }

  override fun onInterrupt() {
    masks.removeAll()
    policy.onContextExit()
    feedSwitch.reset()
  }

  override fun onUnbind(intent: Intent?): Boolean {
    masks.removeAll()
    serviceScope?.cancel()
    serviceScope = null
    triggers.close()
    policy.onContextExit()
    feedSwitch.reset()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    masks.removeAll()
    serviceScope?.cancel()
    serviceScope = null
    triggers.close()
    policy.onContextExit()
    feedSwitch.reset()
    super.onDestroy()
  }
}
