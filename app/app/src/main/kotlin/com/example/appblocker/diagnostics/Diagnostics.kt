package com.example.appblocker.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiagnosticEntry(
  val timeMillis: Long,
  val packageName: String,
  val appVersion: String?,
  val ruleId: String?,
  val detection: String,
  val action: String,
  val masks: Int,
  val capHit: Boolean,
) {
  fun hasSameDecisionAs(previous: DiagnosticEntry?): Boolean =
    previous != null && detection == previous.detection && action == previous.action &&
      masks == previous.masks && ruleId == previous.ruleId
}

object Diagnostics {
  const val MAX_ENTRIES = 200
  private var enabled = false
  private val _entries = MutableStateFlow<List<DiagnosticEntry>>(emptyList())
  val entries: StateFlow<List<DiagnosticEntry>> = _entries.asStateFlow()

  @Synchronized
  fun record(entry: DiagnosticEntry) {
    if (enabled) _entries.value = (_entries.value + entry).takeLast(MAX_ENTRIES)
  }

  @Synchronized
  fun setEnabled(value: Boolean) {
    enabled = value
    if (!value) clear()
  }

  @Synchronized
  fun clear() {
    _entries.value = emptyList()
  }
}
