package com.example.appblocker.detect

sealed interface Detection {
  data class Viewer(val ruleId: String) : Detection
  data object None : Detection
  data object Unknown : Detection
}
