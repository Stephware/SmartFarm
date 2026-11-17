# Carabao onboarding refinement source excerpt

> Source: docs/branding/ANIGO_CARABAO_PHASE8_AUDIT.md; AniGoOnboardingStore.kt; SmartFarmNavHost.kt (feature/anigo-carabao-onboarding)
> Collected: 2026-11-18
> Published: 2026-11-18

## Final refinement — 2026-11-18

- Integrated `dev` at `aeb1d4c` into the existing feature branch in `b7d5411` (48 incoming commits). Resolved the Welcome test by retaining first-run handling and current auth wording. Shared auth/logo, harvest and draft-save fixes are preserved; no Web, API, SQL or business-rule changes remain relative to that dev revision.
- Retained the same native carabao silhouette, horns, hat, face, bandana and six poses. Added restrained coat/hat/horn shading using the existing palette; this remains vector artwork, not a painted-reference reproduction.
- Balanced the lesson group vertically, centered the mascot beside the sample, reduced scene/card spacing, removed redundant sample kickers and the help panel/heading, and bounded wide-screen content to 560dp. Short windows retain 48dp navigation, smaller art and compact fieldwork rows; all work labels and local/pending status remain visible.
- Applied existing charcoal/pine/gold theme roles to sample cards, text and progress. Intro copy uses a solid background in short/large-text layouts and a caption scrim over scenery to maintain contrast.
- Fixed two reduced-motion defects: Back/Next now jump without pager animation, and the OS animation setting is reread on lifecycle changes. Both were reproduced by failing instrumentation regressions before correction.
- Extended real Activity testing through first-run Skip, Welcome replay, authenticated My Account replay and relaunch after sign-out. Tests follow the UI instead of pre-setting completion or clearing app data. Demonstrations still have no ViewModel, API, Room or record-writing dependency.

## Remaining approval and release limits

1. Keep PR #145 **Draft and unmerged** until the owner approves the refined character and current captures. No original approved raster/reference asset was found in the repository or named available attachments; an exact identity/finish match cannot be verified. Do not substitute a different character.
2. Verify all current-head CI jobs before considering technical readiness; use the PR's results, not historical counts.
3. Physical-device motion smoothness, TalkBack, real-farmer sessions and live SQL/provider/production acceptance remain unverified. Emulator frames and lifecycle tests establish geometry/behavior, not perceived physical-phone performance.

All teaching values remain illustrative and labeled. Completion stays a device-wide private preference; authentication credentials, farm data, permissions and Room/WorkManager contracts are unchanged.

## Completion preference source

```kotlin
 * Small, device-wide first-run flag for the instructional AniGo tour.
 * so switching accounts or signing out never forces the same introduction again.
    fun isCompleted(): Boolean = preferences.getBoolean(COMPLETED_KEY, false)
    fun markCompleted(): Boolean =
        preferences.edit().putBoolean(COMPLETED_KEY, true).commit()
        private const val COMPLETED_KEY = "completed_v1"
    if (completed) "welcome" else "onboarding"
```
