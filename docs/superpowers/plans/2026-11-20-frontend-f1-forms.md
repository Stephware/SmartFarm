# AniGo frontend F1 — forms, interactions and feedback

Date: November 20, 2026  
Base: latest `dev` after F0 merge (`add44d95680805c997c0c7ecf730046e18e076f5`)  
Branch: `feature/anigo-frontend-f1-forms-interactions`  
Ownership: Web Razor/JS/CSS and Android Compose/ViewModel/UI tests. No API, SQL or Hangfire changes.

## Scope and implemented behaviors

### Web

- My Farms now offers conspicuous **Create farm** and **Join farm** actions.
- Simple creation uses the existing real MVC Create action with name, nearby address, default `Asia/Manila` and antiforgery; the existing full Create page remains available for detailed map setup.
- Join Farm uses the existing RequestJoin form and permission/approval rules.
- With JavaScript, native dialogs open from actionable links and keep fields focused. Without JavaScript, underlying form sections and links remain accessible.
- A changed dialog draft requires confirmation before Close/Escape/backdrop dismiss. Server-side join validation reopens the dialog on the returned screen; failed creation still returns to the existing full Create screen with its fields preserved.
- Submitting enhanced forms gives accessible “Saving your changes…” status and loading animation without disabling the named submitter needed by MVC.
- New scoped style partial uses existing pine/gold semantic colors and `prefers-reduced-motion` without modifying the locked generated Tailwind CSS asset.
- Existing task, crop, inventory, monitoring, and harvest web forms retain their server-confirmed redirect/validation behavior; this phase does not duplicate mutations with frontend-only success messages.

### Android

- Farm creation and farm joining use Material3 dialogs, with a scrollable form for smaller phones.
- Dismissing a dirty farm entry uses existing `ExitGuard` draft confirmation and never deletes queued offline records. Create remains online-only and the optional farm pin/time zone remains available.
- Removed redundant “My account” shortcut from the farm selection screen. The destination remains in My Farm → Account & saved work.
- Join Farm dialog clears and closes **only on confirmed successful request**; failed requests retain entered codes and inline errors.
- New plot creation and plot editing close/reset after corresponding confirmed saves, retaining drafts when operations fail.
- Supply creation and detail editing close/reset only after confirmed saves and refreshed records. Offline stock movement flows remain unchanged.
- Crop type/planting-plan creation and plan edits close/reset after server-confirmed saves. Existing planned/growing/history views and transition rules remain.
- Saving farm settings returns to the farm overview only after confirmed update; the existing unsaved-change guard remains active.

## F1 acceptance and tests

- Node: `node --test tests/web-ui/*.test.cjs` including `f1-farm-forms.test.cjs`.
- .NET: build and auth MVC tests, including existing real form posts and antiforgery behavior.
- Browser: signed-in My Farms dialog entry, error recovery, focus restoration, Escape and backdrop draft protection, no-JS forms, small/large viewport and both themes.
- Android: build debug/AndroidTest, JVM tests, new `F1FarmDialogsTest`, existing Enrollment, Phase5FormFeedback, BackNavigation/UnsavedNavigation and sync-related regression tests.
- Check real role-based flows for Owner/Manager/Field Worker and that pending offline writes are not misrepresented as confirmed.

## Intentional future-phase work

- F2: task/calendar selection behavior, Today's Work card scrolling, pagination, landing page cleanup and diagnostics wording.
- F3: context-aware Ani guidance/alert overlays.
- F4: full English/Filipino localization, farm-local displayed timestamps, password-recovery UI when Codex backend contract is available.
- F5: final real-browser/device walkthrough, responsive accessibility and regression QA.

## Verification and delivery

The above is the code scope; **do not mark fully accepted without build/test/visual verification**. CI or device blockers must be disclosed rather than labeled passing. Keep branch/draft PR separate from backend Codex changes. Do not merge without explicit authorization.
