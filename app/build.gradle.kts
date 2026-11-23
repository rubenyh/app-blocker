// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
  dependencies {
    constraints {
      // Patched versions of vulnerable AGP/KGP transitive build-time dependencies.
      // None of these ship in the APK; see docs/SECURITY.md for the advisory list.
      classpath(libs.bouncycastle.bcprov)
      classpath(libs.bouncycastle.bcpkix)
      classpath(libs.bouncycastle.bcutil)
      classpath(libs.commons.lang3)
      classpath(libs.guava)
      classpath(libs.jdom2)
      classpath(libs.jose4j)
    }
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
}
