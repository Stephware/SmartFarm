# SmartFarm

> **New product direction (planned, not yet implemented):** simplify AniGo to four MVC farm sections and three Android destinations while preserving the original course MVP, backend safeguards, Hangfire and offline Room. Start with [PRODUCT.md](PRODUCT.md), [the staged implementation plan](docs/superpowers/plans/2026-11-13-mvp-simplification.md) and [scope decision](.local/karpathy-wiki/wiki/project/mvp-simplification.md). The implemented feature inventory below describes the **existing** system until refactoring PRs land; do not delete data or auth tokens as part of documentation work.

SmartFarm is an in-development farm-management platform, displayed as **AniGo**
on Web and Android. Full audit at `ac624be`; publication source checks on 2026-11-12 at `dev` /
`3c5a8b0` are summarized in [current source status](.local/karpathy-wiki/wiki/project/current-source-status.md). This is
implementation documentation, not a fresh deployment/runtime acceptance result.

## Current implementation

Accounts and owner-approved enrollment; farm/plot/member management and location
selection; crop plans/lifecycle/agenda/history; activities, structured inspections
and private photos; tasks/reminders; audited stock and partial harvests; saved
OpenMeteo weather/advisories; dashboard, harvest/cycle comparisons, manually entered
PHP cash and financial insights/CSV are implemented. Android includes farm home,
manager tasks, read-only reports/finance insights and selected Room/WorkManager
cached reads/queued field writes, including atomic activity-plus-stock use.

Current UI is Harvest Gold + Pine with prefixed self-hosted Tailwind utilities.
Web also provides a separately protected Super Admin/MFA area, user support and
announcements; see current source status for security and schema boundaries. Signup uses name/email/password; joining is a
separate signed-in request, and pending users gain no farm access. Harvests do not
complete crops or imply sales; quantity units and planting cycles stay separate.
Finance is recorded cash, not full accounting profit.

Limits include selected offline coverage, memory-only Android sessions, missing
sync-driven refresh of some open confirmed data, mobile report/export parity,
target-specific existing-database upgrades and unverified production/recovery
acceptance. CI exists; automated CD does not. See [current status and history](.local/karpathy-wiki/wiki/project/progress.md)
and [known limits](.local/karpathy-wiki/wiki/project/known-issues.md).

## Architecture and repository navigation

| Area | Responsibility |
| --- | --- |
| `src/SmartFarm.Core` | Plain entities, contracts, interfaces, enums and pure rules |
| `src/SmartFarm.Infrastructure` | Shared services, repositories/unit of work, EF/Identity, provider/private files |
| `src/SmartFarm.Web` | Independent Razor MVC cookie host; shared services, charts and CSV |
| `src/SmartFarm.Api` | Independent opaque-bearer JSON host and transactional sync ledger |
| `src/SmartFarm.Worker` | Headless Hangfire task/stock/weather automation with separate jobs storage |
| `mobile/SmartFarm.Android` | Compose/StateFlow/Retrofit with Room/WorkManager |
| `tests` | Five active backend test projects plus Node UI tests; Android tests live in app source sets |
| `database` | SQL-first creation/upgrades, read-only comparison and recovery runbooks |
| `.github/workflows` | Backend/Android build/tests and recovery-script/schema checks |
| `docs`, `.local/karpathy-wiki` | Development guides, engineering workflow, source-backed knowledge/evidence |

The .NET solution targets net10.0 and includes Core/Infrastructure/Web/Api/Worker
and active tests. Android is a separate Gradle project. MVC does not call API for
business pages; both share services/database. OpenMeteo is the current provider.
Exact dependency pins belong in build manifests, not claims about latest releases.

## Build and local run

Follow [local smoke checks](docs/local-smoke-test.md) and configure SQL with
[database connection](docs/backend-database-connection.md) first for business flows.

```sh
dotnet tool restore
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet test SmartFarm.slnx --no-build
# Separate terminals:
dotnet run --project src/SmartFarm.Web
dotnet run --project src/SmartFarm.Api
```

Default Development URLs: Web `http://localhost:5080`, API
`http://localhost:5081` (`/api/status` is API liveness); database health is
Development-only. Android emulator uses `http://10.0.2.2:5081/`. See
[Android setup](docs/android-initialization.md) for builds/device tests/release URL.

**Worker defaults enabled.** Configure application and separate jobs databases
before starting it; explicitly set `Hangfire__Enabled=false` for an idle host.
See [Worker](docs/background-worker.md) and [weather](docs/weather-integration.md).

`bash scripts/verify-phase-a.sh` combines Release backend/Python/Android build and
unit checks. SQL/device checks require explicit opt-in and disposable environments.
CI runs Node/public Chromium/Tailwind checks and compiles device tests; it does
not run an Android emulator, live SQL or deployment. See
[verification guidance](.local/karpathy-wiki/wiki/project/development.md).

## Database and private media

Use [central SQL setup](database/README.md), with [macOS](database/README_macOS.md)
or [Windows](database/README_Windows.md) instructions. Base CREATE defines 25 tables including admin audit;
finance, support and announcements extend the full EF model to 29 tables. Existing
databases need reviewed workflow/issue/finance/admin/support/announcement and other
missing changes, based on actual metadata/history. The current 03 verifier covers
base/finance counts only; see the central runbook for extended-schema acceptance.
Never rerun fresh-schema DDL on business data. Startup does not migrate or adopt EF
history; [schema alignment](docs/efcore-schema-alignment.md) explains the boundary.

Development Web/API share Web/App_Data observation storage. Production requires
an explicit absolute private non-root path outside wwwroot and the same durable
mounted volume accessible by both hosts; matching path strings do not create shared
storage. Verify/migrate existing files before retiring old storage. SQL stores photo
keys, not image bytes. Protect matching Data Protection key rings, database secrets
and HTTPS configuration. See [backup/recovery acceptance](database/RECOVERY.md).

## Development context

Agents start at [AGENTS.md](AGENTS.md), then the [knowledge index](.local/karpathy-wiki/wiki/index.md).
Use the complementary [engineering workflow](docs/superpowers/README.md):
Karpathy → applicable Superpowers → Ponytail → Impeccable for UI → tests → knowledge.
Source is authoritative; historical plans and tests stay dated. Documentation is
Git-visible; private runtime artifacts/secrets/generated files remain excluded.

Feature guides: [authentication](docs/authentication.md), [farm/plots](docs/farm-plot-management.md),
[crops](docs/crop-management.md), [monitoring/photos](docs/crop-monitoring.md),
[inventory](docs/inventory-management.md), [harvests](docs/harvest-management.md),
[reports/finance](.local/karpathy-wiki/wiki/project/finance-reports.md).

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
- Branch protection is part of the intended `main`/`dev` workflow; its current remote configuration was not audited here. Follow GitHub's displayed approval requirements when opening and merging a PR; another team member should review.
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

## Collaboration rules

Keep one focused task per branch/PR; reuse existing patterns and minimal dependencies
without sacrificing security, validation, errors, accessibility or essential tests.
Inspect diffs and explicit paths before staging. Keep credentials, private farmer
data, keys/photos, database files/volumes/backups and build outputs out of Git.
Coordinate schema/API/offline contract changes and preserve audit/idempotency rules.
Report unfinished behavior and unrun checks honestly. Git commands above describe
the authorized integration workflow; they do not authorize an agent to commit,
push, merge or open PRs during documentation-only maintenance.
