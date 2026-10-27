# Farmer Registration Approval Implementation Plan

> **For agentic workers:** Use focused parallel tasks with exclusive file ownership, test-first verification and one final fresh whole-branch review.

**Goal:** Replace invitation links with registration using farm codes and owner dashboard approval on Web/API/Android.

**Architecture:** Keep Identity accounts separate from farm membership. Shared enrollment services persist requests and owner decisions transactionally. A SQL-first upgrade preserves existing records; registration and clients use the same enrollment contracts.

**Tech Stack:** ASP.NET Core MVC/API, EF Core/SQL Server, Kotlin/Compose.

**Spec:** `docs/superpowers/specs/2026-10-27-farmer-registration-approval-design.md` (approved October 27, 2026).

## Global Constraints

- Keep existing accounts, memberships, ownership and farm permissions.
- Only owners decide requests; approval grants FieldWorker (SQL role 2).
- Pending accounts may log in, but have no farm access.
- No new dependencies, public farm directory, global admin, notification transport or automatic live database updates.
- Retire old invitation routes with clear HTTP 410 responses.
- Code is a 12-character uppercase hex string, displayed with optional hyphens. Normalize case, hyphens and outer whitespace; a code authorizes requesting only.
- Work on feature/farmer-registration-approval. The user explicitly requested immediate implementation; continue without another plan-approval pause.

## Shared Interfaces

- `Farm.JoinCode: string`, generated securely for new farms and backfilled for existing farms with a unique index.
- `FarmJoinRequest`: Guid Id/FarmId/UserId, string Status (Pending/Approved/Rejected), DateTime RequestedAtUtc, Guid? ReviewedByUserId, DateTime? ReviewedAtUtc.
- `FarmJoinRequestDetails(Guid Id, Guid FarmId, string FarmName, Guid UserId, string FullName, string Email, string Status, DateTime RequestedAtUtc, DateTime? ReviewedAtUtc)`.
- `FarmEnrollmentOverview(string JoinCode, IReadOnlyList<FarmJoinRequestDetails> Requests)`.
- `IFarmEnrollmentService.RequestAsync(Guid actor, string code, CancellationToken ct=default)` returns details; `MyRequestsAsync(Guid actor, ct)` returns details list; `OwnerOverviewAsync(Guid farmId, Guid actor, ct)` returns overview; `DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, ct)` returns Task.
- Shared registration: `RegisterAsync(string name, string email, string password, bool joinFarm=false, string? farmCode=null)` returns IdentityResult; farm-code errors have Code="FarmCode". Joining account and request commit together.
- Auth register JSON extends existing fields with `joinFarm` boolean and optional `farmCode`.
- Authenticated API GET/POST `/api/farm-join-requests`: list caller's requests / request body `{farmCode}`.
- Owner API GET `/api/farms/{farmId}/join-requests`: overview; POST `/api/farms/{farmId}/join-requests/{requestId}/decision`: body `{approve}`. Other roles receive existing not-found semantics.

## Review Focus

- A failed or invalid join registration must not leave a partial account.
- Concurrent/repeated requests or decisions must not duplicate membership or overwrite decisions.
- An applicant cannot choose role/farm/user through forged decision fields.
- New code-backed registrations must preserve owner signup and existing memberships.
- Cached/offline Android screens must not claim an approval or leak owner-only codes to another role/account.

### Task 1: Persist codes and requests (schema worker)

**Files:** Core Farm and FarmJoinRequest entities; Infrastructure DbContext, EF configurations/migration/snapshot; database scripts/verification; schema tests and database documentation.

**Interfaces:** Produces entities above and a unique pending-request index on FarmId/UserId filtered Status='Pending'. Decisions preserve historical requests. No SQL executed against the user's application database.

- [x] Add schema/model regression checks first and observe missing-field/table failure.
- [x] Implement models, non-destructive rerunnable upgrade, fresh schema and verification changes. Existing farms get unique codes; rows and memberships remain intact.
- [x] Run schema tests and disposable SQL upgrade checks if available; record unavailable checks.

