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

## YouTube 21.41.525: entry-point masking (B), 2026-11-19

Same device and configuration as above, with "Hide entry points (masking)" on. Rule `yt-shorts-tab-v1` identifies the tab by position: the 2nd button in the bottom bar.

| Scenario | Result | Notes |
|---|---|---|
| Home tab, portrait, light theme | PASS | The Shorts tab is covered and blends with the bar. The other tabs do not move, so a blank gap remains. The bar's top divider stays visible because of a 3 px inset. |
| Dark theme | PASS | The mask color `0xFF0F0F0F` blends with the bar. |
| Landscape | PASS | The mask follows the Shorts tab at its landscape bounds. |
| Tap on the masked tab | PASS | No viewer opens and no Back is sent. Note: the top 3 px of the tab (the inset) is not covered. |
| Leave to the launcher, then return | PASS | The overlay is removed when you leave and shown again when you return. |
| Masking toggled off, then on | PASS | The overlay disappears and then reappears without restarting YouTube. |

The Shorts thumbnails in search results and on the Home feed are **not masked**. They have no resource IDs, and identifying them would require reading text. The Shorts sub-tab on channel pages is not masked either. Mask lag while the screen changes was not measured.

## Instagram 456.1.0.56.78 (2026-11-20)

Setup: the same AVD, Android 16, en-US, a signed-in throwaway test account, and a debug build. The service was enabled with adb (emulator only).
- Rule `ig-clips-viewer-v1` (behavior A)
- Rule `ig-reels-tab-v1` (behavior B)

| Scenario | Result | Notes |
|---|---|---|
| Reels from the bottom Reels tab | PASS | One Back about 0.9 s after the tap |
| Reel from a home-feed video post | PASS | One Back about 1.2 s after the tap |
| Reel from an Explore tile | PASS | One Back about 1.5 s after the tap |
| Reel from a profile Reels grid | PASS | One Back about 0.9 s after the tap |
| Home feed, Explore grid, DM inbox, own profile | PASS (no action) | Inline feed videos keep playing in the feed. They are not the Reels viewer. |
| Story / highlight viewer (`reel_viewer_root`) | PASS (no action) | Still open 7 s later. Stories are never blocked. |
| Masking on: Reels tab, light theme | PASS | The tab is covered by ID and blends with the bar. The remaining tabs do not reflow. |
| Masking on: tap the masked tab | PASS | No viewer opens and no Back is sent |
| Masking on: dark theme | PASS (close match) | The `0xFF0F0F0F` mask is visually close to Instagram's dark bar. Exact color not measured. |
| Leave Instagram, or masking off | PASS | The overlay is removed |

**Not verified (Instagram):**
- A Reel opened from a DM, and the return to the thread. Pending: the owner sends a Reel to the test account.
- An external Reel link.
- Rotation, the es locale, and physical devices.
- The profile Reels sub-tab and the Explore grid. These are not masked by design (no unique ID, or an entry-point grid).

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
