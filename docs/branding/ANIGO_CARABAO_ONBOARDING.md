# AniGo carabao onboarding — Phases 1–3 review

## Scope and review workflow

This branch prototypes the approved carabao-guided Android onboarding without changing the web UI, authentication, API, farm database or persistence of the walkthrough.

- Phase 1: `AniGoCarabao.kt` contains a reusable, native Compose Canvas carabao illustrated in six expressive poses: `WAVE`, `GUIDE`, `PLANT`, `RECORD`, `HARVEST`, and `CELEBRATE`. All poses share the same straw hat, cream horns, brown fur and green AniGo bandana. `frame = 0..3` drives the original arm/gesture variations; optional `walking` switches on alternating leg steps during the intro. Informational slides remain still.
- Phase 2: `AniGoOnboardingScreen.kt` uses four swipeable learning pages, the existing AniGo logo, progress segments, a responsive field illustration, sample UI feature cards, Back / Next / Skip, and a final Continue button. Content is entirely illustrative and explicitly labeled.
- Entry: select **Explore AniGo with our farm guide** on the Android Welcome screen. The new `onboarding-preview` route returns to the normal Welcome screen with Skip or Continue. Login, registration, diagnostics and current welcome photograph remain untouched.
- Light and dark themes follow the existing `LocalFarmAppearance` setting. Guide copy and controls scroll/fit on compact phones and larger accessibility text.
- Phase 3: `AniGoFarmIntro.kt` runs a **single 2.55-second animated scene** when the opt-in guide opens: a carabao walks across a layered field, alternates native vector step frames, stops, waves, and the AniGo logo/headline fade in. An explicit **Skip intro** button or Android Back key immediately opens page 1. The sequence has no repeating loops or remote media.
- If Android's global animation duration scale is disabled, the introduction is bypassed and page 1 is shown directly. Intro completion is saved across configuration changes in the guide via `rememberSaveable`; there is no first-launch persistence yet.
- A single carabao vector implementation is intentional: do not use per-page separately generated character PNGs, which can introduce inconsistent horns, hats, eyes or scale. Inspect the **actual emulator rendering** against the approved reference before wider animation rollout.

## Screens and demonstrations

| Page | Carabao pose | On-screen example | What is taught |
| --- | --- | --- | --- |
| 1. Your farm, organized | GUIDE | Sample farm and plot cards | Create a farm or join a team with owner approval |
| 2. Grow with a plan | PLANT | Rice planting milestones | Crop plans and planting calendar |
| 3. Record work as you go | RECORD | Watering, inspection and photo checklist | Daily recording and accurate offline/sync language |
| 4. From field to harvest | HARVEST | Two sample partial harvest records | Harvests, supplies and distinct measurement units |

The source of truth is `PRODUCT.md`: Android's real navigation remains Today, Record and My Farm. The guide is an **optional preview**, not a fourth top-level destination. No example data is written to a production/local database.

## Follow-on work deliberately deferred

- Phase 3 is implemented: skippable one-shot intro, alternating walk frames and wave pose.
- Phases 4–5: animated / staged feature demonstrations (the current cards are intentionally static).
- Phase 6: onboarding animation refinement and integration after artwork approval.
- Phase 7: first-run completion preference, replay-from-settings behavior, and any automatic first-launch routing.
- Phase 8: final accessibility, performance, screenshot review and merge decision.

## Verification criteria

1. Android debug compile, Compose UI test compile, unit tests.
2. Real emulator: intro Skip/Back, four screens, swipe and buttons, Skip/Continue returning to Welcome, no auth regressions.
3. At 320dp width and 1.6× font size, accessible controls stay visible and descriptive content remains scrollable.
4. Review artwork at 320dp and normal device widths; check horn/hat/body/prop alignment and dark-mode contrast.
5. Preserve opt-in entry and the `WelcomeScreen` default callback contract used by existing tests.

**Artwork note:** Phase 1 is a first native vector interpretation of the approved reference, not an exported/cropped copy of the user's previous generated raster mockup. Intro motion now uses that geometry; its final visual approval is still needed before animating or promoting it into other pages.
