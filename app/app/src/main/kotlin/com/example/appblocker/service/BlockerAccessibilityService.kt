package com.example.appblocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.detect.Detection
import com.example.appblocker.detect.SnapshotExtractor
import com.example.appblocker.detect.detectYouTube
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
  private var triggers = Channel<Unit>(Channel.CONFLATED)

  @Volatile private var settings = BlockerSettings()

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceScope?.cancel()
    triggers.close()
    triggers = Channel(Channel.CONFLATED)
    policy.onContextExit()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    serviceScope = scope
    val repository = SettingsRepository(applicationContext.blockerSettingsDataStore)
    scope.launch {
      repository.settings.collect {
        val wasEnabled = settings.youtubeBlockViewer
        settings = it
        if (!it.youtubeBlockViewer) policy.onContextExit()
        if (it.youtubeBlockViewer && !wasEnabled) triggers.trySend(Unit)
      }
    }
    scope.launch {
      for (trigger in triggers) {
        if (!settings.youtubeBlockViewer) {
          policy.onContextExit()
          continue
        }
        val snapshot = try {
          SnapshotExtractor.extract(rootInActiveWindow)
        } catch (_: RuntimeException) {
          null
        }
        val pkg = snapshot?.packageName ?: YOUTUBE_PACKAGE
        val detection = if (pkg == YOUTUBE_PACKAGE && snapshot != null) {
          detectYouTube(snapshot)
        } else {
          Detection.Unknown
        }
        val action = policy.onTargetScreen(
          pkg, detection, settings.youtubeBlockViewer, SystemClock.elapsedRealtime(),
        )
        if (action == Action.Back) performGlobalAction(GLOBAL_ACTION_BACK)
        if (pkg == YOUTUBE_PACKAGE && applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
          val ruleId = (detection as? Detection.Viewer)?.ruleId
          Log.d("BlockerAccessibility", "detection=$detection ruleId=$ruleId action=$action capHit=${policy.retryLimitReached}")
        }
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val pkg = event?.packageName?.toString() ?: return
    if (pkg != YOUTUBE_PACKAGE) {
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) policy.onContextExit()
      return
    }
    if (settings.youtubeBlockViewer) triggers.trySend(Unit)
  }

  override fun onInterrupt() {
    policy.onContextExit()
  }

  override fun onUnbind(intent: Intent?): Boolean {
    serviceScope?.cancel()
    serviceScope = null
    triggers.close()
    policy.onContextExit()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    serviceScope?.cancel()
    serviceScope = null
    triggers.close()
    policy.onContextExit()
    super.onDestroy()
  }
}
