# Web and Android frontend conventions

> Sources: SmartFarm repository, source review
> Raw: [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Reference frontend merge](../../raw/project/2026-11-11-reference-frontend-merge.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

> **Planned UX simplification:** [MVP scope decision](mvp-simplification.md). Target Web navigation is Today / Crops & Calendar / Supplies / Harvests; Android is Today / Record / My Farm. The five-tab Android and nine-link MVC descriptions below remain **current implemented source** until refactoring; do not add more UI layers or preserve old sections merely for parity. Note: `PRODUCT.md` now exists, superseding historical "no PRODUCT.md" guidance in the dated record.

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
and follow its actual prerequisites. This checkout has incumbent UI but no
`PRODUCT.md` or `DESIGN.md`. That is context status, not permission to invent product
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
