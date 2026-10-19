# Phase 1 — SmartFarm stabilization

## Integration audit

The existing `dev` branch contains the original authentication,
farm/plot and crop management work, but PRs #10–#13 were merged
into **other feature branches**, not into `dev`:

- #10 Monitoring → `feature/crop-management`
- #11 Inventory → `feature/crop-activities-monitoring`
- #12 Harvest → `feature/crop-activities-monitoring`
- #13 Web/Android smoke test → `feature/harvest-management`

The latest `feature/minimal-ui-smoke-test` branch also includes the
17 authorization attribute fixes for CS0182 and the redundant-package cleanup.
This stabilization branch proposes all these changes for `dev`.

Tree audit: **59 added files, 0 removed files** relative to `dev`;
existing SQL schema, EF mappings, and migration history are preserved.
This proves neither build success nor database correctness.

## First: build locally

On macOS/Linux, from the repo root:

```bash
git fetch origin
git switch feature/phase1-stabilization
bash scripts/verify-phase1.sh
```

On Windows, from the repo root:

```powershell
git fetch origin
git switch feature/phase1-stabilization
.\scripts\verify-phase1.ps1
```

The scripts run:
1. .NET 10 SDK check
2. Restore/build the entire `SmartFarm.slnx`
3. All .NET unit/offline schema tests
4. Android debug compilation and unit tests using Gradle

To check only .NET: `bash scripts/verify-phase1.sh --skip-android`
or `.\scripts\verify-phase1.ps1 -SkipAndroid`.

These commands need the appropriate .NET SDK, Java, Android SDK and
Android Studio/Gradle dependencies installed. **They have not been run
by the assistant**.

## Second: smoke test locally

```bash
dotnet run --project src/SmartFarm.Web
```

Then browse `http://localhost:5080/` and
`http://localhost:5080/api/status`.
Expected HTTP 200 and `{"app":"SmartFarm","status":"ok"}`.
This only proves HTTP connectivity, **not database access**.

With the server running on the same development machine, start the Android
emulator in Android Studio (project `mobile/SmartFarm.Android`). Tap
**Check backend**; expected **Backend reachable**. The emulator-only debug
API URL is `http://10.0.2.2:5080/`.

## Third: validate the existing SQL database

Configure the Web `ConnectionStrings:SmartFarm` user secret and connect to
your existing `SmartFarmDB`. Use
[the connection guide](backend-database-connection.md) and
`database/03_VerifySchema.sql` to verify **23 dbo tables**.

In Development, `http://localhost:5080/health/database` should report HTTP
200 when the database is reachable. This alone does not prove CRUD works.
**Do not recreate the database or apply migrations.**

## Critical manual workflow checklist

- [ ] Register/login using two accounts; unauthorized users denied
- [ ] Owner creates a farm/plot; member role permissions enforced
- [ ] Create/activate a crop; second active cycle on the same plot rejected
- [ ] Record crop activities, observations, and view authorized photos
- [ ] Create inventory, receive/consume stock with ledger entries, reject negative stock
- [ ] Record two partial harvests, verify totals, void one without deleting history
- [ ] Complete crop cycle; historical records remain visible
- [ ] Stale rowversion updates rejected; cross-farm requests denied
- [ ] No unintended Hangfire jobs or offline-sync behavior

## Merge gate

Only after the local .NET and Android builds/tests **and critical manual checks
pass**, merge the Phase 1 consolidation PR to `dev` with a **merge commit**
(not squash/rebase). Keep `main` unchanged.

The assistant inspected GitHub source and verified unchanged SQL schema paths,
but **could not run .NET, Android or SQL Server locally** in this environment.
If a check fails, save the exact output and resolve it on the stabilization PR
before proceeding to repository architecture Phase 2.

