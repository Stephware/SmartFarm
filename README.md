# SmartFarm

SmartFarm is an **in-development farm-management platform** planned for a web application and an Android app. The goal is to help farm owners and field teams manage farms, plots, crop cycles, daily work, supply inventory, harvest records, and weather-related reminders. Android will support selected offline operations with synchronization when connectivity returns.

> **Project status:** The .NET 10 solution and four backend project shells are initialized, alongside SQL Server setup scripts and database documentation. The MVC home page is a smoke test only. Domain features, database integration, API endpoints, Hangfire jobs, Android app, and CI checks are **not implemented yet**.

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

Many folders still contain only `.gitkeep` placeholders. The .NET 10 solution and four backend project shells exist; Android Gradle files and automated test projects have not been created yet.

## Database setup

Developers run their **own** SQL Server instances and use the same version-controlled SQL scripts:

- [macOS: Docker Desktop + DBeaver](database/README_macOS.md)
- [Windows: SQL Server + SSMS/DBeaver, no Docker required](database/README_Windows.md)
- [Database setup overview](database/README.md)

Run the database creation, schema creation, and verification scripts in order as described in those guides. The initial schema defines **16 SmartFarm business tables and 7 ASP.NET Core Identity tables**.

**Important:** The schema was created manually; `__EFMigrationsHistory` is not initialized by these SQL scripts. Before using EF Core Code First migrations, create a carefully verified baseline matching the actual schema. Never run a normal generated `InitialCreate` migration over tables that already exist.

Keep `.env`, database passwords, actual database files, and backups out of Git.

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

The backend contains an initial **.NET 10 solution** with four projects. Install the .NET 10 SDK and run from the repository root:

```bash
dotnet --version
dotnet sln SmartFarm.slnx list
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx
dotnet run --project src/SmartFarm.Web/SmartFarm.Web.csproj
```

Open the URL shown by the web app (or `http://localhost:5080` using its HTTP launch profile) to check the placeholder SmartFarm home page. Stop with **Ctrl+C**.

To start the background host independently, run:

```bash
dotnet run --project src/SmartFarm.Worker/SmartFarm.Worker.csproj
```

The Worker is currently idle; Hangfire jobs and SQL Server connections are **not configured**. Stop with **Ctrl+C**.

**Not yet available:** application tests (`dotnet test` has no test projects), Android Gradle builds (`./gradlew` does not exist), working API features, and CI. These come in later milestones.

**Checks that work today:**

- Follow the [database guides](database/README.md) to start or connect to SQL Server, then execute `03_VerifySchema.sql` in DBeaver/SSMS to check the database schema.
- Validate Docker Compose configuration on the macOS setup with `cd database && docker compose config --quiet` after creating your local `.env`.
- Test the GitHub branch/PR workflow using the documentation-only steps above.

**Planned after project initialization:** .NET restore/build/tests; Android Gradle build/tests; GitHub Actions CI for pull requests targeting `dev` and `main`. CI should become a required branch check **only after workflows exist and pass**.

## Collaboration rules

1. Keep changes focused; one feature or setup concern per branch/PR.
2. Never commit production credentials, personal data, database volumes, `.env`, or build outputs.
3. Review your own diff and test what is available before requesting review.
4. Coordinate changes to database schemas, API contracts, and offline sync behavior before merging.
5. Report unfinished functionality honestly in PRs and documentation.

## Next milestones

1. Finish and verify the repository's protected-branch and PR workflow.
2. ~~Initialize the **.NET 10** solution (Core, Infrastructure, Web, Worker).~~ Completed as scaffolding; add tests and CI next.
3. Connect the .NET backend to SQL Server and reconcile EF Core with the existing SQL schema.
4. Initialize the **Kotlin / Compose** Android project.
5. Add **GitHub Actions** build/test workflows, then begin domain features and integrations.
