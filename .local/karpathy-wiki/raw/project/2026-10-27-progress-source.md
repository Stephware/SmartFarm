# SmartFarm progress source snapshot

> Source: Local SmartFarm repository at 850bfc7dab94675d5a70c13a95a9d00c481c1784
> Collected: 2026-10-27
> Published: Unknown

Read-only capture of repository file contents; not proof of runtime behavior.


## Repository file: README.md

# SmartFarm

SmartFarm is an **in-development farm-management platform** planned for a web application and an Android app. The goal is to help farm owners and field teams manage farms, plots, crop cycles, daily work, supply inventory, harvest records, and weather-related reminders. Android will support selected offline operations with synchronization when connectivity returns.

> **Project status:** Identity, farm/plot management, crop planning, activity/observation monitoring, inventory, and partial harvest recording/voiding now have ASP.NET Core API, MVC, and online-only Android implementations. Harvest totals and audit history use the existing SQL-first Harvest table; crop completion remains a separate operation. Tasks, production-ready media storage, automation, offline synchronization and CI/CD are still future work.

## Start the minimal web and Android preview

Follow the [local smoke test guide](docs/local-smoke-test.md) to launch the
web server, verify `/api/status`, run the Android emulator, and optionally
connect to the existing SQL Server database. See the [reference alignment
review](docs/reference-alignment.md) for verified architectural similarities
and important differences.

## Planned technology stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| Backend / Web | **C# / .NET 10, ASP.NET Core MVC + Web API** | Web interface and API for the Android client |
| Domain / Persistence | **Entity Framework Core 10** | Domain models, database access, eventual migration management |
| Database | **Microsoft SQL Server 2022** | Central application database |
| Authentication | **ASP.NET Core Identity** | User accounts and authorization |
| Background work | **Hangfire** with SQL Server storage | Scheduled tasks and notifications |
| Android | **Kotlin + Jetpack Compose** | Native mobile UI |
| Android local data | **Room** | Offline storage |
| API communication | **Retrofit / OkHttp** | Android-to-API calls |
| Background synchronization | **WorkManager** | Retry and sync queued offline changes |
| External weather | **Weather API integration (provider to be finalized)** | Forecasts and farm advisories |
| Development | **Docker Desktop on macOS; local SQL Server on Windows** | Independent local database environments |
| Collaboration | **Git, GitHub, pull requests, GitHub Actions (planned)** | Source control, review, eventual CI |

## Planned features

- **Accounts and access:** Owner, Manager, and Field Worker roles; users may access multiple farms.
- **Farms and plots:** Register farms, manage farm membership and plots.
- **Crop cycles:** Track planting, active crops, progress, and completion history. At most one active crop cycle per plot.
- **Activities and observations:** Record watering, fertilization, inspections, health observations, and photos.
- **Supply inventory:** Track supplies and stock movements with a transaction history.
- **Harvests:** Record multiple partial harvests; completing a crop cycle remains a separate action.
- **Tasks and alerts:** Planned tasks, scheduled reminders, and weather advisories.
- **Android offline support:** Queue supported field operations locally and sync them when online, including conflict and duplicate-operation handling.

These are design goals; their implementation and end-to-end behavior still require development and testing.

## Repository layout

```text
SmartFarm/
├── src/
│   ├── SmartFarm.Core/           # Domain entities, contracts, interfaces
│   ├── SmartFarm.Infrastructure/ # EF Core, persistence, external integrations
│   ├── SmartFarm.Web/            # MVC web app + API endpoints
│   └── SmartFarm.Worker/         # Hangfire background worker
├── mobile/
│   └── SmartFarm.Android/        # Kotlin / Compose / Room application
├── tests/
│   ├── SmartFarm.SchemaVerification/ # Offline EF model and baseline regression tests
│   ├── SmartFarm.Weather.Tests/ # Provider-independent HTTP client tests
│   ├── SmartFarm.Worker.Tests/  # Background processing configuration tests
│   ├── SmartFarm.Core.Tests/
│   └── SmartFarm.IntegrationTests/
├── database/
│   ├── compose.yaml
│   ├── .env.example
│   ├── 01_CreateDatabase.sql
│   ├── 02_CreateSchema.sql
│   ├── 03_VerifySchema.sql
│   ├── README.md
│   ├── README_macOS.md
│   └── README_Windows.md
├── docs/
└── .github/workflows/
```

Feature folders still contain `.gitkeep` placeholders. The .NET 10 solution includes four backend projects and foundation tests; Android includes its own Gradle project.

## Database setup

Developers run their **own** SQL Server instances and use the same version-controlled SQL scripts:

- [macOS: Docker Desktop + DBeaver](database/README_macOS.md)
- [Windows: SQL Server + SSMS/DBeaver, no Docker required](database/README_Windows.md)
- [Database setup overview](database/README.md)

Run the database creation, schema creation, and verification scripts in order as described in those guides. The initial schema defines **16 SmartFarm business tables and 7 ASP.NET Core Identity tables**.

**Important:** The schema was created manually; `__EFMigrationsHistory` is not initialized by these SQL scripts. Before using EF Core Code First migrations, create a carefully verified baseline matching the actual schema. Never run a normal generated `InitialCreate` migration over tables that already exist.

Keep `.env`, database passwords, actual database files, and backups out of Git.

For Phase 6 instructions, see [harvest management](docs/harvest-management.md). For Phase 5 inventory instructions, see [inventory management](docs/inventory-management.md). For Phase 3, see [crop management](docs/crop-management.md). For Phase 2 development, see [farm and plot management](docs/farm-plot-management.md). For Web connection strings and the development connectivity check, see [Backend database connection](docs/backend-database-connection.md). For mapping verification and the approval required before adopting migration history, see [EF Core schema alignment](docs/efcore-schema-alignment.md).

## Git and pull request workflow

Our **two-person team** uses three branch types:

```text
feature/*  ──pull request──>  dev  ──pull request──>  main
individual work              integration          stable
```

- **`main`** is the stable branch; changes arrive from reviewed PRs, normally after integration testing on `dev`.
- **`dev`** is the shared integration branch; create feature branches from its latest commit.
- **`feature/<short-description>`** is a temporary branch for one logical task. Example: `feature/backend-initialization`, `feature/android-initialization`, `docs/project-readme`.
- Do not work directly on `main` or `dev`. Do not force-push or delete protected branches.
- The repository has branch protection for `main` and `dev`. Follow GitHub's displayed approval requirements when opening and merging a PR; another team member should review.
- Prefer **Squash and merge** if linear history is required. Delete a merged feature branch after merging, not `dev` or `main`.

### Terminal workflow (Git CLI)

**First-time clone:**

```bash
git clone https://github.com/Stephware/SmartFarm.git
cd SmartFarm
git fetch origin
git switch dev
git pull --ff-only origin dev
```

**Start a task:** Create a branch from the updated `dev`:

```bash
git switch dev
git pull --ff-only origin dev
git switch -c docs/project-readme
```

Use a task-specific branch name; do not reuse `docs/project-readme` for unrelated changes.

**Review and push your changes:**

```bash
git status
git diff
git add README.md
git commit -m "docs: describe SmartFarm and development workflow"
git push -u origin docs/project-readme
```

For other work, replace the filename, message, and branch name accordingly. Check `git status` before committing; avoid accidentally staging `.env` or generated files.

**Open a pull request:** In GitHub, select **base: `dev`** and **compare: your feature branch**. Request a teammate review. After approval and required checks, use **Squash and merge**.

**After the PR merges:**

```bash
git switch dev
git pull --ff-only origin dev
git branch -d docs/project-readme
```

Optionally delete the merged **remote feature branch** using GitHub's PR page. If the local delete fails, check whether the squash merge was completed before forcing any deletion; do not delete unmerged work.

**Promote tested work:** Open a separate pull request from **`dev` into `main`**, review the integration changes, and merge using an allowed strategy. Sync local branches afterward.

### GUI workflow (GitHub website + GitHub Desktop)

**Option A — GitHub website (documentation or small edits):**

