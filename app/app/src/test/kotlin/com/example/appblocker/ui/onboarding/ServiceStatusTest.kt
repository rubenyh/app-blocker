package com.example.appblocker.ui.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStatusTest {
  private val testPackage = "com.appblocker.app"
  private val testClass = "com.example.appblocker.service.BlockerAccessibilityService"
  private val targetService = "$testPackage/$testClass"

  @Test
  fun nullInputReturnsFalse() {
    assertFalse(isAccessibilityServiceEnabled(null, testPackage, testClass))
  }

  @Test
  fun emptyInputReturnsFalse() {
    assertFalse(isAccessibilityServiceEnabled("", testPackage, testClass))
  }

  @Test
  fun blankInputReturnsFalse() {
    assertFalse(isAccessibilityServiceEnabled("   \t\n  ", testPackage, testClass))
  }

  @Test
  fun otherServicesOnlyReturnsFalse() {
    val singleOther = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
    assertFalse(isAccessibilityServiceEnabled(singleOther, testPackage, testClass))

    val multipleOthers =
      "com.other.app/com.other.Service:com.another.app/com.another.Service"
    assertFalse(isAccessibilityServiceEnabled(multipleOthers, testPackage, testClass))
  }

  @Test
  fun matchingServiceOnlyReturnsTrue() {
    assertTrue(isAccessibilityServiceEnabled(targetService, testPackage, testClass))
  }

  @Test
  fun matchingServiceAmongOthersReturnsTrue() {
    // At the start
    val atStart = "$targetService:com.other.app/com.other.Service"
    assertTrue(isAccessibilityServiceEnabled(atStart, testPackage, testClass))

    // In the middle
    val inMiddle = "com.first.app/com.first.Service:$targetService:com.last.app/com.last.Service"
    assertTrue(isAccessibilityServiceEnabled(inMiddle, testPackage, testClass))

    // At the end
    val atEnd = "com.other.app/com.other.Service:$targetService"
    assertTrue(isAccessibilityServiceEnabled(atEnd, testPackage, testClass))
  }

  @Test
  fun malformedEntriesReturnFalse() {
    // Missing slash
    assertFalse(isAccessibilityServiceEnabled("com.appblocker.app.service.BlockerAccessibilityService", testPackage, testClass))

    // Trailing slash only
    assertFalse(isAccessibilityServiceEnabled("com.appblocker.app/", testPackage, testClass))

    // Leading slash only
    assertFalse(isAccessibilityServiceEnabled("/com.example.appblocker.service.BlockerAccessibilityService", testPackage, testClass))

    // Only colons and empty entries
    assertFalse(isAccessibilityServiceEnabled(":::::", testPackage, testClass))

    // Whitespace only entry
    assertFalse(isAccessibilityServiceEnabled("   :   ", testPackage, testClass))
  }

  @Test
  fun malformedEntriesSurroundingValidServiceReturnsTrue() {
    val mixed = "invalid:corrupt/:$targetService::/also/invalid"
    assertTrue(isAccessibilityServiceEnabled(mixed, testPackage, testClass))
  }

  @Test
  fun samePackageDifferentClassReturnsFalse() {
    val differentClass = "$testPackage/com.example.appblocker.service.OtherService"
    assertFalse(isAccessibilityServiceEnabled(differentClass, testPackage, testClass))
  }

  @Test
  fun differentPackageSameClassReturnsFalse() {
    val differentPackage = "com.other.app/$testClass"
    assertFalse(isAccessibilityServiceEnabled(differentPackage, testPackage, testClass))
  }

  @Test
  fun relativeClassNameResolvesCorrectly() {
    val relativeEntry = "com.appblocker.app/.service.BlockerAccessibilityService"
    assertTrue(
      isAccessibilityServiceEnabled(
        relativeEntry,
        "com.appblocker.app",
        "com.appblocker.app.service.BlockerAccessibilityService",
      ),
    )
  }
}
