# Compatibility

This file lists only behavior verified on a real device or emulator. Any combination not listed here is **unverified**. Fixture or unit-test results do not count as verification.

## Verified configurations

| Date | Device | Android | Locale | Target app | Behavior | Result |
|---|---|---|---|---|---|---|
| 2026-11-19 | AVD `Medium_Phone_API_36.1` (x86_64, 1080x2400) | 16 | en-US | YouTube 21.41.525, signed out | A: Shorts viewer blocking (rule `yt-reel-player-v1`) | PASS for the scenarios below |

## YouTube 21.41.525: scenario results (2026-11-19)

Setup: debug build, service enabled with adb (emulator only), "Block Shorts viewer" on and masking off.

| Scenario | Result | Notes |
|---|---|---|
| Shorts from the bottom Shorts tab | PASS | One Back, about 316 ms after the tap (logcat marker to the Back decision). Lands on the Home tab. |
| Short from search results (Shorts grid) | PASS | One Back, about 370 ms after the tap. Returns to the search results. |
| Search results page with a Shorts grid visible | PASS (no action) | The entry points stay visible. Masking is not implemented (T-306). |
| Normal long-form video (watch page) | PASS (no action) | |
| Home tab, channel page, miniplayer | PASS (no action) | |
| Toggle off while YouTube is open, then Shorts tab | PASS | The viewer stays open and no Back is sent. |
| Toggle on, then return to YouTube with Shorts open | PASS | One Back. The viewer closes. |
| User presses Back about 150 ms after opening Shorts | PASS | The service sent no extra Back, and the app stayed in YouTube. |
| Open Shorts, then Home immediately, then reopen YouTube | PASS | One Back in total. The app ends in YouTube without the viewer. |
| Service disabled and re-enabled, then Shorts tab | PASS | One Back. |

**Not verified:**
- External Shorts link: no link was available without inventing a video ID.
- Home-feed Shorts shelf: signed-out Home shows no feed.
- Signed-in account.
- Rotation and landscape.
- Spanish locale.
- Retry cap with a persistent viewer: covered by unit tests only.
- Enabling through the Settings UI on a sideloaded install with restricted settings.
- Physical devices.
- Visible frames before Back: not measured. Make no zero-exposure claim.
