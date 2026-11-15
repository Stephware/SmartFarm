# Phase 3 Android refinement evidence — 2026-11-15

Source: PR #133 on `feature/anigo-phase3-android-layout-refinement`, starting at
`985509679f15065f604cfe711fe533e9daff956c`; Phase 2/base `dev` is `44618de`.
This is dated branch evidence, not a merge or deployment acceptance certificate.

## Implementation and review

Reviewed all affected Android presentation files and their shared callers against
Phase 1 readable native typography/shared spacing and Phase 2 compact hierarchy.
Refined grouped My Farm rows, fully opened scrollable Record sheet, farmer-readable
roles, long-name/status reflow, adaptive action/filter rows, setup footer, quieter
Today empty states and consistent line icons. Added native button roles. Collapsed
weather retains provider and farm-local last-check time. No ViewModel, repository,
Room/WorkManager, API, permission, database or business-rule changes.

Initial PR CI failed the harvest queue assertion despite an earlier timeout increase.
Logcat indicated IME/inset timing around the Save tap; the existing harvest usability
test already hid the keyboard. The full farmer journey now waits for hidden IME
before a displayed/enabled physical tap. Actual queued harvest, confirmed totals,
task/stock, Back, role/farm switching, recreation and sign-out assertions remain.
Weather fixtures now use the farm's Asia/Manila date, not the runner's local date.

## Observed verification

- Android debug/release APK builds, device-test compilation and 65 JVM tests passed;
  zero JVM failures/errors/skips. Release build is compilation, not deployment.
- Baseline: 120 emulator tests passed locally; initial PR CI had 119 passes/one
  farmer-journey failure. New regressions reproduced hidden forecast provenance,
  missing button semantics, clipped sync guidance and three word-splitting layouts.
- Expanded local suite: 128 emulator tests passed, zero failures/skips. After the
  final compact action-width adjustment, all nine affected regression/visual/full
  farmer-journey tests passed again on the final source.
- Inspected native screenshot fixtures: 21 states × two themes × four configurations
  (390×844dp, 320×720dp at 160% text, 800×1000dp, 720×390dp at 130% text): 168 captures.
  Matching LocalConfiguration exercises compact headers and tablet/landscape rails.
  Root constraints are simulated on the Pixel 9 Android 17 emulator, not separate
  physical devices; dialog windows use the emulator's actual window. Also ran the
  native matrix with Android system font scale 160%, inspected both Record-sheet
  themes, and restored the original system setting. Screens/models are test-only.
- Final pushed-head CI remains to be checked on PR #133; do not infer green CI from
  the earlier head. Screenshots/logs stay in private temporary evidence, not Git.

## Limits

Physical devices, TalkBack traversal, real-farmer usability and production/provider
acceptance were not exercised. Independent review identified the timezone fixture
issue but stopped at a model usage limit; complete independent review is outstanding.
The author completed source/diff/render checks. PR #133 stays open and unmerged.
