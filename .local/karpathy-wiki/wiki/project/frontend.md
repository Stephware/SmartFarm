# Web and Android frontend conventions

> Sources: SmartFarm repository, source review
> Raw: [Phase 7 dark-mode verification](../../raw/project/2026-11-16-phase7-dark-mode-verification.md); [Native picker verification](../../raw/project/2026-11-16-phase5-native-picker-verification.md); [Final Phase 5 native appearance](../../raw/project/2026-11-16-phase5-final-native-appearance.md); [Phase 5 form review](../../raw/project/2026-11-16-phase5-forms-review.md); [Phase 4 data states](../../raw/project/2026-11-16-phase4-data-states-review.md); [Phase 3 Android refinement](../../raw/project/2026-11-15-phase3-android-refinement.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md); [MVP retirement](../../raw/project/2026-11-14-mvp-stage-5.md); [Phase 4 final CI](../../raw/project/2026-11-16-phase4-final-ci.md)
> Updated: 2026-11-16

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

**Current MVP:** Web has Today / Crops & Calendar / Supplies / Harvests; Android has Today / Record / My Farm. Stage 5 removes duplicate plot/timeline pages, cash/charts, decorative crop art, public bulletins and ticket creation/replies. Help and protected read-only histories replace those entry points; security/diagnostics remain. Earlier five-tab/nine-link and finance/bulletin UI descriptions below are historical. [MVP scope/status](mvp-simplification.md) and `PRODUCT.md` supersede them.

## Current visual and interaction system

The current implemented look reconstructs the supplied AniGo reference in Harvest
Gold + Pine, merged through PR #86. It follows the earlier multi-phase workspace overhaul. Web uses Razor, CSS, prefixed Tailwind utilities
and focused JavaScript; Android uses Material3 Compose. No new UI framework is
needed to extend either implementation.

Web `_Layout.cshtml` supplies guest/authenticated shells, sidebar, topbar and
responsive navigation. `_FarmNavigation.cshtml` preserves farm context and current
section. `site.css` contains base tokens and later overrides; inspect the effective
cascade rather than using an early rule as the final palette. Feature styles live
in dashboard, crop-art, fieldwork, harvest-inventory, insights and access-polish CSS.
`reference-ui.css` loads last and defines effective reference geometry/tokens.
Default appearance is light regardless of OS; `color-mode.js` reads only an explicit
`anigo-color-mode` choice before paint. Keep the separate dark palette functional.
Use existing form prefixes, unique labels/IDs, linked validation summaries,
antiforgery, server encoding and confirmation patterns. Some task/finance forms
still redirect after errors: universal draft recovery is not implemented.

Native dialogs progressively enhance existing forms, preserving their POST fields
and inline no-JS fallback. Initial/hash-changed links reveal dialogs and crop
schedule disclosures. Closing a dialog retains its draft in the same DOM. Web
Tasks has a real farm-local monthly calendar; Crops date dots reveal the complete
schedule. Retain every real record and truthful empty state, never reference demo metrics.

Android `Theme.kt` supplies light/dark pine/gold schemes, sans-serif typography
and shared shapes. Reuse `FarmDesignComponents`, `RecordPage`, native date/timezone
inputs, status/offline notices, pending records and confirm-action components.
Workspace bottom tabs preserve the active farm and appear on primary routes; a rail
serves expanded screens. Headers honor system-bar insets. Appearance defaults to
light and persists only the selected boolean; system-icon brightness follows it.
The Account appearance row is one labeled switch. Native Tasks Calendar uses
DatePicker and real assignments; it is not a monthly crop agenda.
Keep large lists lazy with stable keys and inputs reachable with the keyboard.

Crop artwork is local SVG on Web and vector drawable on Android with safe unknown
fallback. Plot layout is schematic, not geographic/cadastral mapping. Location
selection is a separate map/search surface with manual coordinate fallback.

## Phase 7 charcoal dark mode (PR #138)

MVC and Compose now use charcoal backgrounds, pine navigation and harvest-gold
accents, with accessible mint links and separate action colors. Preserve the light
palette and Phase 6 composition. Native framework dialogs do not automatically
inherit Compose colors: `DateInput` explicitly selects the dark picker style only
for the saved dark choice, retaining the default light dialog and ISO callback.
Account switching is tested through the real control and persisted preference.

Welcome photo content grows with enlarged text; image overlays and landing CTA
contrast remain readable in either mode. Keep landing navigation, actions and
credits wrapping under text enlargement. Public baselines were regenerated from
the actual MVC host with pinned Linux Chromium, inspected, then compared without
relaxing the gate. Dark images intentionally changed; light auth images remain
identical. This is agent visual review, not farmer testing or hardware acceptance.

[Dated verification and limits](../../raw/project/2026-11-16-phase7-dark-mode-verification.md)
record local builds, regression suites and emulator/browser checks. Read PR #138
for final exact-head CI and review/merge status. No merge is authorized by this task.
Earlier Phase 4/5 references to reserved Phase 6/7 work are historical.

## Phase 3 Android refinement (PR #133, 2026-11-15)

Phase 1–2 standards remain readable native sans-serif type, shared spacing and
compact progressive disclosure. Phase 3 uses grouped My Farm rows, a fully opened
scrollable Record sheet, quieter Today/task/crop controls and optional setup/help/
forecast details. Long names, roles and action/filter rows reflow at large text
sizes; navigation rows expose button semantics. Display `Field worker` without
changing the `FieldWorker` role code. Keep saved forecast source and farm-local
last-check time visible before expanding daily details.

