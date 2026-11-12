# Background Worker and jobs storage

Reviewed against `dev` at `ac624be` on 2026-11-11. Implementation status below is source-backed; it is not a fresh runtime acceptance result.

`SmartFarm.Worker` is a headless Hangfire host with actual task/reminder, low-stock
and weather automation. It exposes no HTTP listener or dashboard.

## Configuration and startup

Checked-in defaults: `Hangfire:Enabled=true`, `WorkerCount=1`,
`InitializeSchema=false`, `RunJobsOnStartup=false`; OpenMeteo is configured.
An enabled host needs both `ConnectionStrings:SmartFarm` and a distinct
`ConnectionStrings:Hangfire`. Use host-local secrets/environment; never commit values.
Jobs storage refuses system/application catalogs, attached files and same catalog
as the app. Recommended jobs catalog is `SmartFarmJobsDB`.

For intentionally idle development, explicitly set `Hangfire__Enabled=false`.
Do not start the default Worker as a harmless smoke check: enabled startup writes
jobs metadata and queues work.

Review/approve separate jobs provisioning first. `database/05_CreateJobsDatabase.sql`
creates only the jobs database. Provision matching Hangfire schema separately, or
explicitly opt into `Hangfire__InitializeSchema=true` for reviewed initial setup,
then return it to false. Heavy migrations remain disabled. Use dedicated runtime
permissions and trusted deployment TLS.

```sh
# Only after storage/configuration is ready:
dotnet run --project src/SmartFarm.Worker
```

## Schedule and behavior

`WorkerJobScheduler` registers UTC cron: heartbeat hourly, tasks/reminders hourly,
weather every six hours. Startup always queues heartbeat; RunJobsOnStartup also
queues task/weather work for explicit local verification. Farm dates/timezones
control schedules and Monday-start weekly low-stock digests. Invalid zones are
logged/skipped. Deterministic keys prevent duplicate generated tasks/notices.

Fieldwork/lifecycle services fulfill/cancel tasks and dismiss reminders. Low-stock
digest targets active Owner/Managers; task reminders recheck recipients/access.
Weather processing persists forecast/advisory data for UI reads; no email/SMS/push
transport exists. See [automation details](../.local/karpathy-wiki/wiki/project/automation.md).

## Verify

Worker tests check rules/registration, not a running process. For an authorized
live environment inspect logs and `database/06_VerifyHangfireJobs.sql` (read-only).
Verify actual successes, deduplication, missing coordinates and provider failures.
Backup/read-only script passes do not prove job runtime or recovery. See
[weather](weather-integration.md) and [recovery](../database/RECOVERY.md).
