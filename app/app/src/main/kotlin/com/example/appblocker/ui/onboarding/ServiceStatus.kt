package com.example.appblocker.ui.onboarding

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

// The coordinator will switch this to a class reference after merging.
const val BLOCKER_SERVICE_CLASS_NAME = "com.example.appblocker.service.BlockerAccessibilityService"

/**
 * Parses a single component entry into (packageName, className) without requiring Android framework classes,
 * matching ComponentName.unflattenFromString semantics for pure JVM testability.
 */
fun parseComponent(entry: String): Pair<String, String>? {
  val sep = entry.indexOf('/')
  if (sep <= 0 || sep >= entry.length - 1) {
    return null
  }
  val pkg = entry.substring(0, sep)
  var cls = entry.substring(sep + 1)
  if (cls.startsWith('.')) {
    cls = pkg + cls
  }
  return Pair(pkg, cls)
}

/**
 * Pure function to check if the target service is enabled in the ENABLED_ACCESSIBILITY_SERVICES string.
 *
 * @param enabledServices The raw string from Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES.
 * @param targetPackage The package name of the app.
 * @param targetClass The class name of the AccessibilityService.
 * @param entryParser Optional parser matching ComponentName.unflattenFromString semantics.
 */
fun isAccessibilityServiceEnabled(
  enabledServices: String?,
  targetPackage: String,
  targetClass: String,
  entryParser: (String) -> Pair<String, String>? = ::parseComponent,
): Boolean {
  if (enabledServices.isNullOrBlank()) {
    return false
  }
  return enabledServices
    .split(':')
    .filter { it.isNotBlank() }
    .any { entry ->
      val component = entryParser(entry.trim()) ?: return@any false
      component.first == targetPackage && component.second == targetClass
    }
}

/**
 * Reads the system Settings.Secure to check whether the blocker AccessibilityService is enabled.
 */
fun isBlockerServiceEnabled(context: Context): Boolean {
  val enabledServices = Settings.Secure.getString(
    context.contentResolver,
    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
  )
  return isAccessibilityServiceEnabled(
    enabledServices = enabledServices,
    targetPackage = context.packageName,
    targetClass = BLOCKER_SERVICE_CLASS_NAME,
    entryParser = { entry ->
      ComponentName.unflattenFromString(entry)?.let { Pair(it.packageName, it.className) }
    },
  )
}
