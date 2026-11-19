# Predictable Back navigation — implementation plan

> Execute inline with `superpowers:executing-plans`; verify each phase before proceeding.

**Goal:** implement the approved navigation brief (2026-11-19) on `feature/anigo-back-navigation-ux`, with one Draft PR to `dev`, no merge.

**Architecture:** retain Navigation Compose and existing MVC routes. Share Android exit handling and a pinned Material header; register meaningful form changes within each destination. Preserve actual history, use explicit parents only when history is unavailable. MVC uses real contextual links.

**Constraints:** no branding redesign, new dependencies, API/SQL/Room changes, permission changes, or deletion of saved work. Preserve unrelated Gradle configuration. No password persistence. Dialogs dismiss before pages; offline queue deletion remains an explicit separate action.

## A — stacks and fallbacks
- [x] Add route-parent regression tests (RED), then centralize safe Back and root/tab/farm/auth navigation (GREEN).
- [x] Verify JVM tests and Android compilation. Actual prior entry wins over a fallback; login removes all authentication history.

## B — pinned accessible header
- [x] Add native header regressions (RED); use one 48dp Back control with contextual title/label across secondary destinations (GREEN).
- [x] Remove scrolling duplicates, preserve farm/sync/notification controls, keep roots arrow-free. Verify native focused tests and both themes/sizes.

## C — unsaved input and overlays
- [x] Add meaningful-edit/keep/discard/receipt regressions (RED); register changes against each destination and guard Back/farm changes (GREEN).
- [x] Cover auth, farm/plot/crop/task forms, activities/inspection/photos, supplies/movements, harvests, and onboarding intro Back. Defaults and read-only filters are clean; confirmed local saves clear forms through existing receipts only.
- [x] Verify native tests, recreation, keyboard/modal ordering and saved offline data retention.

## D — MVC/admin parents and final acceptance
- [x] Add missing-parent regressions (RED), then contextual links near secondary page titles (GREEN), preserving IDs/fragments and MFA restrictions.
- [x] Run .NET, JVM/device, Node, browser/accessibility/visual checks; inspect fresh rendered evidence. Record unavailable environment checks honestly.
- [x] One fresh whole-branch review and confirmed-finding fixes; scoped knowledge/evidence committed and branch published in [Draft PR #146](https://github.com/Stephware/SmartFarm/pull/146) to `dev`. Never merge. Check its final-head CI before promotion.

**Review focus:** farm and account loops; stale farm arguments; auth-history leaks; dirty defaults or post-save false warnings; overlay/IME precedence; photos/queued records; 320dp/large text; accessible Back labels and 48dp targets; MVC role/return safety.

## Verification and delivery status

Implementation A–D and the single whole-branch review/fix pass are complete locally. Android debug/release + 71 JVM; 199-device suite and 13 focused final lifecycle/auth/task regressions; 603 reported .NET, 90 Node, 7 Python and locked CSS checks pass. SQL-gated methods are not live SQL evidence. All existing browser workflows and 18 new contextual-parent/history cases pass in installed Chrome; fresh native/MVC screenshots were inspected. Local Linux-browser container startup was unavailable; Mac visual differences were not accepted as goldens. Published as [Draft PR #146](https://github.com/Stephware/SmartFarm/pull/146). The PR’s final-head checks are the live acceptance gate; this plan does not replace them. See [dated evidence](../../../.local/karpathy-wiki/raw/project/2026-11-19-navigation-ux.md). No merge authorized.