1. Open [SmartFarm](https://github.com/Stephware/SmartFarm), select `dev` in the branch dropdown.
2. Create a new feature branch from `dev` using the branch selector.
3. Switch to the feature branch, open a file, and use **Edit this file** (pencil icon), or **Add file** to create a new one.
4. Commit changes **to the feature branch**, not `main` or `dev`.
5. Open **Pull requests → New pull request**, choose **base: dev** and **compare: your feature branch**.
6. Describe the change, request your teammate's review, resolve feedback, and merge when allowed.

**Option B — GitHub Desktop (normal development):**

1. Install [GitHub Desktop](https://desktop.github.com/). Sign in and choose **File → Clone repository** to clone `Stephware/SmartFarm`.
2. From **Current Branch**, select `dev`. Click **Fetch origin**, then **Pull origin**.
3. Open **Current Branch → New Branch**, enter a descriptive feature name, and make sure it branches from `dev`.
4. Edit files in VS Code or your preferred editor. Review changes in GitHub Desktop's **Changes** tab.
5. Enter a commit summary, click **Commit to <feature-branch>**, then **Publish branch** or **Push origin**.
6. Select **Create Pull Request**; on GitHub verify **base: dev**, then request review.
7. When merged, return to GitHub Desktop, select `dev`, and pull the latest changes before starting another feature.

**Promotion to `main` (GUI):** In GitHub, open **Pull requests → New pull request** with **base: main** and **compare: dev**, then review and merge after testing.

### How to test branch protection safely

This is a **documentation-only** test; it requires no running backend or database:

1. Open a small documentation PR from a feature branch to `dev`.
2. Confirm the PR displays the expected approval/review requirements.
3. Have your teammate review and approve it using **Files changed → Review changes → Approve**.
4. Confirm GitHub permits merging only when the configured requirements are satisfied; use an allowed merge strategy.
5. Verify the file appears on `dev` after merging, and `main` remains unchanged until a separate `dev` → `main` PR is merged.

If merging is blocked, read GitHub's displayed requirement instead of bypassing protection.

## Build, run, and testing status

The backend contains a **.NET 10 solution** with four application projects and foundation test projects. Install the .NET 10 SDK and run from the repository root:

```bash
dotnet --version
dotnet sln SmartFarm.slnx list
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet test SmartFarm.slnx
dotnet run --project src/SmartFarm.Web/SmartFarm.Web.csproj
```

Open `http://localhost:5080` to try account registration, login and logout using your configured development database. See the [authentication guide](docs/authentication.md) for API, Android and permission testing. Stop with **Ctrl+C**.

To start the background host independently, run:

```bash
dotnet run --project src/SmartFarm.Worker/SmartFarm.Worker.csproj
```

The Worker starts idle by default. Enable processing only after approved jobs database and Hangfire schema provisioning. Stop with **Ctrl+C**. See the [worker setup](docs/background-worker.md) and [weather configuration](docs/weather-integration.md) guides.

For Android build, emulator tests, and local setup, see [Android initialization](docs/android-initialization.md).

**Not yet available:** business features and their tests, a confirmed weather provider adapter, farm background jobs, and CI.

**Checks that work today:**

- Follow the [database guides](database/README.md) to start or connect to SQL Server, then execute `03_VerifySchema.sql` in DBeaver/SSMS to check the database schema.
- Validate Docker Compose configuration on the macOS setup with `cd database && docker compose config --quiet` after creating your local `.env`.
- Test the GitHub branch/PR workflow using the documentation-only steps above.
- Build and test the Android foundation using the [Android guide](docs/android-initialization.md).

**Planned after project initialization:** business features and tests; GitHub Actions CI for pull requests targeting `dev` and `main`. CI should become a required branch check **only after workflows exist and pass**.

## Collaboration rules

1. Keep changes focused; one feature or setup concern per branch/PR.
2. Never commit production credentials, personal data, database volumes, `.env`, or build outputs.
3. Review your own diff and test what is available before requesting review.
4. Coordinate changes to database schemas, API contracts, and offline sync behavior before merging.
5. Report unfinished functionality honestly in PRs and documentation.

## Next milestones

1. Finish and verify the repository's protected-branch and PR workflow.
2. ~~Initialize the **.NET 10** solution (Core, Infrastructure, Web, Worker).~~ Completed; offline schema tests are available and CI remains planned.
3. ~~Connect the .NET backend to SQL Server and reconcile EF Core with the existing SQL schema.~~ Implemented and verified; baseline history adoption requires approval.
4. ~~Initialize the **Kotlin / Compose** Android project.~~ Foundation build and emulator checks are available.
5. Verify Phase 6 harvest management locally. Task automation and Android offline sync are deferred; CI/CD remains deferred.





## Repository file: docs/authentication.md

# Authentication and authorization (Phase 1)

SmartFarm uses the existing seven ASP.NET Core Identity tables. Identity still uses those seven tables. Farm enrollment additionally requires the non-destructive [farm approval upgrade](../database/04_FarmerRegistrationApproval.sql) on existing databases. This phase follows the MVC account-controller pattern from the reference VeterinaryClinicSystem and the separated Web API controller pattern in the reference API projects; authentication itself uses ASP.NET Core Identity rather than copying custom password handling.

## Run the web application

1. Set up the original SQL Server `SmartFarmDB` using [the database guide](../database/README.md). The EF Core migration baseline remains **unapplied**.
2. Configure your local Web connection string as explained in [backend database connection](backend-database-connection.md). Use `ConnectionStrings:SmartFarm` through user secrets or environment variables.
3. From the repository root:

   ```bash
   dotnet tool restore
   dotnet restore SmartFarm.slnx
   dotnet build SmartFarm.slnx
   dotnet test SmartFarm.slnx
   dotnet run --project src/SmartFarm.Web
   ```

4. Open `http://localhost:5080/Account/Register`, register a test user, then log in at `/Account/Login`, open `/Account/Profile`, and log out using the header form. The MVC forms use antiforgery tokens and Identity cookies.
5. Check `http://localhost:5080/health/database` returns 200 with a reachable database (Development only).

Registration asks for full name, email, password and confirmation. Farmers choose **Join an existing farm** and enter the owner’s farm code. The owner approves or rejects the request in their farm dashboard. Pending accounts can sign in and see their request status, but cannot access farm data. Choose **Create/manage my own farm** to use the existing owner signup path. Passwords require 10+ characters, uppercase, lowercase and a number. Identity enforces duplicate email checks and password lockout after five failed attempts for 10 minutes. Sign-up does **not** create a farm or grant management rights.

## Android bearer API

The API uses Identity's **opaque, proprietary bearer tokens**, not JWTs. Browser cookies are not accepted on these bearer-only API endpoints.

| Endpoint | Request | Result |
| --- | --- | --- |
| `POST /api/auth/register` | JSON `fullName, email, password, confirmPassword, joinFarm, farmCode` | 201 when account created |
| `POST /api/auth/login` | JSON `email, password` | Access/refresh tokens |
| `POST /api/auth/refresh` | JSON `refreshToken` | Fresh access/refresh tokens |
| `GET /api/auth/me` | `Authorization: Bearer <access-token>` | Authenticated account |
| `GET /api/farms/{farmId}/access` | Bearer token | Caller’s farm-specific permission (or 404) |

Access tokens expire after 30 minutes and refresh tokens after seven days. Refresh requires a valid Identity security stamp and an enabled account. The Web application does not expose these endpoints as cookie-authenticated routes.

To test API registration/login locally in Development:

```bash
curl -i -H 'Content-Type: application/json' \
  -d '{"fullName":"Test Farmer","email":"tester@example.test","password":"TestPassword123","confirmPassword":"TestPassword123","joinFarm":true,"farmCode":"OWNER_FARM_CODE"}' \
  http://localhost:5081/api/auth/register

curl -i -H 'Content-Type: application/json' \
  -d '{"email":"tester@example.test","password":"TestPassword123"}' \
  http://localhost:5081/api/auth/login
```

Use the returned `accessToken` (do not paste it into shared terminals, logs, tickets or the repo) when testing `/api/auth/me`. Credentials above are only examples; change them in your development environment. **Use HTTPS outside localhost development.**

## Farm permissions

The SQL schema makes `Farm.OwnerUserId` the **Owner**. Active `FarmMember` rows use `Role=1` for Manager and `Role=2` for Field Worker. A removed membership or archived farm grants no access.

| Role | View farm | Manage farm | Manage membership |
| --- | --- | --- | --- |
| Owner | Yes | Yes | Yes |
| Manager | Yes | Yes | No |
| Field Worker | Yes | No | No |
| Nonmember | No | No | No |

The service is in `SmartFarm.Infrastructure/Security/FarmAccessService.cs`; policies are defined in `SmartFarm.Core/Services/Security/FarmAccessRules.cs`. It checks membership **against the database per request**, not by trusting a global role claim. Future feature endpoints must call it before accessing farm-owned records. Global `AspNetRoles` entries are not created during registration.

## Android

Open `mobile/SmartFarm.Android` in Android Studio. The debug build uses `http://10.0.2.2:5081/` for the emulator and permits cleartext **only** to local development hosts through its debug network-security configuration. Production builds require a real HTTPS API address; the placeholder release URL is intentionally empty.

Run `./gradlew assembleDebug testDebugUnitTest` from the Android project folder. Start SmartFarm.Api on port 5081, run the emulator, register/log in using the Compose screens, then visit the account screen and log out.

**Security limitations to address before production:** Android tokens currently live only in memory (restart signs the user out); logout discards local tokens but cannot instantly revoke already issued opaque access tokens. Do not store tokens in Room or plaintext preferences. Email verification, password recovery, two-factor authentication, production HTTPS/configuration, and secure persisted token storage are future work. The public registration endpoint should have rate limiting/abuse controls before public deployment.

## Testing and constraints

- `dotnet test SmartFarm.slnx` includes auth input and role-matrix unit tests, along with the existing offline foundation tests.
- Complete web/database/API registration, login, refresh, disabled-user, unauthorized access and Android emulator smoke tests **require your local SQL Server and Android SDK**.
- Do not call `Database.Migrate()`, `EnsureCreated()`, or alter migration history for this feature.
- No GitHub Actions or CI/CD is introduced by this phase.

## Farm enrollment

After upgrading the database, an owner opens **Farm dashboard → Farmer join requests** to find their code and pending applicants. Farmers can also submit a code after login from **My farms → Join a farm**. Approval grants Field Worker access; existing role management can promote a member later. Rejecting a request does not create membership. Existing accounts and memberships remain unchanged.

`GET /api/farm-join-requests` lists the signed-in account’s requests; `POST` submits `{ "farmCode": "ABCD-1234-5678" }`. Only the owner can call `GET /api/farms/{farmId}/join-requests` for the code/pending applicants, or `POST /api/farms/{farmId}/join-requests/{requestId}/decision` with `{ "approve": true }` or `false`. All require bearer authentication. Signup returns `farmCodeError` alongside `errors` when a code cannot be accepted. Old invitation creation/acceptance URLs return **410 Gone** and grant no access.

Apply `database/04_FarmerRegistrationApproval.sql` to the existing populated SmartFarmDB before running the new hosts and Android client. It backfills stable codes and adds the request table without recreating existing tables. See [database setup](../database/README.md) for SQL Server access. Do not rerun `02_CreateSchema.sql` on populated databases. The application never applies upgrades automatically.


## Repository file: docs/background-worker.md

# Background worker

`SmartFarm.Worker` hosts Hangfire 1.8 with SQL Server storage. It starts idle by default (`Hangfire:Enabled=false`). No jobs are registered.

Before enabling, obtain approval to provision a separate `SmartFarmJobsDB` and install the Hangfire schema from the matching package's `tools/Install.sql`. Use a dedicated account with runtime permissions on that schema. The worker never installs or upgrades schema; do not point it at `SmartFarmDB`.

Configure `ConnectionStrings:Hangfire` in **Worker** user secrets (Development) or `ConnectionStrings__Hangfire` in the environment. Use `Server=127.0.0.1,14333;Database=SmartFarmJobsDB;...` for macOS Docker, or your Windows SQL Server instance with `Database=SmartFarmJobsDB` and Windows authentication. Keep credentials outside the repository and use trusted TLS certificates in deployed environments.

After provisioning approval and schema verification, set `Hangfire__Enabled=true` and run `dotnet run --project src/SmartFarm.Worker`. `Hangfire__WorkerCount` defaults to `1` (allowed: 1–64). Startup validates configuration; SQL availability and runtime permissions are required when enabled. Normal Hangfire startup writes processing metadata to the jobs database.

Hangfire uses host logging, dependency injection, the `default` queue, and graceful shutdown. `IBackgroundJobClient` and `IRecurringJobManager` are available when enabled for future scheduling. This foundation registers no farm jobs.

The worker is headless and exposes no dashboard or HTTP listener. Any future dashboard must require authenticated, authorized access before it is exposed.

See [SQL Server storage](https://docs.hangfire.io/en/latest/configuration/using-sql-server.html) and [dashboard authorization](https://docs.hangfire.io/en/latest/configuration/using-dashboard.html#configuring-authorization).


## Repository file: docs/reference-alignment.md

# Reference repository alignment — source review

Reviewed SmartFarm's feature structure and the eight previously supplied GitHub
repositories. This is a **code-structure comparison**, not an executed build or
proof that every existing feature works end to end.

| User-supplied repository | Verified pattern | Current SmartFarm alignment |
| --- | --- | --- |
| [KurtSarmiento/AspNetCoreMultiApp](https://github.com/KurtSarmiento/AspNetCoreMultiApp) | Separate MVC Web, API, Worker; service/repository and DTO examples | **Conceptual match:** Core/Infrastructure/Web/Worker layers, DTOs, and controllers. **Difference:** SmartFarm hosts MVC and API together in `SmartFarm.Web`, rather than separate web/API hosts; EF Core is used directly inside Infrastructure services, rather than a repository class for each entity. |
| [Stephware/VeterinaryClinicSystem](https://github.com/Stephware/VeterinaryClinicSystem) | Razor MVC controllers, Views, account workflows, ViewModels and service boundaries | **Aligned:** SmartFarm's MVC Accounts/Farms/Crops/Inventory/Harvest pages follow the same broad pattern; simpler UI by design. |
| [Stephware/MyWebApi](https://github.com/Stephware/MyWebApi) | API controllers, DTOs, service contracts | **Aligned:** farm-scoped API controllers call service interfaces and use input/output contracts. Paging is not generalized across all endpoints. |
| [Stephware/RoomDatabaseDiscussion](https://github.com/Stephware/RoomDatabaseDiscussion) | Room entities/DAO/database/repository and StateFlow ViewModels | **Partial:** SmartFarm has Room database and DAO foundation plus repositories and StateFlow ViewModels; live farm operations are currently API-only. Offline persistence/sync is deferred. |
| [Stephware/ComposeUIEssentials](https://github.com/Stephware/ComposeUIEssentials) | Jetpack Compose screens and `NavHost` routing | **Aligned:** SmartFarm uses Compose screens, Navigation Compose, and Material 3. |
| [Stephware/MobdevMidtermExam](https://github.com/Stephware/MobdevMidtermExam) | Kotlin feature screens and Compose forms | **Aligned at UI-pattern level:** functional login and separate feature screens, adapted to SmartFarm. |
| [Stephware/WeatherConsoleApplication](https://github.com/Stephware/WeatherConsoleApplication) | Weather client/service abstraction, typed DTO/options | **Foundation only:** SmartFarm has an isolated weather integration structure; real farm forecast/advisory workflows are not verified as implemented. |
| [Stephware/HangfireConsoleApp](https://github.com/Stephware/HangfireConsoleApp) | Background job/service separation | **Foundation only:** SmartFarm has a Worker/Hangfire setup, but scheduled business jobs, automation and notifications remain deferred. |

## Smoke test design

The web test uses the existing Razor Home view and a tiny anonymous
`GET /api/status` controller. The Android test uses the existing
Retrofit client and a small `StatusRepository` + `StatusViewModel`
(StateFlow), displayed in the existing Compose Welcome screen. It adds
no new UI framework, CSS library, schema object, authentication policy,
background job, or local synchronization behavior.

## Limits and next verification

The repository inspection confirms **structural alignment** with the
references, not identical architecture or guaranteed runtime correctness.
`dotnet build`, `dotnet test`, `./gradlew assembleDebug`, Android device
execution, and SQL Server integration must be checked locally.

Git integration note: PR #14 consolidates the missing later feature work
into `dev`, pending .NET/Android/SQL verification.



## Repository file: scripts/verify-phase1.sh

#!/usr/bin/env bash
# From the repo root: bash scripts/verify-phase1.sh [--skip-android]
set -euo pipefail
cd "$(dirname "$0")/.."
command -v dotnet >/dev/null || { echo "Install .NET 10 SDK." >&2; exit 1; }
echo "[1/4] SDK"; dotnet --version
echo "[2/4] .NET restore and build"
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
echo "[3/4] .NET tests"
dotnet test SmartFarm.slnx --no-build
if [[ "$*" == *"--skip-android"* ]]; then
  echo "[4/4] Android skipped by request."
  exit 0
fi
command -v java >/dev/null || { echo "Install Java/Android Studio Gradle JDK." >&2; exit 1; }
echo "[4/4] Android debug build and unit tests"
(cd mobile/SmartFarm.Android && bash ./gradlew :app:assembleDebug :app:testDebugUnitTest)
echo "PASS: .NET and Android local build checks."



## Repository file: scripts/verify-phase1.ps1

# From repository root: .\scripts\verify-phase1.ps1 [-SkipAndroid]
param([switch]$SkipAndroid)
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Push-Location (Join-Path $PSScriptRoot "..")
try {
    if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
        throw "Install .NET 10 SDK."
    }
    Write-Host "[1/4] SDK"
    & dotnet --version
    if ($LASTEXITCODE -ne 0) { throw "dotnet --version failed" }
    Write-Host "[2/4] Restore + build"
    & dotnet restore SmartFarm.slnx
    if ($LASTEXITCODE -ne 0) { throw "dotnet restore failed" }
    & dotnet build SmartFarm.slnx --no-restore
    if ($LASTEXITCODE -ne 0) { throw "dotnet build failed" }
    Write-Host "[3/4] Tests"
    & dotnet test SmartFarm.slnx --no-build
    if ($LASTEXITCODE -ne 0) { throw "dotnet test failed" }
    if ($SkipAndroid) {
        Write-Host "[4/4] Android skipped by request."
    } else {
        Write-Host "[4/4] Android debug build + unit tests"
        Push-Location "mobile/SmartFarm.Android"
        try {
            & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
            if ($LASTEXITCODE -ne 0) { throw "Android Gradle checks failed" }
        } finally { Pop-Location }
    }
    Write-Host "PASS: local checks completed."
} finally { Pop-Location }



## Repository file: tests/SmartFarm.Api.Tests/BusinessWorkflowSqlTests.cs

using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using SmartFarm.Api.Controllers;
using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Core.Contracts.Crops;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Contracts.Harvests;
using SmartFarm.Core.Contracts.Inventory;
using SmartFarm.Core.Contracts.Monitoring;
using SmartFarm.Core.Contracts.Reports;
using SmartFarm.Core.Contracts.Tasks;
using SmartFarm.Core.Enums;
using Xunit;

namespace SmartFarm.Api.Tests;

// Set SMARTFARM_TEST_CONNECTION only for a separate, disposable SQL Server
// database initialized with SmartFarm's existing SQL scripts.
public sealed class BusinessWorkflowSqlTests
{
    [Fact]
    public async Task OwnerCanManageFarmToHarvestWorkflow_WhenTestDatabaseIsConfigured()
    {
        var connection = Environment.GetEnvironmentVariable("SMARTFARM_TEST_CONNECTION");
        if (string.IsNullOrWhiteSpace(connection))
            return; // Offline test runs do not require a SQL Server instance.

        var database = new SqlConnectionStringBuilder(connection).InitialCatalog;
        Assert.True(database.StartsWith("SmartFarmEnrollmentTest_", StringComparison.Ordinal) || database.StartsWith("SmartFarmEnrollmentBackend_", StringComparison.Ordinal), "SQL workflow tests require a separately initialized disposable enrollment database.");
        using var factory = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.ConfigureAppConfiguration((_, config) =>
                config.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["ConnectionStrings:SmartFarm"] = connection
                }));
            builder.ConfigureServices(services =>
            {
                services.RemoveAll<SmartFarmDbContext>();
                services.RemoveAll<DbContextOptions<SmartFarmDbContext>>();
                services.AddDbContext<SmartFarmDbContext>(options => options.UseSqlServer(connection));
            });
        });
        await using (var scope = factory.Services.CreateAsyncScope())
            Assert.Equal(database, scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>().Database.GetDbConnection().Database);
        using var client = factory.CreateClient();
        var suffix = Guid.NewGuid().ToString("N")[..10];
        var password = "SmartFarmTest2026!";
        var email = $"phase8-{suffix}@example.test";

        await RegisterAsync(client, email, password);
        await LoginAsync(client, email, password);

        var createdFarm = await PostAsync<FarmDetails>(client, "/api/farms", new FarmInput
        {
            Name = "Phase 8 Farm " + suffix,
            LocationText = "Test location",
            TimeZoneId = "Asia/Manila"
        });
        var farm = $"/api/farms/{createdFarm.Id}";
        Assert.Equal("Owner", createdFarm.Role);
        // Weather is available only to members and must work before the worker runs.
        var initialWeather = await GetAsync<SmartFarm.Core.Contracts.Weather.WeatherOverview>(
            client, $"/api/farms/{createdFarm.Id}/weather");
        Assert.False(initialWeather.HasCoordinates);
        Assert.Empty(initialWeather.Days);
        using (var invalidZone = await client.PostAsJsonAsync("/api/farms", new FarmInput
        {
            Name = "Invalid zone farm", LocationText = "Test location",
            TimeZoneId = "Not/ARealTimeZone"
        }))
            Assert.Equal(HttpStatusCode.BadRequest, invalidZone.StatusCode);

        var plot = await PostAsync<PlotDetails>(client, farm + "/plots", new PlotInput
        {
            Name = "Test Plot", AreaSqMeters = 100.00m
        });
        Assert.Equal(createdFarm.Id, plot.FarmId);

        var type = await PostAsync<CropTypeDetails>(client, farm + "/crop-types", new CropTypeInput
        {
            Name = "Phase 8 Crop " + suffix,
            DefaultHarvestUnit = "kg",
            DefaultGrowthDays = 60,
            DefaultWateringIntervalDays = 3
        });
        var planting = DateOnly.FromDateTime(DateTime.UtcNow);
        var planned = await PostAsync<CropCycleDetails>(
            client, farm + $"/plots/{plot.Id}/crop-cycles", new CropCyclePlanInput
            {
                CropTypeId = type.Id,
                PlannedPlantingDate = planting,
                ExpectedHarvestDate = planting.AddDays(60),
                WateringIntervalDays = 3
            });
        var cycle = farm + $"/crop-cycles/{planned.Id}";
        var plantingTask = await PostAsync<FarmTaskDetails>(client, farm + "/tasks",
            new FarmTaskInput(1, planting, "Plant the seedlings", planned.Id, null));
        var active = await PostAsync<CropCycleDetails>(client, cycle + "/transitions",
            new CropCycleTransitionInput
            {
                RowVersion = planned.RowVersion,
                Status = CropCycleStatus.Active,
                PlantedDate = planting
            });
        Assert.Equal(CropCycleStatus.Active, active.Status);
        Assert.Equal((byte)1, (await GetAsync<List<FarmTaskDetails>>(client, farm + "/tasks"))
            .Single(t => t.Id == plantingTask.Id).Status);

        var taskPath = farm + "/tasks";
        var assignedTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(2, planting, "Water the new crop", planned.Id, null));
        Assert.Equal((byte)0, assignedTask.Status);
        Assert.Contains(await GetAsync<List<FarmTaskDetails>>(client, taskPath),
            t => t.Id == assignedTask.Id);

        using (var updateResponse = await client.PutAsJsonAsync(
            taskPath + $"/{assignedTask.Id}", new FarmTaskUpdateInput(
                planting.AddDays(1), "Water tomorrow", null, assignedTask.RowVersion)))
        {
            Assert.Equal(HttpStatusCode.OK, updateResponse.StatusCode);
            var updated = await updateResponse.Content.ReadFromJsonAsync<FarmTaskDetails>();
            Assert.NotNull(updated);
            Assert.Equal(planting.AddDays(1), updated.DueDate);
            Assert.Equal("Water tomorrow", updated.Description);
            using var stale = await client.PutAsJsonAsync(taskPath + $"/{assignedTask.Id}",
                new FarmTaskUpdateInput(planting, "Stale update", null, assignedTask.RowVersion));
            Assert.Equal(HttpStatusCode.Conflict, stale.StatusCode);
            assignedTask = updated;
        }
        using (var closedResponse = await client.PutAsJsonAsync(
            taskPath + $"/{assignedTask.Id}/close",
            new FarmTaskCloseInput(1, assignedTask.RowVersion)))
        {
            Assert.Equal(HttpStatusCode.OK, closedResponse.StatusCode);
            var completed = await closedResponse.Content.ReadFromJsonAsync<FarmTaskDetails>();
            Assert.Equal((byte)1, completed?.Status);
        }

        var linkedTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(2, planting, "Linked watering", planned.Id, null));
        var monitoring = cycle + "/monitoring";
        var activity = await PostAsync<ActivityDetails>(client, monitoring + "/activities",
            new RecordActivityInput { ActivityType = 1, Notes = "Watered", FarmTaskId = linkedTask.Id });
        Assert.Equal(linkedTask.Id, activity.FarmTaskId);
        Assert.Equal((byte)1, (await GetAsync<List<FarmTaskDetails>>(client, taskPath))
            .Single(t => t.Id == linkedTask.Id).Status);
        var autoTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(2, planting, "Automatic watering", planned.Id, null));
        var automatic = await PostAsync<ActivityDetails>(client, monitoring + "/activities",
            new RecordActivityInput { ActivityType = 1, Notes = "Watered again" });
        Assert.Equal(autoTask.Id, automatic.FarmTaskId);
        await PostAsync<ObservationDetails>(client, monitoring + "/observations",
            new RecordObservationInput { HealthStatus = 1, GrowthStage = "Growing" });
        var observations = await GetAsync<MonitoringDetails>(client, monitoring);
        Assert.Equal(2, observations.Activities.Count);
        Assert.Single(observations.Observations);

        // A lost photo-upload response must not create duplicate photos.
        var observationId = observations.Observations.Single().Id;
        var photoId = Guid.NewGuid();
        var photoRoute = monitoring + $"/observations/{observationId}/photos/{photoId}";
        var validPhoto = new byte[] { 0xff, 0xd8, 0xff, 0xe0, 0x01, 0x02, 0x03 };
        async Task<HttpResponseMessage> UploadPhoto(byte[] photoBytes)
        {
            using var form = new MultipartFormDataContent();
            using var image = new ByteArrayContent(photoBytes);
            image.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("image/jpeg");
            form.Add(image, "photo", "field.jpg");
            return await client.PutAsync(photoRoute, form);
        }
        using (var firstUpload = await UploadPhoto(validPhoto))
        {
            Assert.Equal(HttpStatusCode.OK, firstUpload.StatusCode);
            var firstPhoto = await firstUpload.Content.ReadFromJsonAsync<PhotoDetails>();
            Assert.Equal(photoId, firstPhoto?.Id);
        }
        using (var replay = await UploadPhoto(validPhoto))
        {
            Assert.Equal(HttpStatusCode.OK, replay.StatusCode);
            Assert.Equal(photoId, (await replay.Content.ReadFromJsonAsync<PhotoDetails>())?.Id);
        }
        var withPhoto = await GetAsync<MonitoringDetails>(client, monitoring);
        Assert.Single(withPhoto.Observations.Single().Photos);
        using (var differentPhoto = await UploadPhoto(new byte[] { 0xff, 0xd8, 0xff, 0xe0, 0x09 }))
            Assert.Equal(HttpStatusCode.Conflict, differentPhoto.StatusCode);

        var inventory = farm + "/inventory";
        var item = await PostAsync<InventoryItemDetails>(client, inventory, new InventoryItemInput
        {
            Name = "Test Fertilizer", Category = InventoryCategory.Fertilizers,
            Unit = "kg", InitialQuantity = 5m, ReorderLevel = 2m
        });
        var itemPath = inventory + $"/{item.Id}";
        item = await PostAsync<InventoryItemDetails>(client, itemPath + "/transactions",
            new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Receipt,
                QuantityChange = 2m,
                RowVersion = item.RowVersion
            });
        item = await PostAsync<InventoryItemDetails>(client, itemPath + "/transactions",
            new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Consumption,
                QuantityChange = -1m,
                CropActivityId = activity.Id,
                RowVersion = item.RowVersion
            });
        Assert.Equal(6m, item.QuantityOnHand);
        var movements = await GetAsync<List<InventoryTransactionDetails>>(
            client, itemPath + "/transactions");
        Assert.Equal(3, movements.Count); // Opening + receipt + consumption.

        var harvestTask = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(5, planting.AddDays(60), "Harvest the crop", planned.Id, null));
        var harvests = cycle + "/harvests";
        var first = await PostAsync<HarvestEntry>(client, harvests,
            new RecordHarvestInput { Quantity = 2m });
        Assert.Equal((byte)1, (await GetAsync<List<FarmTaskDetails>>(client, taskPath))
            .Single(t => t.Id == harvestTask.Id).Status);
        await PostAsync<HarvestEntry>(client, harvests,
            new RecordHarvestInput { Quantity = 1m });
        var beforeVoid = await GetAsync<HarvestOverview>(client, harvests);
        Assert.Equal(3m, beforeVoid.TotalQuantity);
        Assert.Equal(2, beforeVoid.ValidHarvestCount);

        await PostAsync<HarvestEntry>(client, harvests + $"/{first.Id}/void",
            new VoidHarvestInput { Reason = "Duplicate entry" });
        var afterVoid = await GetAsync<HarvestOverview>(client, harvests);
        Assert.Equal(1m, afterVoid.TotalQuantity);
        Assert.Equal(1, afterVoid.ValidHarvestCount);
        Assert.Equal(1, afterVoid.VoidedHarvestCount);

        // Farm-wide dashboard and reports use exactly the same harvest records.
        var farmReport = await GetAsync<HarvestReport>(client, farm + "/reports/harvests");
        Assert.Equal(1, farmReport.ValidHarvestCount);
        Assert.Equal(1, farmReport.VoidedHarvestCount);
        Assert.Equal(2, farmReport.Entries.Count);
        Assert.Equal(1m, farmReport.UnitTotals.Single().Quantity);
        Assert.Equal("kg", farmReport.UnitTotals.Single().Unit);
        Assert.Single(farmReport.CropPlotTotals);
        var filteredReport = await GetAsync<HarvestReport>(client,
            farm + $"/reports/harvests?plotId={plot.Id}&cropTypeId={type.Id}");
        Assert.Equal(1m, filteredReport.UnitTotals.Single().Quantity);
        using (var invalidReport = await client.GetAsync(
            farm + "/reports/harvests?from=2026-11-10&to=2026-01-01"))
            Assert.Equal(HttpStatusCode.BadRequest, invalidReport.StatusCode);
        var dashboard = await GetAsync<FarmDashboard>(client, farm + "/dashboard");
        Assert.Equal(1, dashboard.ActivePlots);
        Assert.Equal(1, dashboard.ActiveCrops);
        Assert.Equal(1m, dashboard.HarvestLast30Days.Single().Quantity);

        // Lost-response retries must not create extra inventory movements or harvests.
        var inventorySyncId = Guid.NewGuid();
        var inventorySync = new SyncWriteInput
        {
            FarmId = createdFarm.Id, TargetId = item.Id, OperationType = "inventory",
            Movement = new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Receipt,
                QuantityChange = 1m, RowVersion = item.RowVersion
            }
        };
        var syncRoute = $"/api/sync/operations/{inventorySyncId}";
        var firstStockSync = await PostAsync<SyncWriteResult>(client, syncRoute, inventorySync);
        var secondStockSync = await PostAsync<SyncWriteResult>(client, syncRoute, inventorySync);
        Assert.False(firstStockSync.Replayed);
        Assert.True(secondStockSync.Replayed);
        Assert.Equal(firstStockSync.Result.GetRawText(), secondStockSync.Result.GetRawText());
        var stockAfterSync = await GetAsync<InventoryItemDetails>(client, itemPath);
        Assert.Equal(7m, stockAfterSync.QuantityOnHand);
        Assert.Equal(4, (await GetAsync<List<InventoryTransactionDetails>>(
            client, itemPath + "/transactions")).Count);

        using (var changedPayload = await client.PostAsJsonAsync(syncRoute, new SyncWriteInput
        {
            FarmId = createdFarm.Id, TargetId = item.Id, OperationType = "inventory",
            Movement = new InventoryMovementInput
            {
                TransactionType = StockTransactionType.Receipt,
                QuantityChange = 2m, RowVersion = item.RowVersion
            }
        }))
            Assert.Equal(HttpStatusCode.Conflict, changedPayload.StatusCode);

        var harvestSyncId = Guid.NewGuid();
        var harvestSync = new SyncWriteInput
        {
            FarmId = createdFarm.Id, TargetId = planned.Id, OperationType = "harvest",
            Harvest = new RecordHarvestInput { Quantity = 2m, Notes = "Offline harvest" }
        };
        var harvestSyncRoute = $"/api/sync/operations/{harvestSyncId}";
        Assert.False((await PostAsync<SyncWriteResult>(client, harvestSyncRoute, harvestSync)).Replayed);
        Assert.True((await PostAsync<SyncWriteResult>(client, harvestSyncRoute, harvestSync)).Replayed);
        var finalHarvest = await GetAsync<HarvestOverview>(client, harvests);
        Assert.Equal(3m, finalHarvest.TotalQuantity);
        Assert.Equal(2, finalHarvest.ValidHarvestCount);
        var updatedReport = await GetAsync<HarvestReport>(client, farm + "/reports/harvests");
        Assert.Equal(3m, updatedReport.UnitTotals.Single().Quantity);
        Assert.Equal(1, updatedReport.VoidedHarvestCount);

        var pendingOnCompletion = await PostAsync<FarmTaskDetails>(client, taskPath,
            new FarmTaskInput(4, planting, "Inspect before closure", planned.Id, null));
        var completedCycle = await PostAsync<CropCycleDetails>(client, cycle + "/transitions",
            new CropCycleTransitionInput { RowVersion = active.RowVersion,
                Status = CropCycleStatus.Completed });
        Assert.Equal(CropCycleStatus.Completed, completedCycle.Status);
        Assert.Equal((byte)3, (await GetAsync<List<FarmTaskDetails>>(client, taskPath))
            .Single(t => t.Id == pendingOnCompletion.Id).Status);

        // A second account must not access another owner's farm or harvest.
        using var other = factory.CreateClient();
        var otherEmail = $"phase8-other-{suffix}@example.test";
        await RegisterAsync(other, otherEmail, password);
        await LoginAsync(other, otherEmail, password);
        using var restrictedFarm = await other.GetAsync(farm);
        using var restrictedHarvest = await other.GetAsync(harvests);
        using var restrictedTasks = await other.GetAsync(taskPath);
        using var restrictedWeather = await other.GetAsync(farm + "/weather");
        using var restrictedDashboard = await other.GetAsync(farm + "/dashboard");
        using var restrictedReport = await other.GetAsync(farm + "/reports/harvests");
        using var restrictedSync = await other.PostAsJsonAsync(syncRoute, inventorySync);
        Assert.Equal(HttpStatusCode.NotFound, restrictedSync.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedFarm.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedHarvest.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedTasks.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedWeather.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedDashboard.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, restrictedReport.StatusCode);
    }

    private static async Task RegisterAsync(HttpClient client, string email, string password)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/register", new
        {
            FullName = "Phase 8 Tester",
            Email = email,
            Password = password,
            ConfirmPassword = password
        });
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }

    private static async Task LoginAsync(HttpClient client, string email, string password)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/login",
            new { Email = email, Password = password });
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        var token = json.RootElement.GetProperty("accessToken").GetString();
        Assert.False(string.IsNullOrWhiteSpace(token));
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    private static async Task<T> PostAsync<T>(HttpClient client, string path, object body)
    {
        using var response = await client.PostAsJsonAsync(path, body);
        Assert.True(response.IsSuccessStatusCode,
            $"{path}: {(int)response.StatusCode} {await response.Content.ReadAsStringAsync()}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }

    private static async Task<T> GetAsync<T>(HttpClient client, string path)
    {
        using var response = await client.GetAsync(path);
        Assert.True(response.IsSuccessStatusCode,
            $"{path}: {(int)response.StatusCode} {await response.Content.ReadAsStringAsync()}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }
}


## Repository file: tests/SmartFarm.Api.Tests/FarmEnrollmentSqlTests.cs

using System.Data;
using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.SqlClient;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SmartFarm.Core.Contracts.Farms;
using SmartFarm.Core.Interfaces.Farms;
using SmartFarm.Infrastructure.Persistence;
using SmartFarm.Infrastructure.Identity;
using SmartFarm.Infrastructure.Repositories;
using Xunit;

namespace SmartFarm.Api.Tests;

public sealed class FarmEnrollmentSqlTests
{
    private const string Password = "EnrollmentTest2026!";

    [Fact]
    public async Task JoiningRegistrationAndConcurrentOwnerApprovalUseRealSql_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection, synchronizeSignups: true);
        await AssertDisposableDatabase(factory, connection);
        using var owner = factory.CreateClient();
        var suffix = Guid.NewGuid().ToString("N");
        var ownerEmail = $"owner-{suffix}@example.test";
        await Register(owner, ownerEmail);
        await Login(owner, ownerEmail);
        var farm = await Post<FarmDetails>(owner, "/api/farms", new FarmInput { Name = "Approval " + suffix, LocationText = "Test farm" });
        var farmPath = $"/api/farms/{farm.Id}";
        var overview = await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests");
        Assert.Matches("^[A-F0-9]{12}$", overview.JoinCode);

        // Parallel signups must not deadlock Identity's uniqueness reads inside the joining transaction.
        using var parallelOne = factory.CreateClient();
        using var parallelTwo = factory.CreateClient();
        var parallelSignups = await Task.WhenAll(
            parallelOne.PostAsJsonAsync("/api/auth/register", Registration($"parallel-a-{suffix}@example.test", true, overview.JoinCode)),
            parallelTwo.PostAsJsonAsync("/api/auth/register", Registration($"parallel-b-{suffix}@example.test", true, overview.JoinCode)));
        foreach (var signup in parallelSignups) { using (signup) Assert.Equal(HttpStatusCode.Created, signup.StatusCode); }
        var parallelRequests = (await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests")).Requests;
        foreach (var request in parallelRequests)
        {
            using var rejection = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{request.Id}/decision", new { approve = false });
            Assert.Equal(HttpStatusCode.NoContent, rejection.StatusCode);
        }

        using var applicant = factory.CreateClient();
        var invalidEmail = $"invalid-{suffix}@example.test";
        using (var invalid = await applicant.PostAsJsonAsync("/api/auth/register", Registration(invalidEmail, true, "000000000000")))
        {
            Assert.Equal(HttpStatusCode.BadRequest, invalid.StatusCode);
            using var error = JsonDocument.Parse(await invalid.Content.ReadAsStringAsync());
            Assert.False(string.IsNullOrEmpty(error.RootElement.GetProperty("farmCodeError").GetString()));
        }
        await WithDb(factory, async db => Assert.False(await db.Users.AnyAsync(u => u.Email == invalidEmail)));

        var applicantEmail = $"farmer-{suffix}@example.test";
        await Register(applicant, applicantEmail, true, "  " + overview.JoinCode[..4].ToLowerInvariant() + "-" + overview.JoinCode[4..].ToLowerInvariant() + "  ");
        using (var duplicate = await applicant.PostAsJsonAsync("/api/auth/register", Registration(applicantEmail, true, overview.JoinCode))) Assert.Equal(HttpStatusCode.BadRequest, duplicate.StatusCode);
        await WithDb(factory, async db => Assert.Equal(1, await db.Users.CountAsync(u => u.Email == applicantEmail)));
        await Login(applicant, applicantEmail);
        using (var pendingAccess = await applicant.GetAsync(farmPath)) Assert.Equal(HttpStatusCode.NotFound, pendingAccess.StatusCode);
        using (var hiddenOverview = await applicant.GetAsync(farmPath + "/join-requests")) Assert.Equal(HttpStatusCode.NotFound, hiddenOverview.StatusCode);
        var pending = Assert.Single(await Get<List<FarmJoinRequestDetails>>(applicant, "/api/farm-join-requests"));
        Assert.Equal("Pending", pending.Status);
        Assert.Equal(applicantEmail, pending.Email);
        var repeats = await Task.WhenAll(Enumerable.Range(0, 4).Select(_ => Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode })));
        Assert.All(repeats, r => Assert.Equal(pending.Id, r.Id));
        Assert.Single((await Get<FarmEnrollmentOverview>(owner, farmPath + "/join-requests")).Requests);
        using (var wrongFarm = await owner.PostAsJsonAsync($"/api/farms/{Guid.NewGuid()}/join-requests/{pending.Id}/decision", new { approve = true })) Assert.Equal(HttpStatusCode.NotFound, wrongFarm.StatusCode);
        using (var missingDecision = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{pending.Id}/decision", new { })) Assert.Equal(HttpStatusCode.BadRequest, missingDecision.StatusCode);
        var decisions = await Task.WhenAll(Enumerable.Range(0, 4).Select(_ => owner.PostAsJsonAsync(farmPath + $"/join-requests/{pending.Id}/decision", new { approve = true, role = 1, userId = Guid.NewGuid(), farmId = Guid.NewGuid() })));
        foreach (var decision in decisions) { using (decision) Assert.Equal(HttpStatusCode.NoContent, decision.StatusCode); }
        using (var oppositeDecision = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{pending.Id}/decision", new { approve = false })) Assert.Equal(HttpStatusCode.Conflict, oppositeDecision.StatusCode);
        Assert.Equal("FieldWorker", (await Get<FarmDetails>(applicant, farmPath)).Role);
        using (var workerOverview = await applicant.GetAsync(farmPath + "/join-requests")) Assert.Equal(HttpStatusCode.NotFound, workerOverview.StatusCode);
        await WithDb(factory, async db =>
        {
            var membership = Assert.Single(await db.FarmMembers.Where(m => m.FarmId == farm.Id && m.UserId == pending.UserId && m.RemovedAtUtc == null).ToListAsync());
            Assert.Equal((byte)2, membership.Role);
            Assert.Equal("Approved", (await db.FarmJoinRequests.SingleAsync(r => r.Id == pending.Id)).Status);
        });

        await Login(parallelOne, $"parallel-a-{suffix}@example.test");
        var contested = await Post<FarmJoinRequestDetails>(parallelOne, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        var conflictingDecisions = await Task.WhenAll(
            owner.PostAsJsonAsync(farmPath + $"/join-requests/{contested.Id}/decision", new { approve = true }),
            owner.PostAsJsonAsync(farmPath + $"/join-requests/{contested.Id}/decision", new { approve = false }));
        Assert.Equal(1, conflictingDecisions.Count(r => r.StatusCode == HttpStatusCode.NoContent));
        Assert.Equal(1, conflictingDecisions.Count(r => r.StatusCode == HttpStatusCode.Conflict));
        foreach (var decision in conflictingDecisions) decision.Dispose();

        // Removed memberships may reapply, and a rejection preserves the earlier approvals.
        var member = (await Get<List<FarmMemberDetails>>(owner, farmPath + "/members")).Single(m => m.UserId == pending.UserId);
        using (var removal = await owner.DeleteAsync(farmPath + $"/members/{member.Id}")) Assert.Equal(HttpStatusCode.NoContent, removal.StatusCode);
        var reapplied = await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        using (var rejection = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{reapplied.Id}/decision", new { approve = false })) Assert.Equal(HttpStatusCode.NoContent, rejection.StatusCode);
        using (var rejectedAccess = await applicant.GetAsync(farmPath)) Assert.Equal(HttpStatusCode.NotFound, rejectedAccess.StatusCode);
        var afterRejection = await Post<FarmJoinRequestDetails>(applicant, "/api/farm-join-requests", new { farmCode = overview.JoinCode });
        Assert.Equal(3, (await Get<List<FarmJoinRequestDetails>>(applicant, "/api/farm-join-requests")).Count);

        // A disabled applicant and an archived farm never receive access.
        await WithDb(factory, async db => { (await db.Users.SingleAsync(u => u.Id == pending.UserId)).DisabledAtUtc = DateTime.UtcNow; await db.SaveChangesAsync(); });
        using (var disabled = await owner.PostAsJsonAsync(farmPath + $"/join-requests/{afterRejection.Id}/decision", new { approve = true })) Assert.Equal(HttpStatusCode.NotFound, disabled.StatusCode);
        using (var archived = await owner.SendAsync(new HttpRequestMessage(HttpMethod.Delete, farmPath) { Content = JsonContent.Create(new RowVersionInput { RowVersion = farm.RowVersion }) })) Assert.Equal(HttpStatusCode.NoContent, archived.StatusCode);
        var archivedEmail = $"archived-{suffix}@example.test";
        using (var invalidArchived = await applicant.PostAsJsonAsync("/api/auth/register", Registration(archivedEmail, true, overview.JoinCode))) Assert.Equal(HttpStatusCode.BadRequest, invalidArchived.StatusCode);
        await WithDb(factory, async db => Assert.False(await db.Users.AnyAsync(u => u.Email == archivedEmail)));
    }

    [Fact]
    public async Task EnrollmentPersistenceFailureRollsBackTheRealIdentityInsert_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection, failEnrollment: true);
        await AssertDisposableDatabase(factory, connection);
        using var client = factory.CreateClient();
        var email = $"rollback-{Guid.NewGuid():N}@example.test";
        using var response = await client.PostAsJsonAsync("/api/auth/register", Registration(email, true, "ABCD12345678"));
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        await WithDb(factory, async db => Assert.False(await db.Users.AnyAsync(u => u.Email == email)));
    }

    [Fact]
    public async Task ActiveOwnerAndApplicantReadsCannotDeadlockAcrossFarms_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        using var factory = Factory(connection);
        await AssertDisposableDatabase(factory, connection);
        var first = new SmartFarmUser { FullName = "Lock test owner one" };
        var second = new SmartFarmUser { FullName = "Lock test owner two" };
        await WithDb(factory, async db => { db.Users.AddRange(first, second); await db.SaveChangesAsync(); });
        var ready = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var arrivals = 0;
        async Task CrossRead(Guid owner, Guid applicant)
        {
            await using var scope = factory.Services.CreateAsyncScope();
            var db = scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>();
            await using var transaction = await db.Database.BeginTransactionAsync(IsolationLevel.Serializable);
            var repository = new FarmEnrollmentRepository(db);
            Assert.True(await repository.IsActiveUserAsync(owner, default));
            if (Interlocked.Increment(ref arrivals) == 2) ready.TrySetResult();
            await ready.Task.WaitAsync(TimeSpan.FromSeconds(15));
            Assert.True(await repository.IsActiveUserAsync(applicant, default));
            await transaction.CommitAsync();
        }
        await Task.WhenAll(CrossRead(first.Id, second.Id), CrossRead(second.Id, first.Id));
    }

    [Fact]
    public async Task FarmCreationRetriesARandomJoinCodeCollision_WhenDisposableDatabaseConfigured()
    {
        var connection = TestConnection();
        if (connection is null) return;
        var collision = new ForceJoinCodeCollision();
        using var factory = Factory(connection, interceptor: collision);
        await AssertDisposableDatabase(factory, connection);
        using var owner = factory.CreateClient();
        var email = $"collision-{Guid.NewGuid():N}@example.test";
        await Register(owner, email);
        await Login(owner, email);
        await WithDb(factory, async db =>
        {
            var user = await db.Users.SingleAsync(u => u.Email == email);
            db.Farms.Add(new SmartFarm.Core.Entities.Farm { OwnerUserId = user.Id, Name = "Occupied join code", LocationText = "Test", JoinCode = collision.OccupiedCode });
            await db.SaveChangesAsync();
        });
        var created = await Post<FarmDetails>(owner, "/api/farms", new FarmInput { Name = "Collision retry farm", LocationText = "Test farm" });
        Assert.Equal(2, collision.Attempts);
        await WithDb(factory, async db => Assert.NotEqual(collision.OccupiedCode, (await db.Farms.SingleAsync(f => f.Id == created.Id)).JoinCode));
    }

    private static string? TestConnection()
    {
        var connection = Environment.GetEnvironmentVariable("SMARTFARM_ENROLLMENT_TEST_CONNECTION");
        if (string.IsNullOrWhiteSpace(connection)) return null;
        var database = new SqlConnectionStringBuilder(connection).InitialCatalog;
        Assert.True(database.StartsWith("SmartFarmEnrollmentTest_", StringComparison.Ordinal) || database.StartsWith("SmartFarmEnrollmentBackend_", StringComparison.Ordinal), "Enrollment SQL tests require a separately initialized disposable enrollment database.");
        return connection;
    }

    private static WebApplicationFactory<Program> Factory(string connection, bool failEnrollment = false, bool synchronizeSignups = false, IInterceptor? interceptor = null) =>
        new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
        {
            builder.ConfigureAppConfiguration((_, config) => config.AddInMemoryCollection(new Dictionary<string, string?> { ["ConnectionStrings:SmartFarm"] = connection }));
            builder.ConfigureServices(services =>
            {
                services.RemoveAll<SmartFarmDbContext>();
                services.RemoveAll<DbContextOptions<SmartFarmDbContext>>();
                services.AddDbContext<SmartFarmDbContext>(options =>
                {
                    options.UseSqlServer(connection);
                    if (interceptor is not null) options.AddInterceptors(interceptor);
                });
                if (failEnrollment) services.AddScoped<IFarmEnrollmentService, FailedEnrollment>();
                if (synchronizeSignups) services.AddSingleton<IUserValidator<SmartFarmUser>, ParallelSignupBarrier>();
            });
        });

    private static Task AssertDisposableDatabase(WebApplicationFactory<Program> factory, string connection) =>
        WithDb(factory, db =>
        {
            Assert.Equal(new SqlConnectionStringBuilder(connection).InitialCatalog, db.Database.GetDbConnection().Database);
            return Task.CompletedTask;
        });

    private static object Registration(string email, bool joinFarm, string? code) => new { fullName = "Enrollment Tester", email, password = Password, confirmPassword = Password, joinFarm, farmCode = code };
    private static async Task Register(HttpClient client, string email, bool joinFarm = false, string? code = null)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/register", Registration(email, joinFarm, code));
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
    }
    private static async Task Login(HttpClient client, string email)
    {
        using var response = await client.PostAsJsonAsync("/api/auth/login", new { email, password = Password });
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", json.RootElement.GetProperty("accessToken").GetString());
    }
    private static async Task<T> Post<T>(HttpClient client, string path, object input)
    {
        using var response = await client.PostAsJsonAsync(path, input);
        Assert.True(response.IsSuccessStatusCode, $"{path}: {(int)response.StatusCode} {(await response.Content.ReadAsStringAsync()).Split("HEADERS\n")[0]}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }
    private static async Task<T> Get<T>(HttpClient client, string path)
    {
        using var response = await client.GetAsync(path);
        Assert.True(response.IsSuccessStatusCode, $"{path}: {(int)response.StatusCode} {(await response.Content.ReadAsStringAsync()).Split("HEADERS\n")[0]}");
        return (await response.Content.ReadFromJsonAsync<T>())!;
    }
    private static async Task WithDb(WebApplicationFactory<Program> factory, Func<SmartFarmDbContext, Task> work)
    {
        await using var scope = factory.Services.CreateAsyncScope();
        await work(scope.ServiceProvider.GetRequiredService<SmartFarmDbContext>());
    }
    private sealed class ForceJoinCodeCollision : SaveChangesInterceptor
    {
        public string OccupiedCode { get; } = Guid.NewGuid().ToString("N")[..12].ToUpperInvariant();
        public int Attempts { get; private set; }
        public override ValueTask<InterceptionResult<int>> SavingChangesAsync(DbContextEventData eventData, InterceptionResult<int> result, CancellationToken ct = default)
        {
            var farm = eventData.Context!.ChangeTracker.Entries<SmartFarm.Core.Entities.Farm>()
                .SingleOrDefault(e => e.State == EntityState.Added && e.Entity.Name == "Collision retry farm");
            if (farm is not null && ++Attempts == 1) farm.Entity.JoinCode = OccupiedCode;
            return ValueTask.FromResult(result);
        }
    }

    private sealed class ParallelSignupBarrier : IUserValidator<SmartFarmUser>
    {
        private int arrivals;
        private readonly TaskCompletionSource ready = new(TaskCreationOptions.RunContinuationsAsynchronously);
        public async Task<IdentityResult> ValidateAsync(UserManager<SmartFarmUser> manager, SmartFarmUser user)
        {
            if (user.Email?.StartsWith("parallel-", StringComparison.Ordinal) == true)
            {
                if (Interlocked.Increment(ref arrivals) == 2) ready.TrySetResult();
                await ready.Task.WaitAsync(TimeSpan.FromSeconds(15));
            }
            return IdentityResult.Success;
        }
    }

    private sealed class FailedEnrollment : IFarmEnrollmentService
    {
        public Task<FarmJoinRequestDetails> RequestAsync(Guid actor, string code, CancellationToken ct = default) => throw new DbUpdateException("Simulated enrollment persistence failure.");
        public Task<IReadOnlyList<FarmJoinRequestDetails>> MyRequestsAsync(Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task<FarmEnrollmentOverview> OwnerOverviewAsync(Guid farmId, Guid actor, CancellationToken ct = default) => throw new NotImplementedException();
        public Task DecideAsync(Guid farmId, Guid requestId, Guid actor, bool approve, CancellationToken ct = default) => throw new NotImplementedException();
    }
}


## Repository file: tests/SmartFarm.Auth.Tests/MvcIndependentHostTests.cs

using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Xunit;

namespace SmartFarm.Auth.Tests;

public sealed class MvcIndependentHostTests
{
    [Fact]
    public async Task PublicMvcHomepageWorksWithoutAnApiEndpoint()
    {
        // The MVC host must not depend on a running/configured Web API.
        using var app = new WebApplicationFactory<Program>().WithWebHostBuilder(builder =>
            builder.ConfigureAppConfiguration((_, config) =>
                config.AddInMemoryCollection(new Dictionary<string, string?>
                {
                    ["SmartFarmApi:BaseUrl"] = "not-a-valid-api-address"
                })));

        using var client = app.CreateClient(new WebApplicationFactoryClientOptions
        {
            AllowAutoRedirect = false
        });
        using var response = await client.GetAsync("/");
        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }
}


## Repository file: tests/SmartFarm.Api.Tests/SmartFarm.Api.Tests.csproj

<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <IsTestProject>true</IsTestProject>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.14.1" />
    <PackageReference Include="xunit" Version="2.9.3" />
    <PackageReference Include="xunit.runner.visualstudio" Version="3.1.4" PrivateAssets="all" />
    <PackageReference Include="Microsoft.AspNetCore.Mvc.Testing" Version="10.0.13" />
    <ProjectReference Include="../../src/SmartFarm.Api/SmartFarm.Api.csproj" />
  </ItemGroup>
</Project>


## Repository file: tests/SmartFarm.Auth.Tests/SmartFarm.Auth.Tests.csproj

<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <IsTestProject>true</IsTestProject>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.14.1" />
    <PackageReference Include="xunit" Version="2.9.3" />
    <PackageReference Include="xunit.runner.visualstudio" Version="3.1.4" PrivateAssets="all" />
    <PackageReference Include="Microsoft.AspNetCore.Mvc.Testing" Version="10.0.13" />
    <ProjectReference Include="../../src/SmartFarm.Core/SmartFarm.Core.csproj" />
    <ProjectReference Include="../../src/SmartFarm.Web/SmartFarm.Web.csproj" />
  </ItemGroup>
</Project>



## Repository file: tests/SmartFarm.SchemaVerification/SmartFarm.SchemaVerification.csproj

<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <IsTestProject>true</IsTestProject>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.14.1" />
    <PackageReference Include="xunit" Version="2.9.3" />
    <PackageReference Include="xunit.runner.visualstudio" Version="3.1.4" PrivateAssets="all" />
    <ProjectReference Include="../../src/SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj" />
    <None Include="../../database/01_CreateDatabase.sql" Link="Database/01_CreateDatabase.sql" CopyToOutputDirectory="PreserveNewest" />
    <None Include="../../database/02_CreateSchema.sql" Link="Database/02_CreateSchema.sql" CopyToOutputDirectory="PreserveNewest" />
    <None Include="../../database/03_VerifySchema.sql" Link="Database/03_VerifySchema.sql" CopyToOutputDirectory="PreserveNewest" />
    <None Include="../../database/04_FarmerRegistrationApproval.sql" Link="Database/04_FarmerRegistrationApproval.sql" CopyToOutputDirectory="PreserveNewest" />
  </ItemGroup>
</Project>


## Repository file: tests/SmartFarm.Weather.Tests/SmartFarm.Weather.Tests.csproj

<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <IsTestProject>true</IsTestProject>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.14.1" />
    <PackageReference Include="xunit" Version="2.9.3" />
    <PackageReference Include="xunit.runner.visualstudio" Version="3.1.4" PrivateAssets="all" />
    <ProjectReference Include="../../src/SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj" />
  </ItemGroup>
</Project>


## Repository file: tests/SmartFarm.Worker.Tests/SmartFarm.Worker.Tests.csproj

<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <IsTestProject>true</IsTestProject>
    <IsPackable>false</IsPackable>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.14.1" />
    <PackageReference Include="xunit" Version="2.9.3" />
    <PackageReference Include="xunit.runner.visualstudio" Version="3.1.4" PrivateAssets="all" />
    <ProjectReference Include="../../src/SmartFarm.Worker/SmartFarm.Worker.csproj" />
  </ItemGroup>
</Project>


## Repository revision

850bfc7dab94675d5a70c13a95a9d00c481c1784


## Local branch

chore/ignore-local-docs


## Recent committed changes

850bfc7 Keep docs local and remove documentation from tracking
59c5f46 Merge pull request #44 from Stephware/feature/farmer-registration-approval
5b8c267 Add farm code registration and owner approval workflow
ed58469 Document farmer registration and owner approval flow
0ce304a Merge pull request #43 from Stephware/ui/consistent-responsive-layouts
814d2c9 Align responsive Web and Android UI layouts and themes
d4508e0 Merge pull request #42 from Stephware/ui/navigation-practical-inputs
7b16107 Improve farm navigation, native inputs and task access
910656d Merge pull request #41 from Stephware/ui/android-drafts-offline-feedback
7e22c75 Retain Android drafts until local save and show pending records
460f3e6 Merge pull request #40 from Stephware/ui/safe-actions
9374afe Confirm consequential farm actions on Web and Android
f0d7e6e Merge pull request #39 from Stephware/ui/forms-recovery-accessibility
05b723b Preserve web form drafts and improve validation accessibility
0bbe3c2 Merge pull request #38 from Stephware/feature/mvc-shared-services
9513548 Test MVC starts without API
37a8449 Test shared weather registration
e033efd Register shared weather service
3864bb6 Use shared farm weather service
b03365b Use shared farm weather service
24838d8 Share farm weather read logic
6278ea1 Share farm weather read logic
605b7b7 Test MVC shared service registrations
14d8181 Use shared services across MVC and API
968bd95 Use shared services across MVC and API


## Tracked file inventory

.config/dotnet-tools.json
.github/workflows/.gitkeep
.gitignore
Directory.Build.props
README.md
SmartFarm.slnx
database/.env.example
database/01_CreateDatabase.sql
database/02_CreateSchema.sql
database/03_VerifySchema.sql
database/04_FarmerRegistrationApproval.sql
database/README.md
database/README_Windows.md
database/README_macOS.md
database/compose.yaml
database/verification/.gitignore
database/verification/README.md
database/verification/read_catalog.sql
database/verification/test_verify_schema.py
database/verification/verify_schema.py
global.json
mobile/SmartFarm.Android/.gitattributes
mobile/SmartFarm.Android/app/build.gradle.kts
mobile/SmartFarm.Android/app/schemas/com.smartfarm.mobile.data.local.SmartFarmDatabase/1.json
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/.gitkeep
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/DraftFeedbackTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/EnrollmentScreenTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/EnrollmentStateTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/FoundationTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/ImageOptimizerTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/NavigationInputsTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/OfflineCacheTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/OfflinePhotoQueueTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/ResponsiveListsTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/RetryRefreshTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/SafeActionsTest.kt
mobile/SmartFarm.Android/app/src/androidTest/java/com/smartfarm/mobile/WelcomeScreenTest.kt
mobile/SmartFarm.Android/app/src/debug/AndroidManifest.xml
mobile/SmartFarm.Android/app/src/debug/res/xml/debug_network_security.xml
mobile/SmartFarm.Android/app/src/main/AndroidManifest.xml
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/MainActivity.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/SmartFarmApplication.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/OfflineReadCache.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/SmartFarmDatabase.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/dao/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/dao/CachedResponseDao.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/dao/LocalMetadataDao.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/dao/PendingOperationDao.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/dao/PendingPhotoDao.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/entity/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/entity/CachedResponse.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/entity/LocalMetadata.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/entity/PendingOperation.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/local/entity/PendingPhoto.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/ApiClientFactory.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/AuthApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/CropApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/FarmApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/HarvestApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/InventoryApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/MonitoringApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/StatusApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/SyncApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/TaskApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/api/WeatherApi.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/AuthDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/CropDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/FarmDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/HarvestDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/InventoryDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/MonitoringDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/SyncDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/remote/dto/WeatherDtos.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/AuthRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/CropRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/FarmRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/HarvestRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/InventoryRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/MonitoringRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/StatusRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/TaskRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/repository/WeatherRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/ImageOptimizer.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/LocalPhotoFiles.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncRepository.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/data/sync/SyncWorker.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/di/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/di/AppContainer.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/domain/model/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/activities/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/auth/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/auth/AuthScreens.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/auth/AuthViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/crops/CropScreens.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/crops/CropViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/farms/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/farms/FarmScreens.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/farms/FarmViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/harvests/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/harvests/HarvestScreen.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/harvests/HarvestViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inspections/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inventory/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inventory/InventoryScreens.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/inventory/InventoryViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/monitoring/MonitoringScreens.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/monitoring/MonitoringViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/status/StatusViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/syncstatus/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/syncstatus/SyncStatusScreen.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/syncstatus/SyncStatusViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/tasks/TaskScreens.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/features/tasks/TaskViewModel.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/navigation/SmartFarmNavHost.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/ConfirmActionButton.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/DateInput.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/GlidePhoto.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/OfflineDataNotice.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/PendingRecords.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/RecordPage.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/TimeZoneInput.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/components/WelcomeScreen.kt
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/theme/.gitkeep
mobile/SmartFarm.Android/app/src/main/java/com/smartfarm/mobile/ui/theme/Theme.kt
mobile/SmartFarm.Android/app/src/main/res/drawable/.gitkeep
mobile/SmartFarm.Android/app/src/main/res/drawable/ic_launcher.xml
mobile/SmartFarm.Android/app/src/main/res/values/.gitkeep
mobile/SmartFarm.Android/app/src/main/res/values/strings.xml
mobile/SmartFarm.Android/app/src/main/res/values/themes.xml
mobile/SmartFarm.Android/app/src/main/res/xml/backup_rules.xml
mobile/SmartFarm.Android/app/src/main/res/xml/data_extraction_rules.xml
mobile/SmartFarm.Android/app/src/main/res/xml/photo_file_paths.xml
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/.gitkeep
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/ApiClientFactoryTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/EnrollmentApiContractTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/PhotoUploadContractTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/PracticalInputsTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/StandaloneApiIntegrationTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/SyncApiContractTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/TaskApiContractTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/ThemeContrastTest.kt
mobile/SmartFarm.Android/app/src/test/java/com/smartfarm/mobile/WeatherApiContractTest.kt
mobile/SmartFarm.Android/build.gradle.kts
mobile/SmartFarm.Android/gradle.properties
mobile/SmartFarm.Android/gradle/wrapper/.gitkeep
mobile/SmartFarm.Android/gradle/wrapper/gradle-wrapper.jar
mobile/SmartFarm.Android/gradle/wrapper/gradle-wrapper.properties
mobile/SmartFarm.Android/gradlew
mobile/SmartFarm.Android/gradlew.bat
mobile/SmartFarm.Android/settings.gradle.kts
scripts/verify-phase1.ps1
scripts/verify-phase1.sh
src/SmartFarm.Api/Authentication/SmartFarmAuthSchemes.cs
src/SmartFarm.Api/Controllers/AuthController.cs
src/SmartFarm.Api/Controllers/CropCyclesController.cs
src/SmartFarm.Api/Controllers/CropTypesController.cs
src/SmartFarm.Api/Controllers/FarmAccessController.cs
src/SmartFarm.Api/Controllers/FarmApiControllerBase.cs
src/SmartFarm.Api/Controllers/FarmJoinRequestsController.cs
src/SmartFarm.Api/Controllers/FarmMembersController.cs
src/SmartFarm.Api/Controllers/FarmReportsController.cs
src/SmartFarm.Api/Controllers/FarmTasksController.cs
src/SmartFarm.Api/Controllers/FarmWeatherController.cs
src/SmartFarm.Api/Controllers/FarmsController.cs
src/SmartFarm.Api/Controllers/HarvestsApiController.cs
src/SmartFarm.Api/Controllers/InventoryApiController.cs
src/SmartFarm.Api/Controllers/MonitoringApiController.cs
src/SmartFarm.Api/Controllers/NotificationsController.cs
src/SmartFarm.Api/Controllers/StatusController.cs
src/SmartFarm.Api/Controllers/SyncOperationsController.cs
src/SmartFarm.Api/Models/Auth/LoginInput.cs
src/SmartFarm.Api/Models/Auth/RefreshTokenInput.cs
src/SmartFarm.Api/Models/Auth/RegisterInput.cs
src/SmartFarm.Api/Program.cs
src/SmartFarm.Api/Properties/launchSettings.json
src/SmartFarm.Api/SmartFarm.Api.csproj
src/SmartFarm.Api/appsettings.json
src/SmartFarm.Core/Contracts/Crops/CropContracts.cs
src/SmartFarm.Core/Contracts/Farms/FarmContracts.cs
src/SmartFarm.Core/Contracts/Farms/FarmEnrollmentContracts.cs
src/SmartFarm.Core/Contracts/Farms/FarmOperationException.cs
src/SmartFarm.Core/Contracts/Harvests/HarvestContracts.cs
src/SmartFarm.Core/Contracts/Inventory/InventoryContracts.cs
src/SmartFarm.Core/Contracts/Monitoring/MonitoringContracts.cs
src/SmartFarm.Core/Contracts/Reports/FarmReportContracts.cs
src/SmartFarm.Core/Contracts/Requests/.gitkeep
src/SmartFarm.Core/Contracts/Responses/.gitkeep
src/SmartFarm.Core/Contracts/Tasks/TaskContracts.cs
src/SmartFarm.Core/Contracts/Weather/WeatherForecastResult.cs
src/SmartFarm.Core/Contracts/Weather/WeatherOverview.cs
src/SmartFarm.Core/Contracts/Weather/WeatherServiceException.cs
src/SmartFarm.Core/Entities/CropActivity.cs
src/SmartFarm.Core/Entities/CropCycle.cs
src/SmartFarm.Core/Entities/CropObservation.cs
src/SmartFarm.Core/Entities/CropType.cs
src/SmartFarm.Core/Entities/Crops/.gitkeep
src/SmartFarm.Core/Entities/Farm.cs
src/SmartFarm.Core/Entities/FarmJoinRequest.cs
src/SmartFarm.Core/Entities/FarmMember.cs
src/SmartFarm.Core/Entities/FarmTask.cs
src/SmartFarm.Core/Entities/Farms/.gitkeep
src/SmartFarm.Core/Entities/Harvest.cs
src/SmartFarm.Core/Entities/Harvests/.gitkeep
src/SmartFarm.Core/Entities/Identity/.gitkeep
src/SmartFarm.Core/Entities/Inventory/.gitkeep
src/SmartFarm.Core/Entities/InventoryItem.cs
src/SmartFarm.Core/Entities/InventoryTransaction.cs
src/SmartFarm.Core/Entities/Notification.cs
src/SmartFarm.Core/Entities/ObservationPhoto.cs
src/SmartFarm.Core/Entities/Plot.cs
src/SmartFarm.Core/Entities/Scheduling/.gitkeep
src/SmartFarm.Core/Entities/SyncOperation.cs
src/SmartFarm.Core/Entities/Synchronization/.gitkeep
src/SmartFarm.Core/Entities/Weather/.gitkeep
src/SmartFarm.Core/Entities/WeatherAdvisory.cs
src/SmartFarm.Core/Entities/WeatherForecast.cs
src/SmartFarm.Core/Enums/.gitkeep
src/SmartFarm.Core/Enums/CropCycleStatus.cs
src/SmartFarm.Core/Enums/FarmAccessLevel.cs
src/SmartFarm.Core/Enums/InventoryCodes.cs
src/SmartFarm.Core/Interfaces/.gitkeep
src/SmartFarm.Core/Interfaces/Crops/ICropManagementService.cs
src/SmartFarm.Core/Interfaces/Farms/IFarmEnrollmentService.cs
src/SmartFarm.Core/Interfaces/Farms/IFarmManagementService.cs
src/SmartFarm.Core/Interfaces/Harvests/IHarvestService.cs
src/SmartFarm.Core/Interfaces/Inventory/IInventoryService.cs
src/SmartFarm.Core/Interfaces/Monitoring/IMonitoringService.cs
src/SmartFarm.Core/Interfaces/Monitoring/IObservationPhotoStore.cs
src/SmartFarm.Core/Interfaces/Persistence/IUnitOfWork.cs
src/SmartFarm.Core/Interfaces/Reports/IFarmReportService.cs
src/SmartFarm.Core/Interfaces/Repositories/ICropActivityRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/ICropCycleRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/ICropObservationRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/ICropTypeRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IFarmEnrollmentRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IFarmMemberRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IFarmRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IFarmTaskRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IHarvestRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IInventoryItemRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IInventoryTransactionRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IObservationPhotoRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IPlotRepository.cs
src/SmartFarm.Core/Interfaces/Repositories/IUserRepository.cs
src/SmartFarm.Core/Interfaces/Security/IFarmAccessService.cs
src/SmartFarm.Core/Interfaces/Tasks/IFarmTaskService.cs
src/SmartFarm.Core/Interfaces/Weather/IFarmWeatherService.cs
src/SmartFarm.Core/Interfaces/Weather/IWeatherService.cs
src/SmartFarm.Core/Services/.gitkeep
src/SmartFarm.Core/Services/Crops/CropCycleRules.cs
src/SmartFarm.Core/Services/Harvests/HarvestRules.cs
src/SmartFarm.Core/Services/Inventory/StockRules.cs
src/SmartFarm.Core/Services/Monitoring/MonitoringRules.cs
src/SmartFarm.Core/Services/Reports/HarvestReportRules.cs
src/SmartFarm.Core/Services/Security/FarmAccessRules.cs
src/SmartFarm.Core/Services/Tasks/TaskRules.cs
src/SmartFarm.Core/Services/Tasks/WeatherRules.cs
src/SmartFarm.Core/SmartFarm.Core.csproj
src/SmartFarm.Infrastructure/Data/Configurations/.gitkeep
src/SmartFarm.Infrastructure/Data/Migrations/.gitkeep
src/SmartFarm.Infrastructure/Data/Seed/.gitkeep
src/SmartFarm.Infrastructure/DependencyInjection.cs
src/SmartFarm.Infrastructure/Identity/AccountRegistrationService.cs
src/SmartFarm.Infrastructure/Identity/IdentitySetup.cs
src/SmartFarm.Infrastructure/Identity/SmartFarmRole.cs
src/SmartFarm.Infrastructure/Identity/SmartFarmUser.cs
src/SmartFarm.Infrastructure/Integrations/Weather/.gitkeep
src/SmartFarm.Infrastructure/Integrations/Weather/IWeatherProviderAdapter.cs
src/SmartFarm.Infrastructure/Integrations/Weather/OpenMeteoAdapter.cs
src/SmartFarm.Infrastructure/Integrations/Weather/WeatherHttpClient.cs
src/SmartFarm.Infrastructure/Integrations/Weather/WeatherOptions.cs
src/SmartFarm.Infrastructure/Integrations/Weather/WeatherServiceCollectionExtensions.cs
src/SmartFarm.Infrastructure/Persistence/.gitkeep
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetRoleClaimsConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetRolesConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetUserClaimsConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetUserLoginsConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetUserRolesConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetUserTokensConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/AspNetUsersConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/CropActivityConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/CropCycleConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/CropObservationConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/CropTypeConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/FarmConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/FarmJoinRequestConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/FarmMemberConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/FarmTaskConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/HarvestConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/InventoryItemConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/InventoryTransactionConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/NotificationConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/ObservationPhotoConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/PlotConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/SyncOperationConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/WeatherAdvisoryConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/Configurations/WeatherForecastConfiguration.cs
src/SmartFarm.Infrastructure/Persistence/EfUnitOfWork.cs
src/SmartFarm.Infrastructure/Persistence/Migrations/20261017105514_ExistingSchemaBaseline.Designer.cs
src/SmartFarm.Infrastructure/Persistence/Migrations/20261017105514_ExistingSchemaBaseline.cs
src/SmartFarm.Infrastructure/Persistence/Migrations/20261027022450_FarmerRegistrationApproval.Designer.cs
src/SmartFarm.Infrastructure/Persistence/Migrations/20261027022450_FarmerRegistrationApproval.cs
src/SmartFarm.Infrastructure/Persistence/Migrations/SmartFarmDbContextModelSnapshot.cs
src/SmartFarm.Infrastructure/Persistence/SmartFarmDbContext.cs
src/SmartFarm.Infrastructure/Persistence/SmartFarmDbContextFactory.cs
src/SmartFarm.Infrastructure/Repositories/CropActivityRepository.cs
src/SmartFarm.Infrastructure/Repositories/CropCycleRepository.cs
src/SmartFarm.Infrastructure/Repositories/CropObservationRepository.cs
src/SmartFarm.Infrastructure/Repositories/CropTypeRepository.cs
src/SmartFarm.Infrastructure/Repositories/FarmEnrollmentRepository.cs
src/SmartFarm.Infrastructure/Repositories/FarmMemberRepository.cs
src/SmartFarm.Infrastructure/Repositories/FarmRepository.cs
src/SmartFarm.Infrastructure/Repositories/FarmTaskRepository.cs
src/SmartFarm.Infrastructure/Repositories/HarvestRepository.cs
src/SmartFarm.Infrastructure/Repositories/InventoryItemRepository.cs
src/SmartFarm.Infrastructure/Repositories/InventoryTransactionRepository.cs
src/SmartFarm.Infrastructure/Repositories/ObservationPhotoRepository.cs
src/SmartFarm.Infrastructure/Repositories/PlotRepository.cs
src/SmartFarm.Infrastructure/Repositories/UserRepository.cs
src/SmartFarm.Infrastructure/Security/FarmAccessService.cs
src/SmartFarm.Infrastructure/Services/.gitkeep
src/SmartFarm.Infrastructure/Services/Crops/CropManagementService.cs
src/SmartFarm.Infrastructure/Services/Farms/FarmEnrollmentService.cs
src/SmartFarm.Infrastructure/Services/Farms/FarmManagementService.cs
src/SmartFarm.Infrastructure/Services/Harvests/HarvestService.cs
src/SmartFarm.Infrastructure/Services/Inventory/InventoryService.cs
src/SmartFarm.Infrastructure/Services/Monitoring/LocalObservationPhotoStore.cs
src/SmartFarm.Infrastructure/Services/Monitoring/MonitoringService.cs
src/SmartFarm.Infrastructure/Services/Reports/FarmReportService.cs
src/SmartFarm.Infrastructure/Services/Tasks/FarmTaskService.cs
src/SmartFarm.Infrastructure/Services/Weather/FarmWeatherService.cs
src/SmartFarm.Infrastructure/SmartFarm.Infrastructure.csproj
src/SmartFarm.Web/Api/Controllers/.gitkeep
src/SmartFarm.Web/Areas/Admin/Controllers/.gitkeep
src/SmartFarm.Web/Areas/Admin/Views/.gitkeep
src/SmartFarm.Web/Authentication/SmartFarmAuthSchemes.cs
src/SmartFarm.Web/Authorization/.gitkeep
src/SmartFarm.Web/Clients/CropApiService.cs
src/SmartFarm.Web/Clients/FarmAccessApiService.cs
src/SmartFarm.Web/Clients/FarmApiService.cs
src/SmartFarm.Web/Clients/FarmReportApiService.cs
src/SmartFarm.Web/Clients/FarmTaskApiService.cs
src/SmartFarm.Web/Clients/HarvestApiService.cs
src/SmartFarm.Web/Clients/HarvestCsv.cs
src/SmartFarm.Web/Clients/InventoryApiService.cs
src/SmartFarm.Web/Clients/MonitoringApiService.cs
src/SmartFarm.Web/Clients/SmartFarmApiClient.cs
src/SmartFarm.Web/Controllers/.gitkeep
src/SmartFarm.Web/Controllers/AccountController.cs
src/SmartFarm.Web/Controllers/CropsController.cs
src/SmartFarm.Web/Controllers/DashboardController.cs
src/SmartFarm.Web/Controllers/FarmsController.cs
src/SmartFarm.Web/Controllers/HarvestsController.cs
src/SmartFarm.Web/Controllers/HomeController.cs
src/SmartFarm.Web/Controllers/InventoryController.cs
src/SmartFarm.Web/Controllers/MonitoringController.cs
src/SmartFarm.Web/Controllers/NotificationsController.cs
src/SmartFarm.Web/Controllers/ReportsController.cs
src/SmartFarm.Web/Controllers/TasksController.cs
src/SmartFarm.Web/Controllers/WeatherController.cs
src/SmartFarm.Web/Filters/ApiUnavailableFilter.cs
src/SmartFarm.Web/HealthChecks/DatabaseConnectivityHealthCheck.cs
src/SmartFarm.Web/Program.cs
src/SmartFarm.Web/Properties/launchSettings.json
src/SmartFarm.Web/SmartFarm.Web.csproj
src/SmartFarm.Web/ViewModels/.gitkeep
src/SmartFarm.Web/ViewModels/Auth/LoginInput.cs
src/SmartFarm.Web/ViewModels/Auth/RefreshTokenInput.cs
src/SmartFarm.Web/ViewModels/Auth/RegisterInput.cs
src/SmartFarm.Web/ViewModels/Crops/CropWorkspaceViewModel.cs
src/SmartFarm.Web/ViewModels/Farms/FarmWorkspaceViewModel.cs
src/SmartFarm.Web/ViewModels/Harvests/HarvestPage.cs
src/SmartFarm.Web/ViewModels/Inventory/InventoryPages.cs
src/SmartFarm.Web/ViewModels/Monitoring/MonitoringWorkspaceViewModel.cs
src/SmartFarm.Web/ViewModels/Reports/HarvestReportPage.cs
src/SmartFarm.Web/ViewModels/Tasks/TaskPage.cs
src/SmartFarm.Web/Views/Account/AccessDenied.cshtml
src/SmartFarm.Web/Views/Account/Login.cshtml
src/SmartFarm.Web/Views/Account/Profile.cshtml
src/SmartFarm.Web/Views/Account/Register.cshtml
src/SmartFarm.Web/Views/Crops/EditPlan.cshtml
src/SmartFarm.Web/Views/Crops/Index.cshtml
src/SmartFarm.Web/Views/Dashboard/Index.cshtml
src/SmartFarm.Web/Views/Farms/Create.cshtml
src/SmartFarm.Web/Views/Farms/Details.cshtml
src/SmartFarm.Web/Views/Farms/Edit.cshtml
src/SmartFarm.Web/Views/Farms/EditPlot.cshtml
src/SmartFarm.Web/Views/Farms/Index.cshtml
src/SmartFarm.Web/Views/Harvests/Index.cshtml
src/SmartFarm.Web/Views/Home/Index.cshtml
src/SmartFarm.Web/Views/Inventory/Details.cshtml
src/SmartFarm.Web/Views/Inventory/Edit.cshtml
src/SmartFarm.Web/Views/Inventory/Index.cshtml
src/SmartFarm.Web/Views/Monitoring/Index.cshtml
src/SmartFarm.Web/Views/Notifications/Index.cshtml
src/SmartFarm.Web/Views/Reports/Harvests.cshtml
src/SmartFarm.Web/Views/Shared/.gitkeep
src/SmartFarm.Web/Views/Shared/ApiUnavailable.cshtml
src/SmartFarm.Web/Views/Shared/_FarmEnrollment.cshtml
src/SmartFarm.Web/Views/Shared/_FarmNavigation.cshtml
src/SmartFarm.Web/Views/Shared/_Layout.cshtml
src/SmartFarm.Web/Views/Shared/_TimeZones.cshtml
src/SmartFarm.Web/Views/Shared/_ValidationSummary.cshtml
src/SmartFarm.Web/Views/Tasks/Index.cshtml
src/SmartFarm.Web/Views/Weather/Index.cshtml
src/SmartFarm.Web/Views/_ViewImports.cshtml
src/SmartFarm.Web/Views/_ViewStart.cshtml
src/SmartFarm.Web/appsettings.json
src/SmartFarm.Web/wwwroot/css/.gitkeep
src/SmartFarm.Web/wwwroot/css/site.css
src/SmartFarm.Web/wwwroot/images/.gitkeep
src/SmartFarm.Web/wwwroot/js/.gitkeep
src/SmartFarm.Web/wwwroot/js/site.js
src/SmartFarm.Worker/BackgroundProcessingRegistration.cs
src/SmartFarm.Worker/HangfireOptions.cs
src/SmartFarm.Worker/Jobs/.gitkeep
src/SmartFarm.Worker/Jobs/TaskReminderJob.cs
src/SmartFarm.Worker/Jobs/WeatherCheckJob.cs
src/SmartFarm.Worker/Jobs/WorkerHeartbeatJob.cs
src/SmartFarm.Worker/Program.cs
src/SmartFarm.Worker/Scheduling/.gitkeep
src/SmartFarm.Worker/Scheduling/WorkerJobScheduler.cs
src/SmartFarm.Worker/Services/FarmAutomationService.cs
src/SmartFarm.Worker/Services/IFarmAutomationService.cs
src/SmartFarm.Worker/Services/IWorkerHealthService.cs
src/SmartFarm.Worker/Services/WorkerHealthService.cs
src/SmartFarm.Worker/SmartFarm.Worker.csproj
src/SmartFarm.Worker/appsettings.json
tests/SmartFarm.Api.Tests/ApiSmokeTests.cs
tests/SmartFarm.Api.Tests/BusinessEndpointTests.cs
tests/SmartFarm.Api.Tests/BusinessWorkflowSqlTests.cs
tests/SmartFarm.Api.Tests/DatabaseHealthTests.cs
tests/SmartFarm.Api.Tests/EnrollmentRegistrationTests.cs
tests/SmartFarm.Api.Tests/FarmEnrollmentEndpointTests.cs
tests/SmartFarm.Api.Tests/FarmEnrollmentServiceTests.cs
tests/SmartFarm.Api.Tests/FarmEnrollmentSqlTests.cs
tests/SmartFarm.Api.Tests/SmartFarm.Api.Tests.csproj
tests/SmartFarm.Auth.Tests/AuthenticationFoundationTests.cs
tests/SmartFarm.Auth.Tests/AuthorizationSchemeTests.cs
tests/SmartFarm.Auth.Tests/BusinessClientRouteTests.cs
tests/SmartFarm.Auth.Tests/BusinessPageAccessTests.cs
tests/SmartFarm.Auth.Tests/CropPhaseThreeTests.cs
tests/SmartFarm.Auth.Tests/FarmEnrollmentWebTests.cs
tests/SmartFarm.Auth.Tests/FarmPhaseTwoTests.cs
tests/SmartFarm.Auth.Tests/FormRecoveryTests.cs
tests/SmartFarm.Auth.Tests/HarvestCsvTests.cs
tests/SmartFarm.Auth.Tests/HarvestPhaseSixTests.cs
tests/SmartFarm.Auth.Tests/HomePageSmokeTests.cs
tests/SmartFarm.Auth.Tests/InventoryPhaseFiveTests.cs
tests/SmartFarm.Auth.Tests/MonitoringPhaseFourTests.cs
tests/SmartFarm.Auth.Tests/MvcIndependentHostTests.cs
tests/SmartFarm.Auth.Tests/SmartFarm.Auth.Tests.csproj
tests/SmartFarm.Auth.Tests/WebApiClientTests.cs
tests/SmartFarm.Auth.Tests/WebLoginTests.cs
tests/SmartFarm.Core.Tests/.gitkeep
tests/SmartFarm.IntegrationTests/.gitkeep
tests/SmartFarm.SchemaVerification/FarmMemberQueryTests.cs
tests/SmartFarm.SchemaVerification/RepositoryRegistrationTests.cs
tests/SmartFarm.SchemaVerification/SchemaModelTests.cs
tests/SmartFarm.SchemaVerification/SmartFarm.SchemaVerification.csproj
tests/SmartFarm.SchemaVerification/SqlInitializationTests.cs
tests/SmartFarm.Weather.Tests/OpenMeteoAdapterTests.cs
tests/SmartFarm.Weather.Tests/SmartFarm.Weather.Tests.csproj
tests/SmartFarm.Weather.Tests/WeatherServiceTests.cs
tests/SmartFarm.Worker.Tests/BackgroundProcessingTests.cs
tests/SmartFarm.Worker.Tests/FarmAutomationTests.cs
tests/SmartFarm.Worker.Tests/HarvestReportRulesTests.cs
tests/SmartFarm.Worker.Tests/SmartFarm.Worker.Tests.csproj
tests/SmartFarm.Worker.Tests/TaskBusinessRulesTests.cs
tests/SmartFarm.Worker.Tests/WorkerJobTests.cs
tests/web-ui/site.test.cjs


## Available local documentation

docs/local-smoke-test.md
docs/inventory-management.md
docs/crop-monitoring.md
docs/superpowers/specs/2026-10-27-farmer-registration-approval-design.md
docs/superpowers/plans/2026-10-26-ui-improvements.md
docs/superpowers/plans/2026-10-27-farmer-registration-approval.md
docs/weather-integration.md
docs/android-initialization.md
docs/harvest-management.md
docs/farm-plot-management.md
docs/reference-alignment.md
docs/backend-database-connection.md
docs/efcore-schema-alignment.md
docs/crop-management.md
docs/background-worker.md
docs/authentication.md
