# AniGo carabao onboarding — Phase 8 audit

**Branch:** `feature/anigo-carabao-onboarding`  
**Draft PR:** https://github.com/Stephware/SmartFarm/pull/145 (base: `dev`; NOT merged)  
**Final Android audit:** https://github.com/Stephware/SmartFarm/actions/runs/44134677201

## Automated verification

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

## Visual review

The real Android screenshots (not concept art) show:

- AniGo logo, pine/gold visual identity, and consistent heading/controls on all four pages
- The carabao's hat, horns, brown body and green bandana consistently rendered in four guide poses
- Sample farm/plot, rice crop plan, pending-sync fieldwork, and partial-harvest cards visibly distinct and legible
- Dark-mode copy and demo cards maintain workable contrast; normal phone layouts have no observed clipping
- 320dp at 1.6× text switches to a vertical illustrated scene + sample card; the details are **scrollable**, not all visible at once, while Skip/Back/Next remain accessible
- The still intro frame has a clear logo, farm landscape, salakot carabao, and visible Skip intro action

**Outstanding creative sign-off:** This carabao is a **new, flatter native vector interpretation** of the previously approved painted/cartoon reference, not an exact asset export. Its face, proportions and rendering finish may differ from that reference. It must be approved visually before promoting to the rest of AniGo or merging the PR. The normal phone design also leaves deliberate breathing room above the fixed navigation buttons; adjust only after comparing with the intended TUKI-style pacing.

## Scope and outstanding merge gates

1. Do not merge until the owner reviews the 10 actual emulator screenshots and approves the mascot look.
2. Reconcile newer commits on `dev` (notably shared auth styling and logo assets); rerun checks on the updated integration result.
3. Verify animation smoothness interactively on a physical Android phone if available. Automated still captures validate geometry/contrast, **not actual frame timing or perceived smoothness**.
4. Keep the PR in draft until the above review is complete.

No farm/API/Room records are modified by this onboarding. Sample demo values are illustrative and explicitly labeled.
