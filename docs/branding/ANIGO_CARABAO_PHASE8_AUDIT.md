# AniGo carabao onboarding — Phase 8 audit

**Branch:** `feature/anigo-carabao-onboarding`  
**Draft PR:** https://github.com/Stephware/SmartFarm/pull/145 (base: `dev`; NOT merged)  
**Original Phase 8 audit (historical):** https://github.com/Stephware/SmartFarm/actions/runs/44134677201

## Original Phase 8 verification

- Debug APK compilation: PASS
- Instrumentation APK compilation: PASS
- Android unit tests: PASS
- Pixel 6 / Android API 35 emulator: **28/28 instrumentation tests passed, 0 failures and 0 skipped**
- Included the AniGo onboarding class (17 tests), PhotoAuthScreens, Phase6WelcomeScreen, WelcomeScreen, and FarmOnboardingScreen
- Production-style first-launch navigation, completion store, replay actions and native Back behavior are covered by unit/instrumentation assertions. The full MainActivity welcome test now explicitly follows the first-run guide before checking the welcome screen.
- Ten real Compose screenshots collected through Android MediaStore, exported and validated as PNG:
  - `01-farm-light.png`, `02-crop-light.png`, `03-work-light.png`, `04-harvest-light.png`
  - `01-farm-dark.png`, `02-crop-dark.png`, `03-work-dark.png`, `04-harvest-dark.png`
  - `intro-light.png`, `farm-320dp-large-text.png`
- Evidence artifact: https://github.com/Stephware/SmartFarm/actions/runs/44134677201/artifacts/14352775485

## Original visual review

The real Android screenshots (not concept art) show:

- AniGo logo, pine/gold visual identity, and consistent heading/controls on all four pages
- The carabao's hat, horns, brown body and green bandana consistently rendered in four guide poses
- Sample farm/plot, rice crop plan, pending-sync fieldwork, and partial-harvest cards visibly distinct and legible
- Dark-mode copy and demo cards maintain workable contrast; normal phone layouts have no observed clipping
- 320dp at 1.6× text switches to a vertical illustrated scene + sample card; the details are **scrollable**, not all visible at once, while Skip/Back/Next remain accessible
- The still intro frame has a clear logo, farm landscape, salakot carabao, and visible Skip intro action

**Outstanding creative sign-off:** This carabao is a **new, flatter native vector interpretation** of the previously approved painted/cartoon reference, not an exact asset export. Its face, proportions and rendering finish may differ from that reference. It must be approved visually before promoting to the rest of AniGo or merging the PR. The normal phone design also leaves deliberate breathing room above the fixed navigation buttons; adjust only after comparing with the intended TUKI-style pacing.

## Final refinement — 2026-11-18

- Integrated `dev` at `aeb1d4c` into the existing feature branch in `b7d5411` (48 incoming commits). Resolved the Welcome test by retaining first-run handling and current auth wording. Shared auth/logo, harvest and draft-save fixes are preserved; no Web, API, SQL or business-rule changes remain relative to that dev revision.
- Retained the same native carabao silhouette, horns, hat, face, bandana and six poses. Added restrained coat/hat/horn shading using the existing palette; this remains vector artwork, not a painted-reference reproduction.
- Balanced the lesson group vertically, centered the mascot beside the sample, reduced scene/card spacing, removed redundant sample kickers and the help panel/heading, and bounded wide-screen content to 560dp. Short windows retain 48dp navigation, smaller art and compact fieldwork rows; all work labels and local/pending status remain visible.
- Applied existing charcoal/pine/gold theme roles to sample cards, text and progress. Intro copy uses a solid background in short/large-text layouts and a caption scrim over scenery to maintain contrast.
- Fixed two reduced-motion defects: Back/Next now jump without pager animation, and the OS animation setting is reread on lifecycle changes. Both were reproduced by failing instrumentation regressions before correction.
- Extended real Activity testing through first-run Skip, Welcome replay, authenticated My Account replay and relaunch after sign-out. Tests follow the UI instead of pre-setting completion or clearing app data. Demonstrations still have no ViewModel, API, Room or record-writing dependency.

## Verification and fresh evidence

- Local debug/release APKs, device-test compilation and **69 JVM tests** passed. Solution Release build, **600 reported .NET tests**, **90 Node tests**, and **7 script + 15 schema-tool tests** passed. Database-gated .NET methods do not establish live SQL acceptance without a disposable connection.
- The final local **40-test audit selection passed with zero failures/skips**. Instrumentation now covers **27 onboarding tests**; the dedicated audit selects **40 tests** including the complete loopback farmer journey and adjacent auth/farm screens. The full device suite includes **189 tests**. Exact final-head results and CI links are maintained in [PR #145](https://github.com/Stephware/SmartFarm/pull/145); the original 28-test result above is historical.
- Fresh evidence uses actual Pixel 9 / API 37 Compose rendering locally; CI uses Pixel 6 / API 35. Native portrait/landscape windows were also inspected with emulator rotation restored afterward. Forced-size tablet/landscape captures are layout checks on an emulator, not physical-device acceptance.
- **24 PNGs** are exported and PNG-validated: eight normal animated lesson end states (including Replay), four tablet lessons, four landscape lessons, four scrolled 320dp/1.6× dark lessons, the 320dp/1.6× light lesson, both intro themes, and a short/large-text dark intro. Functional tests separately exercise physical buttons and swipes; capture navigation uses accessibility click actions to avoid transient touch-ripple frames.
- Screenshot creation replaces only the same named image in the reserved `Pictures/AniGoOnboardingAudit` folder, preventing exports from selecting a previous run. The exporter fails on test failure or any missing/non-PNG capture. No assertions, test selection gates or comparison thresholds were weakened.
- The initial screenshots remain available in the historical artifact above. The current exact-head `carabao-onboarding-preview-tests` artifact linked from PR #145 contains the fresh images. Local comparison sheets identify the different emulator versions; they are visual review aids, not pixel-difference acceptance.

## Remaining approval and release limits

1. Keep PR #145 **Draft and unmerged** until the owner approves the refined character and current captures. No original approved raster/reference asset was found in the repository or named available attachments; an exact identity/finish match cannot be verified. Do not substitute a different character.
2. Verify all current-head CI jobs before considering technical readiness; use the PR's results, not historical counts.
3. Physical-device motion smoothness, TalkBack, real-farmer sessions and live SQL/provider/production acceptance remain unverified. Emulator frames and lifecycle tests establish geometry/behavior, not perceived physical-phone performance.

All teaching values remain illustrative and labeled. Completion stays a device-wide private preference; authentication credentials, farm data, permissions and Room/WorkManager contracts are unchanged.
