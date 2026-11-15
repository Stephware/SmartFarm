# Phase 4 data-state refinement — 2026-11-16 (Asia/Manila)

Scope: PR #134, existing `feature/anigo-phase4-data-states`; baseline
`56051f4753ca35ce4662fd709892c9616dbf1b53`, base `430ea08` (Phase 3 merge).
This is dated branch evidence, not merge/deployment acceptance. UI Phase 6's
approved landing redesign remains reserved; public source/PNG goldens and pixel
thresholds were not redesigned or replaced.

## Changes and constraints

MVC: Supplies uses 25-row view pages with search/filter/farm context and whole-farm
counts preserved; Notifications uses 20-row pages within the existing 100-most-recent
repository window. Mark-read retains page/filter and the view clamps out-of-range
pages after records disappear. ID tie-breakers stabilize view ordering and the SQL
selection before its existing Take(100); recipient predicate and limit are unchanged.
Recent-unread wording and a 100-record scope notice avoid claiming all history is
read. First-use Supplies removes useless filters/zero counters, retaining direct
role-gated creation. Scoped CSS fixes mobile flex bases (previously a 404px form /
95.5px search field), compact/start-aligned empty guidance and keyboard-visible paging.

Android: successful cached-empty lists are distinct from missing cache, loading and
failure. Failed/unfinished task refreshes do not claim no work/zero due tasks. Loading
names the actual resource and exposes one labeled indeterminate-progress announcement.
Retry controls retain disabled/theme semantics. Supplies cards keep long names and
full supported balances readable at 320dp/160% text: the amount receives the full
flow row instead of its too-small intrinsic width. No digits/font scaling/precision
are removed. Offline guidance retains snapshot date, staleness, queued sending and
online-settings limits with less repeated text. No ViewModel, DTO/API, schema,
permissions, token handling, Room/WorkManager or business-write changes.

The enrollment CI failure at the earlier head was an undelivered physical tap:
expected submitted code, actual empty callback value; logcat showed IME movement and
hide only after failure. The test now hides IME, waits for actual hidden insets,
asserts displayed/enabled and physically taps. Exact submitted code, exactly one
callback, pending-access restrictions and invalid-code draft/error assertions remain.
No longer timeout, skipped test, direct callback or semantic-click bypass was added.

## Verification observed locally

- Normal .NET Release build and 594 tests passed. A stale shared compiler initially
  emitted Razor errors in untouched views; isolated compilation passed, build-server
  shutdown restored a normal full rebuild. Stale no-build results were excluded.
- Android debug/release builds, 65 JVM and all 139 Android 17 emulator tests passed;
  no failures/errors/skips. Final visual-fixture refinement also passed separately.
- 71 Node tests passed. Existing public browser smoke, 220 authenticated-farm,
  210 field-operation, 120 Admin and 192 Phase 4 renders passed. Phase 4 includes
  Owner/Manager/Field Worker, empty/populated/filtered/large data, both themes,
  320/390/768/1440px, working first-create dialogs, page context and 48 zero-violation
  WCAG 2.x/2.1 AA scans. Fixtures are actual Razor with test-only services/data.
- 552 native captures: 20 states × three roles × two themes × four constraints
  (390×844dp, 320×720dp/160% text, 800×1000dp, 720×390dp/130% text), plus first/last
  sorted-list views. Actual display-order endpoints in 100-record crop/supply lists
  are asserted. Native components run on one Pixel 9 Android 17 emulator with
  simulated root constraints, not four physical devices. Visuals/logs remain private.
- Fresh independent source review found no Critical/Important issues and one
  notification-boundary tie issue; its minimal read-order fix is included. No
  deferred product findings. Final pushed-head seven-job CI must be checked on PR134;
  historical or local results alone do not establish that gate.

## Limits and authorization

Live SQL, real weather/Worker/provider/deployment, physical devices, TalkBack traversal
and real-farmer sessions were not exercised in this UI task. SQL-backed tests can
return without DB work when disposable connections are absent; do not infer live
SQL coverage from passing .NET counts. The 100-notification window is preserved and
explicit in the UI; paging is presentation, not a new paginated API. User authorized
commit/push to PR134 only; do not merge into dev/main or enable auto-merge.