### Task 2: Enrollment rules, registration and API (backend worker)

**Files:** Core enrollment contracts/interface; Infrastructure enrollment repository/service and DI, AccountRegistrationService, farm creation/legacy invitation service; API auth and enrollment/retired invitation controllers; backend enrollment/API tests.

**Interfaces:** Implements Shared Interfaces, using task 1's entities. Use existing repositories/unit-of-work patterns, serializable transactions/conditional writes and unique indexes for race safety. Registration shares the same scoped context/transaction. Do not alter Web or Android files.

- [x] Write tests for invalid/valid registration, no partial account on failure, duplicate/repeated decisions, authorization, disabled/archived records and reapplication.
- [x] Observe tests fail, implement shared rules and routes, reject retired links, and protect code generation against collision.
- [x] Run backend tests, update affected API workflow fixtures and record results.

### Task 3: Web registration, pending status and owner dashboard (coordinator)

**Files:** Web Account/Farms/Dashboard controllers, registration/farm/dashboard viewmodels and Razor views; rendered Web tests; authentication/farm documentation.

**Interfaces:** Consumes task 2 service signatures. Owner overview is loaded only after existing farm authorization; no owner code in non-owner HTML. Signup offers join vs owner choice, validates required code when joining, retains non-password values on errors. Existing accounts submit code from My farms. Dashboard and member management show named approve/reject forms with antiforgery.

- [x] Write rendered registration/pending/dashboard and retired-flow tests; verify missing controls/routes fail.
- [x] Implement Web flow and status/error feedback; remove link-based invitation UI.
- [x] Run targeted and full .NET checks; report two known baseline failures separately.

### Task 4: Android registration and owner approval (Android worker)

**Files:** Android auth/farm DTOs/API/repositories/viewmodels/screens/NavHost; JVM/API contract and Compose tests only.

**Interfaces:** Consumes API routes and camelCase JSON above. Registration callback adds joinFarm/farmCode, adapting all call sites/tests. FarmUiState carries own requests and owner-only enrollment overview. Fetch approval data online only; clear on logout and role/account/farm change. Do not store join codes/applicant emails in shared offline farm cache.

- [x] Write behavior/contract tests for signup choice/code, pending display, owner approval and role isolation; observe RED.
- [x] Replace invite/token UI with code submission, owner code/pending controls; preserve scrolling/keyboard/drafts and existing farm workflows.
- [x] Run JVM and emulator checks, updating affected prior UI tests for intentional new controls.

### Task 5: Integrate and review

- [x] Reconcile all contracts and run schema/.NET/Node/Android checks; inspect meaningful failures.
- [x] Fresh whole-branch review for security, transactional behavior, SQL upgrade and UI continuity. Fix important findings with regression tests.
- [x] Commit implementation and provide results, upgrade instructions and any remaining checks/risks. Follow established PR-to-dev workflow when ready.

## Final verification and review

- .NET: 305 passed, two known baseline failures (report-route date query and development health expectation).
- Web UI: six rendered enrollment/antiforgery tests pass; Node checks 2/2; schema Python checks 15/15.
- Android: JVM 18/18; emulator 35/36, with only the known invalid-JPEG photo fixture failure. New registration/code/role/refresh checks pass.
- SQL: five isolated integration tests pass, covering the full farm-to-harvest workflow, registration rollback, synchronized signups, repeated/concurrent requests and decisions, reciprocal owner/applicant locking and injected code collision. Populated database upgrade was applied twice in disposable databases and preserved existing rows and stable codes; schema and EF migration equivalence passed.
- Fresh review found and verified fixes for signup and reciprocal-approval deadlocks; signup uses ReadCommitted while explicit farm locks serialize enrollment. Active-user reads retain shared HOLDLOCK to prevent disabling during a decision. The approval notice now confirms the decision without making a stale claim about current membership. Registration radio IDs are unique after a failing regression.
- No application database schema upgrade was applied. The deployment step is explicit in database/README.md. The backfill takes a Farm table lock; apply it before restarting hosts/clients. Manual browser, physical-device and TalkBack checks were not performed.
