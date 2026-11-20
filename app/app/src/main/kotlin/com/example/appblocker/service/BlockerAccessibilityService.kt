package com.example.appblocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appblocker.settings.BlockerSettings
import com.example.appblocker.settings.SettingsRepository
import com.example.appblocker.settings.blockerSettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BlockerAccessibilityService : AccessibilityService() {
  private var serviceScope: CoroutineScope? = null

  @Volatile private var settings = BlockerSettings()

  override fun onServiceConnected() {
    super.onServiceConnected()
    serviceScope?.cancel()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    serviceScope = scope
    val repository = SettingsRepository(applicationContext.blockerSettingsDataStore)
    scope.launch {
      repository.settings.collect { settings = it }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val pkg = event?.packageName ?: return
    if (!isEnabledFor(pkg, settings)) return
    val type = event.eventType
    if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
      Log.d("BlockerAccessibility", "$pkg ${AccessibilityEvent.eventTypeToString(type)}")
    }
  }

  override fun onInterrupt() {
    // No actions, overlays or listeners to interrupt yet.
  }

  override fun onUnbind(intent: Intent?): Boolean {
    serviceScope?.cancel()
    serviceScope = null
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    serviceScope?.cancel()
    serviceScope = null
    super.onDestroy()
  }
}