Preserve Today / Record / My Farm routes, all role gates, form validation and
pending/server-confirmed distinctions. This refinement changes presentation and
test readiness only; ViewModels, repositories, Room/WorkManager and API contracts
are unchanged. The farmer-journey test waits for the keyboard to hide before
physically tapping Save; it still verifies the actual queued harvest and confirmed
totals. See the dated raw evidence for build/device checks and their limits. PR
#133 targets `dev`; this task does not authorize a merge.

## Phase 4 data states (PR #134, 2026-11-16)

Keep genuine empty, in-flight, failed, saved-empty and populated states distinct.
Cached empty crops/supplies describe the saved snapshot, not missing cache; failed
or unfinished task refreshes never show an all-clear. Native loading merges its
resource label and indeterminate-progress semantics; retries remain real actions.
Keep names and every balance digit readable at large text sizes rather than
truncating values or reducing the user's font setting. Reuse the compact shared
empty/error components and preserve snapshot date/staleness/sending cautions.

MVC Supplies pages show 25 matching rows while counts retain full farm scope.
Notifications shows 20 rows per page within the existing 100-most-recent window;
label that scope, including unread-empty messages. Preserve page/filter after
mark-read, clamp after count changes and order ties by ID before the repository
limit as well as in the view. Empty first-use lists need guidance and the permitted
next action, not useless filters. Mobile column layouts must reset desktop flex
bases; inspect the computed cascade and keyboard focus, not just source selectors.

[Verification evidence](../../raw/project/2026-11-16-phase4-data-states-review.md)
distinguishes local native/browser checks from live SQL, hardware and deployment.
UI Phase 6's landing redesign remains planned; public goldens/thresholds are unchanged.
The task did not authorize a merge. Final application head `2b4ccdc` passed all
seven CI jobs, including all 139 emulator tests without failures or skips. GitHub
subsequently records an external merge by `Stephware`; this agent did not merge.
See [final CI and integration evidence](../../raw/project/2026-11-16-phase4-final-ci.md).
Later workflow/context maintenance remains distinct from that application head.

## Phase 5 forms and workflow feedback (PR #135)

Use semantic button/editable-field selectors in native tests; text alone may match
both a heading and a submit button. Check real lazy-list composition, keyboard
insets and unclipped bounds separately before calling a failure a scrolling bug.
Keep date value/action spacing and long-note fields usable at enlarged text.
Let the date action wrap when needed instead of squeezing the value into a narrow
column; verify actual text layout as well as non-overlap. Native picker tests
require foreground host focus and Espresso's dialog root; select a changed date,
confirm once and assert the ISO callback. Do not hide focus failures by reopening.

MVC opt-in forms reveal concealed invalid controls, cancel invalid submissions and
preserve named submitters. Supply-edit validation re-renders the draft with its
submitted rowversion, never silently rebasing it; reuse live edit-access checks and
the shared error summary. This does not establish universal create-form recovery.

Native task drafts survive busy/error/choice refresh. Freeze in-flight editing and
require current, explicitly resolved new-task choices. Never silently clear a crop
or assignee. Existing owner assignments can be absent from member-choice rows:
keep server validation authoritative rather than applying membership-only client
rules to edits. Match required-field/date/length feedback across create and edit.

[Verification evidence](../../raw/project/2026-11-16-phase5-forms-review.md) records
150 local device tests and scoped browser checks, with SQL/hardware/provider limits.
UI Phase 6 landing and UI Phase 7 palette work remain separate planned scope; this
task authorizes review-only PR updates, not a merge.

## Product and accessibility constraints

Prioritize farmer-readable language, large usable targets, visible labels/focus,
legible contrast, narrow-screen reflow, dark mode and large fonts. Pair status
color with text. Keep pending/offline/stale/server-confirmed states distinct.
Read-only roles must have meaningful history/empty/error states; server services
still enforce authorization. Decorative imagery must not carry sole meaning.
Inspect loaded HTML/CSS and rendered screens before declaring visual quality.
Unit/source/semantics checks cannot prove the result at real device sizes.

## Dated visual verification

PR #86 passed 186 Playwright captures at 1440, 768 and 390 pixels, role/form
interactions and a Pixel 9 emulator suite. Actual MainActivity light/dark screenshots
verified system-bar spacing/readability. These are dated checks of that revision;
physical-device and production/provider acceptance remain unverified.

## Applying Impeccable in future work

Use the installed `impeccable` skill for UI design, audit or refinement. Run its
context command once, select only the relevant playbook and platform guidance,
and follow its actual prerequisites. This checkout now has `PRODUCT.md` (2026-11-13) defining the planned MVP
simplification; no additional design-spec document is required solely to explain
that decision. That is context status, not permission to invent product
requirements or redesign during a documentation task. New-surface/redesign flows
require the skill's product-context step; narrow refinements can use incumbent
code as its instructions permit. Browser-only tools do not apply to native Compose.

Preserve current architectural/business constraints while reviewing Owner,
Manager and Field Worker flows. Check offline/retry/stock-conflict states,
empty/loading/failure states, keyboard, large text, screen-reader semantics and
consequential-action cancellation. Reuse existing assets/components and minimal
dependencies under Ponytail; never trade accessibility or correctness for brevity.

## Source navigation

[Web shell](../../../../src/SmartFarm.Web/Views/Shared/_Layout.cshtml),
[Web styles](../../../../src/SmartFarm.Web/wwwroot/css),
[Web scripts](../../../../src/SmartFarm.Web/wwwroot/js),
[Android theme](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/theme/Theme.kt),
[Android components](../../../../mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components),
[UI tests](../../../../tests/SmartFarm.Auth.Tests/UiOverhaulPageTests.cs).

## See Also

[Architecture](architecture.md), [Android](android.md), [Features](features.md),
[Development](development.md), [Progress](progress.md).
