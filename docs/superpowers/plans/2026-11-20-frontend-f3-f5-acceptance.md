# AniGo F3–F5 continuation and final frontend acceptance ledger

**Date:** 20 November 2026  
**Working branch:** `feature/anigo-frontend-f3-f5`, based on merged F2 in `dev`.  
**Review:** Draft PR #152. No automated merge/push to `dev` or `main`.  
**Boundary:** Frontend Razor/Compose, localization resources, API consumption, UI tests and visual baselines. Existing backend rules and offline writes remain intact.

## F3 — Mascot, setup and meaningful warnings

| Area | Scope audited and implemented | Remaining verification |
| --- | --- | --- |
| Approved Ani artwork | Web uses existing carabao atlas; Android reuses `AniGoCarabao` poses, rather than generating a different mascot | Verify on actual phones and two themes |
| First farm setup | Step selection follows real plots, planned crop, active crop, and current role. Actions navigate only to existing routes; no automatic record mutation | Role + empty/partial farm emulator journeys |
| Dismiss/replay | Web session/local dismissal scoped to farmer, farm and step; Android uses scoped preferences and replay | Refresh, account switch and restoration |
| Priority advisories | Ani only highlights backend records marked important; dismiss/acknowledge remain separate; stale and synthetic forecasts are not asserted as official PAGASA warnings | Online/offline/role and slow-provider tests |

## F4 — Locale, time and account recovery

| Area | Scope audited and implemented | Remaining verification |
| --- | --- | --- |
| Web localization | ASP.NET resource tables, EN/Fil cookie choice, stable keys, localization checks for Razor/JS and errors | Production translations should be reviewed by a fluent Filipino speaker |
| Android localization | English/Filipino resources and app locale preference, fallback resources, errors, small-screen layouts; `ApiClientFactory` now forwards the selected `Accept-Language` on every request and is tested with `MockWebServer` after an in-process language change | Emulator locale switching, large-text and OS-version checks |
| Timezone | UTC persistence unchanged. Web `FarmTimestamp` and Android `farmDateTime` convert actual instants to farm time, default Manila. Offset-bearing timestamp test added | Verify every historical timestamp on real SQL data and midnight boundaries |
| Web password recovery | Real Get/Post forms, proper antiforgery, NoStore/no-referrer, email-existence-neutral confirmation, reset token/password validation and invalid/complete views | Requires configured SMTP and real test inbox |
| Android forgot-password | Anonymous API request with email validation, busy/failure/neutral-success states; emailed token opens HTTPS web reset form | Requires configured API host, SMTP, and reachable public HTTPS `ApplicationOrigin` |

## F5 — Cross-platform verification inventory

| Screen/workflow group | Web | Android | Test/acceptance focus |
| --- | --- | --- | --- |
| Landing/auth | Updated login→forgot→reset, localized registration and visually reviewed public responsive pages | Recovery request, English/Filipino, draft-aware sign-in and account screens | Auth/CSRF, safe URL, loading/validation, small/large |
| My Farms, create/join | F0–F2 routes/modal contracts retained | Compose dialogs, confirmation guard and account access retained | Empty/multiple farms, join-request owner approval, no duplicate actions |
| Today, tasks/calendar | Inner task scroll, date-scoped views, task deep links, pagination retained | DatePicker closes after day selection, task pages and today action retained | Manila date boundaries, role visibility, navigation continuity |
| Plots and crop lifecycle | Existing forms, deep links, one-active-crop rule retained | Add/edit plot and plan/active/complete flows retained | No mutation from read-only Ani/Gemini; retain failed form drafts |
| Monitoring/photos | Existing upload/privacy controls retained | Offline inspection/photo storage and confirmations retained | Large photos, privacy, save/retry and sync |
| Inventory/harvest | Existing transaction and partial-harvest flows unchanged | Room cached reads, queued writes and visible sync state retained | Atomicity, units, history, nonnegative inventory |
| Weather/alerts | Backend-verified guidance GET renders fallback or approved Gemini messages; explicit crop selection | Online-only authenticated guidance; stale/synthetic notices; optional crop context | Never present Open-Meteo as official warning; cannot auto-change records |
| Notifications | Inbox pagination and role-scoped alert acknowledgment retained | Priority banner + read/acknowledgment | Dedupe, dismissal, failure, offline |
| Profile/admin | Language choice in account; admin role safety retained | Account accessibility including without selected farm | Owner/Manager/Worker/Super Admin and authorization |

## CI checklist and truthful boundaries

- [ ] Node web-ui regression suite on current PR head
- [ ] ASP.NET Core solution build and .NET Auth/API/Worker/Weather/schema tests on current PR head
- [ ] Android assembleDebug, assembleAndroidTest, JVM suite on current PR head (includes new API request-language header regression)
- [ ] Android emulator journey / screen tests on current PR head
- [ ] Authenticated Chromium browser smoke tests on current PR head
- [ ] Reviewed 30 responsive 320/375/768/1024/1440 light/dark public screenshot fingerprints on current PR head
- [ ] Localization validation script on current PR head
- [ ] Live SMTP delivery and link use on a test account **blocked pending private provider settings**
- [ ] Live Gemini responses on fresh, non-synthetic advisories **blocked pending private provider settings and matching database**
- [ ] Existing local development SQL Server populated/empty farm visual regression **not accessible from this GitHub-only session**
- [ ] Dedicated real-phone/manual WCAG review, larger text/offline/slow network **not automatically proven by build or browser screenshot checks**

**Release decision:** Keep PR #152 in draft until required automated checks pass and external/production-only acceptance work is confirmed. A passing build alone is not full F5 acceptance. Never replace real tests with mocks or automatically mark blocked items complete.

## Provider configuration required from the operator

**Web and API (separately):** `Gemini__Enabled`, `Gemini__ApiKey`, `Gemini__Model`; `Email__Mode=Smtp`, `Email__Host`, `Email__Port`, `Email__From`, `Email__Username`, `Email__Password`, `AccountRecovery__ApplicationOrigin=https://<actual-domain>`. Use environment variables or local user secrets, not committed files. Production hosts need HTTPS and correctly shared Identity Data Protection keys.

## Non-regression commitments

No fresh schema, migrations, Hangfire changes, altered crop/harvest/inventory business rules, forced Room sync, automatic Gemini mutations, invented weather data, or unsanctioned branch merge. Android intentionally does not cache new AI advisory responses as current warnings. Forms must distinguish pending offline work from confirmed saves.

**Scope note:** Automated screenshot comparisons cover the referenced public pages; they do not substitute for a complete manual pass of every authenticated role/page. Do not claim that device/SQL/provider operations ran unless their real results are recorded.
