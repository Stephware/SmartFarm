# Web and Android frontend conventions

> Sources: SmartFarm repository, source review
> Raw: [Phase 4 final CI](../../raw/project/2026-11-16-phase4-final-ci.md); [Phase 4 data states](../../raw/project/2026-11-16-phase4-data-states-review.md); [Phase 3 Android refinement](../../raw/project/2026-11-15-phase3-android-refinement.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md); [MVP retirement](../../raw/project/2026-11-14-mvp-stage-5.md)
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
