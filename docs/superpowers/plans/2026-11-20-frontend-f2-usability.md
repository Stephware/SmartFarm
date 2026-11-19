# AniGo frontend F2 — dashboard, calendar, pagination and landing simplification

Date: 20 November 2026  
Base: `dev` after F1 merge, commit `c6b137a6e85c55257c718b64f74ff4fc5f225b45`  
Branch: `feature/anigo-frontend-f2-workspace-usability`

## Implemented (frontend only)

1. **Today dashboard (web):** `Today's work` is an independently scrollable task list with keyboard focus, visible task actions, fixed maximum height and `overscroll-behavior-y: contain` to prevent scroll chaining. The preview remains server-scoped and does not claim to be every task; a link opens the full task page.
2. **Calendar (web):** Calendar button opens the actual month picker; selecting *any* date, including days without tasks, navigates to that date's list and hides the calendar. The same Calendar / Change date control reopens the picker. The selected day is a farm calendar `DateOnly`, not a browser/UTC timestamp.
3. **Calendar (Android):** The DatePicker initializes to the farm-local month. A selected date immediately replaces the picker with assignments for that day; Change date reopens a fresh picker. Owner/Manager see farm tasks; workers see assigned and closed tasks, without inventing tasks.
4. **Large lists:** Server-rendered, farm-scoped pagination for web task lists (long views), My Farms, join requests, crop records (deep-link aware), recent harvests (latest 100 only; complete CSV unaffected). Existing inventory and notification paging remains, with notification pager anchors restored. Android farm list, join request history, crop lists, supply lists and task history/management reveal 20 more records on demand; existing LazyColumn virtualization remains.
5. **Landing:** Remove How it works / Features / About / Contact buttons from the public navigation; trim long workflow/story sections; keep value proposition, four core farm modules, create/join account paths and CTA. Remove public source/credits panel and decorative "illustration only" auth labels. Preserve photo provenance and licensing notes internally under `docs/branding/ANIGO_PHOTO_PROVENANCE.md`.
6. **Visual consistency:** Use separate scoped Razor style partial instead of editing the locked generated Tailwind asset; keep keyboard-visible focus, minimum 44px interactive controls, dark-mode palette and reduced-motion support.
7. **Tests:** F2 Node contract checks plus updates to affected legacy landing and Compose calendar/long-list instrumentation tests.

## Intentional scope boundaries

- This **does not change** API contracts, SQL, EF Core, Hangfire, Gemini, email delivery, roles/authorization or offline queue behavior.
- Android `Show more` is client-side incremental display for records already loaded; it does *not* request extra server pages.
- Web report pagination covers the latest 100 loaded entries only; the existing CSV export remains the full-history path.
- Web tasks in `today` and `mine` deliberately remain unpaginated to preserve deep links from the dashboard to individual task IDs; longer `all`, `completed`, `upcoming` and selected-date views paginate.
- Database timestamp storage may remain UTC for correctness. This phase's farm-day selection uses farm-local `DateOnly`; consistent PHT display of all timestamps and broader localizations remain in planned F4.
- F3 mascot/notification guide, F4 full localization and F5 release/device audit remain separate.

## Validation checklist

- [ ] Node `node --test tests/web-ui/*.test.cjs`
- [ ] .NET/Razor tests + CSS parity
- [ ] Full browser smoke: 320/390/768/1440 in light/dark; keyboard scroll; scroll margin; no-JS forms
- [ ] Android compile + unit/instrumentation: DailyWorkScreenTest, ResponsiveListsTest, role access and offline record flows
- [ ] Verify visual changes, especially short public top bar and date-picker calendar card
- [ ] Do **not** merge without explicit user approval; keep any CI errors documented for Codex follow-up

## Hand-off

If Codex is changing the backend concurrently, do not edit API/Worker/Core/Infrastructure from this F2 PR. A separately approved server-side pagination contract may be appropriate later for high-volume production data, but is outside F2.
