# Security

Build and supply-chain controls, and how to keep them current. App data handling is in [PRIVACY.md](PRIVACY.md).

## Build toolchain

| Component | Version | Why |
|---|---|---|
| Gradle wrapper | 9.9.2 | Fixes all Gradle advisories published up to 2026-11-19, including GHSA-3cfg-jpcr-gfp3 (critical, version catalog values run as code; fixed 9.9.1) and GHSA-7f7r-77ff-fr6f (snapshot dependencies skip verification; fixed 9.9.2). 9.1.0 was affected. |
| Android Gradle Plugin | 9.5.2 | AGP 9.5 needs Gradle 9.6.0 or newer and supports API 37 (compileSdk/targetSdk 37 unchanged). |
| Kotlin | 2.4.21 | KGP 2.3.20 is affected by GHSA-r937-wjx7-w2jp (CVE-2026-53914, unsafe deserialization in the Kotlin build cache; fixed 2.4.20). |

`distributionSha256Sum` in `app/gradle/wrapper/gradle-wrapper.properties` pins the distribution checksum, so a tampered download fails.

### Upgrade the wrapper

Run from `app/`. Take both checksums from `https://services.gradle.org/versions/all`.

```powershell
.\gradlew.bat wrapper --gradle-version <v> --distribution-type bin --gradle-distribution-sha256-sum <bin.zip sha256>
.\gradlew.bat wrapper --gradle-version <v> --distribution-type bin --gradle-distribution-sha256-sum <bin.zip sha256>
(Get-FileHash gradle\wrapper\gradle-wrapper.jar).Hash   # must equal the published wrapper.jar sha256
```

The second run uses the new Gradle, so it also refreshes `gradle-wrapper.jar`, `gradlew` and `gradlew.bat`. CI (`gradle/actions/setup-gradle`) rejects any wrapper JAR that does not match a published checksum.

## Dependency verification

`app/gradle/verification-metadata.xml` holds a SHA-256 for every artifact the build resolves. Gradle fails the build when an artifact is missing or does not match.

After any dependency or plugin change, regenerate it from `app/` and review the diff:

```powershell
Remove-Item gradle\verification-metadata.xml
.\gradlew.bat --write-verification-metadata sha256 --no-configuration-cache --rerun-tasks :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease buildEnvironment :app:buildEnvironment
```

Then restore the Linux and macOS `aapt2` entries. `aapt2` is OS-specific and Windows resolves only the `-windows` jar. Copy each SHA-256 from the `.sha256` file Google Maven publishes next to the jar (`https://dl.google.com/dl/android/maven2/com/android/tools/build/aapt2/<version>/aapt2-<version>-linux.jar.sha256`).

Dependabot does not update this file. Regenerate it on the Dependabot branch before merging, or CI fails.

## Pinned build-tool dependencies

AGP and its lint tool bring in libraries with published advisories. None of them ship in the APK. Constraints in `app/build.gradle.kts` (plugin classpath) and `app/app/build.gradle.kts` (`androidLintTool`) raise them to the first patched release. Versions are in `app/gradle/libs.versions.toml`.

| Library | Was | Pinned | Advisory |
|---|---|---|---|
| `org.bouncycastle:bcprov/bcpkix/bcutil-jdk18on` | 1.80.2 | 1.85 | GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26, GHSA-wg6q-6289-32hp |
| `com.google.guava:guava` | 33.4.0-jre | 33.7.2-jre | GHSA-xxph-c9ww-hj94 |
| `org.apache.commons:commons-lang3` | 3.16.0 | 3.18.0 | GHSA-j288-q9x7-2f5v |
| `org.apache.httpcomponents:httpclient` (lint only) | 4.5.6 | 4.5.13 | GHSA-7r82-7xv7-xcpj |
| `org.jdom:jdom2` (plugin classpath only) | 2.0.6 | 2.0.6.1 | GHSA-2363-cqg2-863c |
| `org.bitbucket.b_c:jose4j` (plugin classpath only) | 0.9.5 | 0.9.6 | GHSA-3677-xxcr-wjqv |

Remove a pin once AGP resolves an equal or newer version on its own. The scan was an OSV.dev batch query on 2026-11-22 over every component in `verification-metadata.xml`. The app runtime classpath had no known advisories.

## CI and Dependabot

- `.github/workflows/ci.yml`: wrapper validation, then assemble, unit tests, lint and release assemble with dependency verification on. Read-only token. Actions are pinned to commit SHAs.
- On `main` only, a second job submits the resolved dependency graph so Dependabot alerts cover transitive and build-time dependencies. It needs `contents: write` and runs with lenient verification, because the action injects a Gradle plugin that is not in the metadata.
- `.github/dependabot.yml`: weekly Gradle and GitHub Actions updates.
- Dependabot alerts and the dependency graph must be turned on in the repository settings.

## Diagnostic archives

Never commit bug reports, UI dumps, heap dumps or screen recordings. The root `.gitignore` blocks the common names.
