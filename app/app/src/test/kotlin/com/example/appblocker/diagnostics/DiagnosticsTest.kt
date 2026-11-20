package com.example.appblocker.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsTest {
  private val entry = DiagnosticEntry(1L, "com.google.android.youtube", "21.41.525", null, "None", "Nothing", 0, false)

  @After
  fun reset() {
    Diagnostics.setEnabled(false)
  }

  @Test
  fun disabledByDefaultDoesNotRecord() {
    Diagnostics.record(entry)
    assertTrue(Diagnostics.entries.value.isEmpty())
  }

  @Test
  fun enablingRecordsEntries() {
    Diagnostics.setEnabled(true)
    Diagnostics.record(entry)
    assertEquals(listOf(entry), Diagnostics.entries.value)
  }

  @Test
  fun capDropsOldestEntry() {
    Diagnostics.setEnabled(true)
    repeat(201) { Diagnostics.record(entry.copy(timeMillis = it.toLong())) }
    assertEquals(200, Diagnostics.entries.value.size)
    assertEquals(1L, Diagnostics.entries.value.first().timeMillis)
    assertEquals(200L, Diagnostics.entries.value.last().timeMillis)
  }

  @Test
  fun disablingClearsAndStopsRecording() {
    Diagnostics.setEnabled(true)
    Diagnostics.record(entry)
    Diagnostics.setEnabled(false)
    Diagnostics.record(entry)
    assertTrue(Diagnostics.entries.value.isEmpty())
  }

  @Test
  fun clearLeavesRecordingEnabled() {
    Diagnostics.setEnabled(true)
    Diagnostics.record(entry)
    Diagnostics.clear()
    assertTrue(Diagnostics.entries.value.isEmpty())
    Diagnostics.record(entry)
    assertEquals(listOf(entry), Diagnostics.entries.value)
  }

  @Test
  fun duplicateComparisonUsesOnlyDecisionFields() {
    assertTrue(entry.hasSameDecisionAs(entry.copy(timeMillis = 2L, appVersion = null, capHit = true)))
    assertFalse(entry.hasSameDecisionAs(null))
    assertFalse(entry.hasSameDecisionAs(entry.copy(detection = "Unknown")))
    assertFalse(entry.hasSameDecisionAs(entry.copy(action = "Back")))
    assertFalse(entry.hasSameDecisionAs(entry.copy(masks = 1)))
    assertFalse(entry.hasSameDecisionAs(entry.copy(ruleId = "yt-shorts-tab-v1")))
  }
}
