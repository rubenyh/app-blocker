package com.example.appblocker.service

import android.accessibilityservice.AccessibilityService
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
    masks.removeAll()
    activePackage = null
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    serviceScope = scope
    val repository = SettingsRepository(applicationContext.blockerSettingsDataStore)
    scope.launch {
      repository.settings.collect {
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
        }
        activePackage = pkg
        if (!isEnabledFor(pkg, settings)) {
          masks.removeAll()
          policy.onContextExit()
          continue
        }
        val detection = if (snapshot == null) Detection.Unknown else when (pkg) {
          YOUTUBE_PACKAGE -> detectYouTube(snapshot)
          INSTAGRAM_PACKAGE -> detectInstagram(snapshot)
          else -> Detection.Unknown
        }
        val maskRuleId = if (pkg == INSTAGRAM_PACKAGE) "ig-reels-tab-v1" else "yt-shorts-tab-v1"
        val action = policy.onTargetScreen(
          pkg, detection, settings.viewerEnabledFor(pkg), SystemClock.elapsedRealtime(),
        )
        val rects = if (settings.maskEnabledFor(pkg) && snapshot != null && detection == Detection.None) {
          when (pkg) {
            YOUTUBE_PACKAGE -> youTubeEntryPoints(snapshot)
            INSTAGRAM_PACKAGE -> instagramEntryPoints(snapshot)
            else -> emptyList()
          }
        } else emptyList()
        if (rects.isEmpty()) masks.removeAll() else masks.show(rects)
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
              ?: if (rects.isNotEmpty()) maskRuleId else null,
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

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val pkg = event?.packageName?.toString() ?: return
    if (pkg != YOUTUBE_PACKAGE && pkg != INSTAGRAM_PACKAGE) {
      if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
        masks.removeAll()
        policy.onContextExit()
        activePackage = null
      }
      return
    }
    if (pkg != activePackage) {
      masks.removeAll()
      policy.onContextExit()
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
  }

  override fun onUnbind(intent: Intent?): Boolean {
    masks.removeAll()
    serviceScope?.cancel()
    serviceScope = null
    triggers.close()
    policy.onContextExit()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    masks.removeAll()
    serviceScope?.cancel()
    serviceScope = null
    triggers.close()
    policy.onContextExit()
    super.onDestroy()
  }
}
