---
name: accessibility-rule
description: Create or change a YouTube/Instagram detection rule from captured device evidence. Use whenever a rule needs a resource-id, class, flag or bounds pattern.
---

# accessibility-rule

Detection rules must come from evidence captured on a device. Never invent selectors. Never copy selectors from other projects without device confirmation.

## 1. Capture evidence
1. Use only the owner's own test account, on the emulator.
2. Open the screen to capture. Capture one screen per file.
3. Dump the hierarchy:
   ```powershell
   adb shell uiautomator dump /sdcard/window.xml
   adb pull /sdcard/window.xml
   ```
   Alternative: a debug-only extractor log that prints only the fields in step 2.
4. `uiautomator dump` may fail on animating video screens. Retry while the video is paused, or use the extractor.

## 2. Redact immediately
1. Keep only: resource-id, class, selected/visible flags, bounds.
2. Strip all text and content-desc.
3. Delete the raw dump in the same session, on the device and locally:
   ```powershell
   adb shell rm /sdcard/window.xml
   Remove-Item window.xml
   ```
4. Never commit, log or keep a raw dump. Keep no screenshots by default.

## 3. Encode fixtures
1. Store each redacted capture verbatim as a text fixture in `app/app/src/test/resources/fixtures/<app>/<screen>.txt`.
   - Each node is one line: `<indent>class|resource-id|sel=<bool>|[l,t][r,b]`. Two spaces of indent per depth level.
   - Put the `#` header lines described in step 6 at the top.
   - Tests parse this format with a small helper. Add no JSON library.
2. Name fixtures by screen, not by app version.
3. Check that every non-`#` line matches the format above, so no text slipped through.

## 4. Write the rule
1. Viewer rules (Back on entry) must require active-player evidence, not only the presence of an entry point.
2. Entry-point rules (overlay masks) must never produce Back.
3. Match on the most stable signal that the evidence supports. Prefer resource-id plus class plus flags over bounds.
4. Unknown screens trigger no Back and no new mask; existing masks are cleared.
5. Handle only `com.google.android.youtube` and `com.instagram.android`.

## 5. Negative fixtures (mandatory)
Add a fixture that must produce no Back for each screen that exists in the target app. Include at least the negatives listed in the task:
- Home feed.
- Normal (non-Short, non-Reel) video.
- Direct messages (Instagram).
- Profile or channel.
- Search.

A rule without these tests is incomplete.

## 6. Record capture metadata
Put this in a header comment of the fixture file. Do not put it in `docs/COMPATIBILITY.md`, which holds only verified device behavior. A fixture is not device validation.
- App versionName:
  ```powershell
  adb shell dumpsys package <pkg> | Select-String versionName
  ```
- Android version, locale and capture date.
- Which fixtures the evidence supports.

## 7. Validate
1. Run the `android-validation` skill.
2. Fixture tests are NOT device validation. Report Device as NOT RUN unless the rule was exercised on the emulator.

## Report
- Rule and files changed.
- Evidence source and app version.
- Positive and negative fixtures added.
- Anything UNKNOWN.
