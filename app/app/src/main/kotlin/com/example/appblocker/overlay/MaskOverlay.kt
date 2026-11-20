package com.example.appblocker.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.example.appblocker.detect.Bounds

class MaskOverlay(private val context: Context) {
  private val windowManager = context.getSystemService(WindowManager::class.java)
  private val views = mutableListOf<View>()

  fun show(rects: List<Bounds>) {
    while (views.size > rects.size) removeLast()
    val dark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
      Configuration.UI_MODE_NIGHT_YES
    for ((index, rect) in rects.withIndex()) {
      val params = WindowManager.LayoutParams(
        rect.right - rect.left, (rect.bottom - rect.top - MASK_TOP_INSET_PX).coerceAtLeast(1),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.OPAQUE,
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = rect.left
        y = rect.top + MASK_TOP_INSET_PX
      }
      val existing = views.getOrNull(index)
      val mask = existing ?: View(context).apply {
        setOnClickListener { }
        setOnLongClickListener { true }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
      }
      mask.setBackgroundColor(if (dark) MASK_COLOR_DARK else MASK_COLOR_LIGHT)
      val success = windowOperation {
        if (existing == null) windowManager.addView(mask, params)
        else windowManager.updateViewLayout(mask, params)
      }
      if (existing == null && (success || mask.isAttachedToWindow)) views.add(mask)
      if (!success) {
        removeAll()
        return
      }
    }
  }

  fun removeAll() {
    while (views.isNotEmpty()) removeLast()
  }

  private fun removeLast() {
    val mask = views.removeAt(views.lastIndex)
    windowOperation { windowManager.removeViewImmediate(mask) }
  }

  private inline fun windowOperation(action: () -> Unit): Boolean = try {
    action()
    true
  } catch (_: WindowManager.BadTokenException) {
    false
  } catch (_: IllegalStateException) {
    false
  } catch (_: IllegalArgumentException) {
    false
  }

  companion object {
    // Calibrated 2026-11-19 on YouTube 21.41.525 (AVD 1080x2400): colors blend with the bottom bar in
    // light and dark themes; the top inset keeps the bar's divider line visible.
    // ponytail: fixed pixels/colors; recalibrate when YouTube restyles the bar or on other densities.
    const val MASK_COLOR_LIGHT = 0xFFFFFFFF.toInt()
    const val MASK_COLOR_DARK = 0xFF0F0F0F.toInt()
    const val MASK_TOP_INSET_PX = 3
  }
}
