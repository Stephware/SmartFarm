# Phase 5 forms and workflow review

> Source: current repository, PR #135 and failed CI test artifact; local build/browser/device verification
> Collected: 2026-11-16
> Published: 2026-11-16

PR: https://github.com/Stephware/SmartFarm/pull/135
Branch: feature/anigo-phase5-forms-workflows only.
Base: 933409fa27d5f9bc7a7c3412da80f8fd209d0ccd.
Starting PR head: 6253447a77136664894fa3c9f16d1fc107a83a11.
This capture describes verified refinements to that head; manifest source hashes
identify the exact files. Final remote checks/commit/status belong to PR #135.
No merge into dev or main is authorized or performed. UI Phase 6 landing and
UI Phase 7 dark-palette work remain reserved.

## Confirmed causes and minimal fixes

The failing emulator artifact had 140 tests, 138 passing, 2 failures and no skips.
Both failed before any scroll injection: Create farm matched a heading and a
button, while inventory queried Name after the field became Supply name.
The unchanged local baseline reproduced both failures. Tests now select the real
button/editable-field semantics and current label, retaining scrolling, visibility,
enabled/disabled and business-result assertions. No skips or timeout increases.

Farm name/location now enforce the existing two-character minimum and existing
120/250 maxima before submission. Native error semantics describe the actual
problem. HTML native constraints match the server; no business rule changed.

MVC supply editing used to redirect away on invalid/rejected input. It now
re-renders through the existing authorized GET/item-state checks, preserving all
entered fields and the submitted rowversion. It never replaces that version with
the newly fetched one. Success still redirects normally. Global ModelState error
summary reports failures once and optional settings open on server validation.
Other pre-existing MVC create-form redirects remain; this is not universal draft
recovery. Live service validation/authorization remain authoritative.

Real Chromium showed an invalid field inside a closed optional disclosure could
block submission without becoming visible/focused. The opt-in handler reuses the
existing disclosure/dialog revealer on captured invalid events. Invalid submit
events now actually cancel and report validity. Repeat-submit guard retains named
submitter values, CSRF/rowversion inputs, confirmation cancellations and pageshow
recovery; it does not replace server idempotency. Optional-summary text was 12px;
scoped Phase 5 CSS makes it 14px, preserving the existing 44px target. Three
redundant edit-form eyebrow labels were removed. Public PNG goldens/thresholds,
public pages and theme palette remain unchanged; only compiled-CSS metadata moved.

Native harvest notes and task instructions expanded into oversized fields at
320dp and 160% font scale. Visible text areas now cap at four lines and scroll
internally, preserving all text and existing 1,000/500 limits. A first-task draft
on an empty farm no longer disappears during loading/error/choices refresh.
Creating still requires current choices; initial empty/error/loading states remain
distinct. Submission disables editable choices/text instead of discarding changes.

After refresh, new-task crop/member selections must still appear in the current
eligible choices. Unlisted choices are described truthfully and require explicit
reselection or clearing; no silent conversion into farm-wide/unassigned work.
Existing assignees are different: an owner is assignable server-side but absent
from member-choice rows. No membership-only guard was added to existing task
editing; its original assignee/version remain intact and server-validated.
Planting-plan edit dates now explain reversed/missing dates as the create form does.

Native date value and Choose date text had zero separation at enlarged text.
A clipped/non-overlap-only test missed it; an unclipped 8dp gap/containment check
reproduced the crowding. Weighted date text plus an 8dp gap fixes it without
changing formatting, picker callbacks or accessibility descriptions.

## Verification and review

All listed regressions were observed failing for their actual behavioral causes
before minimal fixes. One fresh read-only whole-branch review found two material
task gaps plus missing edit-date feedback; all were fixed with regressions. Owner
assignment was checked against actual repository/service queries rather than
mistaking the member-choice list for authority. No second review or broad rewrite.

Final normal .NET Release build and 600 tests passed (385 Auth, 103 API, 54 Worker,
37 Weather, 21 schema). Android debug/release APKs and device-test build passed;
65 JVM tests passed with zero failures/errors/skips. Final full emulator suite:
150 tests passed without failures or skips, including enrollment, responsive lists,
full farmer journey, offline/Room/WorkManager, native visual and form workflows.
74 Node tests passed. Public browser smoke plus 806 authenticated Razor renders
passed: 220 onboarding, 210 field operations, 192 Phase 4, 64 Phase 5 and 120 Admin.
Phase 5 includes 16 zero-violation WCAG scans, closed-invalid-field focus, date
feedback, retained field/token payloads, one rapid-submit POST, named submitter
preservation and no-JavaScript disclosure access. Native narrow/enlarged-text,
existing light/dark error/draft captures and representative Web renders inspected.
CI now retains all prior checks and adds the Phase 5 form runner.

Test fixtures and POST captures are synthetic and test-only. No production data,
credentials, generated captures or private logs are included. No Core/API/DTO,
SQL schema, Worker, sync/Room/session, permissions, package dependencies or public
visual-world changes. Local SQL suites had no disposable connection configured;
a green .NET count does not prove SQL work. Live SQL/providers/deployment,
physical hardware/TalkBack, ordinary mobile-network and real-farmer acceptance
remain unverified. No farmer or production-readiness claims.
