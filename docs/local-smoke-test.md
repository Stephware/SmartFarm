# SmartFarm local Web/API/Android smoke checks

Reviewed against `dev` at `ac624be` on 2026-11-11. Implementation status below is source-backed; it is not a fresh runtime acceptance result.

## Build and start the independent hosts

Preserve the current checkout and unrelated work. Use the latest intended `dev`
base for a new feature branch; old PR14/phase1 branch instructions are historical.
Install the pinned .NET SDK, then run from repository root:

```sh
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet test SmartFarm.slnx --no-build
# Separate terminals:
dotnet run --project src/SmartFarm.Web
dotnet run --project src/SmartFarm.Api
```

Web home is `http://localhost:5080/`; API liveness is
`http://localhost:5081/api/status`, returning `{"app":"SmartFarm","status":"ok"}`.
Liveness/home can work without a database; that does not establish CRUD readiness.
Development-only `/health/database` exists on both hosts: Web checks connectivity,
API also probes main Identity/farm tables. Neither establishes full schema equality.
Follow [DB configuration](backend-database-connection.md) before account/field tests.

## Android

Open `mobile/SmartFarm.Android` in Android Studio. Debug emulator API is
`http://10.0.2.2:5081/` (API host, not Web). Run **Check backend** on the AniGo
welcome screen, then register/sign in and open a farm workspace. Current signup
uses name/email/password; farm requests happen after sign-in.

```sh
# From mobile/SmartFarm.Android:
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
# Requires authorized device/emulator:
bash ./gradlew :app:connectedDebugAndroidTest
```

A physical phone needs a reachable configured HTTPS API; `10.0.2.2` is an emulator
alias. Release API URL is intentionally unset. See [Android setup](android-initialization.md).

## Functional acceptance

Use disposable test data/schema and the [Phase A command](../scripts/verify-phase-a.sh).
Owner: create farm/plot/crop, activate, record field activity/inspection, stock and
partial harvest. Check worker/member permissions and owner-only enrollment decisions.
Confirm partial harvest does not finish the crop, void/reversal history remains,
and stock conflicts require review. Test offline supported writes, reconnect/replay,
photos and displayed freshness separately. Reports/finance are confirmed-server data.

`bash scripts/verify-phase-a.sh --sql --device` opts into configured disposable SQL
and authorized device checks. The stricter `verify-phase6.sh` also needs running
Web/API URLs and both disposable SQL settings. Read [development checks](../.local/karpathy-wiki/wiki/project/development.md).
Do not start Worker casually: its checked-in default is enabled and may process jobs.
Browser interactions, real Worker/provider execution and recovery require separate
evidence; a status response or green no-SQL suite does not prove them.
