# AniGo frontend F0 — navigation and UI audit

Date: 2026-11-19  
Base: `dev` at `6fbd0a07bca18c588214291b2a1c13ee798aa5e9`  
Branch: `feature/anigo-frontend-f0-navigation`  
Scope: Source review and safe navigation improvements only. No API, database, Worker, Android business rules, or production changes.

## F0 navigation decisions

- **Root pages**: My Farms, Today, Crops & Calendar, Supplies, and Harvests are navigated through the primary workspace navigation. Do not add Back arrows to roots. Crops and Supplies now use farm-context links without implying a back action.
- **Detail/edit pages**: Display a consistent, accessible 44px Back icon with a real server-side parent URL. Icon is visible; destination label remains available to screen readers and pointer tooltips.
- **Dialogs**: Close/dismiss rather than navigate away. Draft-confirmation behavior remains intact and is reviewed in F1.
- **Navigation context**: Retain same-tab scroll context only for short-lived known destinations. Prefer URL fragments; preserve filtered parent URL when opening a detail page; native browser Back/Forward and fragment scrolling take precedence; validation errors take precedence over scroll restoration.
- **Security**: No new guessed farm URLs, no cross-origin restoration, no cross-farm resource restoration, and no app or database access needed.
- **Visual design**: Existing Pine/Gold tokens, light/dark palette, and single generated Tailwind asset remain unchanged. Minimal shared Razor navigation styles cover both layouts.

## Web screen audit (source-level)

| Screen groups | Existing structure / F0 action | Follow-up |
|---|---|---|
| Public landing, Login, Register | Public entry routes and account actions stay available. Two-factor sign-in now uses Back icon. | F2 landing cleanup; F4 password recovery |
| My Farms and farm creation | Main farm selector is a root; Create Farm now has icon Back to selector. | F1 create/join form ergonomics, My Account |
| Farm details and settings | Back to farm selector and Edit Farm icon routes fixed; Add Plot remains existing workflow. | F1 save/close/feedback and setup |
| Today dashboard | Root view with role-specific tasks, weather and actions. No Back icon added. | F2 Today's Work internal scroll; F3 Ani |
| Tasks / calendar | Filters now anchor to task list and route preserves farm context. | F2 date selection, task paging and list usability |
| Crops & Calendar | Main root retains farm context without a misleading Back arrow. Edit Plan's shared header uses Back icon. | F2 calendar UX; F1 plan form flow |
| Supplies / inventory | Supplies root arrow-free. Stock details and supply editing use icon-based Back, with parent route history where available. | F1 stock entry; F2 pagination |
| Monitoring, inspections, photos | Monitoring workspace now has explicit Back icon. Existing direct anchors still work. | F1 multi-step form feedback; F2 history |
| Harvest entry / report | Harvest detail Back icon; Harvests report root retains primary navigation. | F1 partial harvest form; F2 record paging |
| Weather | Farm weather detail uses icon Back and retains saved source/age information. | F3 alert UI; F4 time |
| Notifications | Secondary inbox uses icon Back; All/Unread tabs anchor to list. | F2 server paging; F3 priority |
| Account / Help / legacy support | Account and Help shared Back header is standardized. Historical support detail now has icon Back. | F4 localization and account recovery |
| Admin: users, farms, audit, monitoring, announcements, support, MFA | Secondary details/sync/MFA pages use icon Back; admin root sections keep sidebar navigation. | F5 responsive/admin role checks |

**Not modified:** Retired features are not revived; existing database/SQL, roles, and any linked historical records are preserved.

## Android source-level audit

| Area | Evidence and F0 decision | Follow-up |
|---|---|---|
| Global Back stack | `BackNavigation.kt` implements back-or-parent and root fallback. PR #146 was merged into dev. No competing navigation stack added. | F5 instrumented verification |
| Headers and destinations | `PageNavigation.kt`, `RecordPage.kt`, `SmartFarmNavHost.kt` centralize Back behavior and header controls. | F5 visual consistency |
| Drafts, exits, overlays | `SmartFarmNavHost.kt` and `UnsavedChanges` infrastructure already protect pending edits; avoid bypassing these handlers. | F1 save lifecycle; F5 regression |
| Farm selection/setup | Inline create/join and setup guide remain present. | F1 bottom sheets/dialogs; F3 Ani setup |
| Tasks/calendar | Calendar selection behavior needs the planned immediate date-task view. | F2 |
| Today and Record | Mobile daily work previews and offline record actions exist. | F2 prioritization/scroll UX |
| Weather/notifications | Alert data and mascot overlay contracts not yet integrated. | F3 after Codex B2 |
| Language/account | Existing screens need comprehensive localization and recovery integration. | F4 after Codex B1/B3 |

The Android audit here is based on source and existing tests, **not** a newly executed emulator pass.

## F0 checks to run before accepting the PR

- Node: `node --test tests/web-ui/*.test.cjs`.
- .NET: Web/Auth solution build and tests; Razor compilation includes both layouts and shared Back partial.
- Browser: Chromium Back navigation fixture checks and authenticated navigation, at 390/768/1440 widths, light and dark modes.
- Confirm that generated CSS asset remains identical to baseline; F0 does not require `npm run css:build`.
- Test direct entry to edit/detail pages, Browser Back, explicit parent icon, filters, POST-return, validation failure, redirected query normalization, deep-linked section and keyboard navigation.
- Android: no production Kotlin files changed, but existing navigation JVM and device tests remain regression gates.

## Known UX work intentionally deferred to approved phases

- **F1 — Forms and interactions:** Create/join farm surfaces, save/close behavior, error draft retention, controls/buttons, account placement, animations and confirmation UX.
- **F2 — Dashboard and pages:** Today's Work scroll, date-picker collapse, appropriate paging, inventory/search, notification history, landing simplification, user-facing developer text cleanup.
- **F3 — Ani:** Contextual first-use setup, meaningful alert presentation, nonintrusive animation and acknowledgment flow.
- **F4 — Language, time, authentication:** English/Filipino on both platforms, farm-local timestamps and password recovery screens.
- **F5 — Verification:** Real-device/emulator and responsive browser walkthroughs across roles, offline/slow network, large text, light/dark, accessibility and full end-to-end regression.

## Constraints and honest verification status

F0 only changes frontend-owned Razor/JS/style/test/docs files. SQL and API contracts remain untouched. The source/code review is complete, but real browser/emulator visual sign-off is pending CI and/or a supported running environment. Do not label F0 fully accepted until those checks pass. Do not merge into dev without authorization.
