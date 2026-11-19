# AniGo illustrated onboarding — Step 5 verification

**Date:** 2026-11-19  
**Target branch:** `feature/anigo-carabao-onboarding` (not merged into `dev`)  
**Verified Android code commit:** `f65771dc511bd9f7caf10e461dbe4feaf98865ff`  
**Successful CI run:** https://github.com/Stephware/SmartFarm/actions/runs/44535622069  
**Evidence artifact:** https://github.com/Stephware/SmartFarm/actions/runs/44535622069/artifacts/14536058087

## Results

| Gate | Evidence | Result |
| --- | --- | --- |
| Android debug build, Compose UI-test compilation, JVM unit tests | Build and test AniGo onboarding job | PASS |
| Instrumented UI, navigation, animation and lifecycle regression tests | Android emulator / Android 15 (API 35) | **56/56 passed; 0 failed; 0 skipped** |
| Packaged raster sprite / animation variation | `AniGoSpriteAssetTest` and Compose tests | PASS |
| First-run preference, signed-in bypass, replay and sign-out back-stack behavior | `CarabaoOnboardingPreviewTest`, `AniGoSessionNavigationTest`, `FullAppJourneyTest` | PASS |
| Welcome + four farming lessons and accurate offline / harvest examples | Compose instrumentation tests | PASS |
| Reduced motion, pause/resume, swipe, Back, 320dp / 1.6x font, landscape and tablet coverage | Compose instrumentation tests | PASS |
| Real PNG screenshot capture and export | GitHub Actions screenshots | **30/30 images present** |

## Actual screenshot review

Reviewed the captured emulator images for light/dark intro and welcome screens, lessons 1–4, a narrower phone, tablet, landscape, 320dp / 1.6x font, and especially the corrected short-landscape intro at 1.6x font.

**Issue discovered and fixed in Step 5:** the previous short-landscape, large-text screenshot showed only a small portion of Ani below a tall text panel. `AniGoFarmIntro.kt` now uses a balanced two-column layout in compact landscape so Ani, the headline, supporting text, and Skip are simultaneously visible. The test `compactDarkIntroScrollsWithoutCoveringItsSkipAction` now checks actual mascot visibility and viewport bounds. A matching light-theme test `compactLightLandscapeIntroShowsFullMascotAndReadableCopy` and `intro-landscape-light-large-text.png` were added. The corrected light and dark screenshots have been visually checked.

The original illustrated mascot atlas and the production navigation flow remain unchanged. This Step 5 change touches only the Android intro composition, its instrumentation tests, and the screenshot verification script (plus audit documentation).

## Release limitations

- Passing screenshots and UI checks **do not substitute for owner approval** of artistic fidelity to the original approved reference; the source reference was not available for a pixel-accurate comparison.
- CI covers emulator/API 35, not physical-phone GPU smoothness, actual TalkBack navigation on hardware, or farmer acceptance testing.
- This verification does not assert live production authentication, weather, API or SQL service availability; the full app journey uses a deterministic local/mock API.
- PR #145 was **merged on 2026-11-17**. Subsequent mascot-image and Steps 2–5 commits remain on this feature branch and have **not** been merged into `dev`. Reconcile branch divergence and obtain design approval before any release merge.

**Outcome:** the five-step image-based AniGo onboarding plan is technically verified at the Android code commit above. Owner visual/physical-device acceptance remains a separate release gate.
