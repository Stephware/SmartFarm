# Tasks, weather, and background automation

> Sources: SmartFarm Worker, weather integration, and business services
> Raw: [Automation sources](../../raw/project/2026-10-27-automation-source.md); [Feature sources](../../raw/project/2026-10-27-features-source.md); [PR48 fix and merge](../../raw/project/2026-10-28-pr48-fix-and-merge.md); [backend source review](../../raw/project/2026-11-11-backend-current.md); [android source review](../../raw/project/2026-11-11-android-current.md); [Current documentation audit](../../raw/project/2026-11-11-documentation-audit.md); [Publication source check](../../raw/project/2026-11-12-documentation-publication.md)
> Updated: 2026-11-12

**Source scope:** Earlier detailed audit/acceptance is dated below. For source
changes through `dev` at `3c5a8b0`, see [current source status](current-source-status.md),
which supersedes conflicting baseline UI, admin, CI and schema statements.

## Implemented behavior

Owner/Manager creates/updates tasks with eligible assignees; workers can close
their assigned work. Android now includes manager task forms and farm-local Today,
overdue, upcoming and completed groups plus lightweight presets. Guided field
notes do not automatically create observations or tasks. Recording related work
can fulfill tasks; crop transitions and generated-task changes dismiss reminders.

`WorkerJobScheduler` registers hourly heartbeat, hourly tasks/reminders and weather
every six hours, all UTC cron. Farm business dates use each farm's timezone.
Planting/watering/harvest schedules have deterministic deduplication keys.
Low-stock checks create deduplicated in-app reminders. Eligible reminder recipients
have current access, with owner fallback for an invalid assignee. No email/SMS/push
transport is implemented.

OpenMeteo is an implemented no-key provider. The typed client validates options,
constrains HTTPS/provider origin, disables redirects and request logging, times out
and sanitizes provider failures. Worker persists forecasts and advisories, resolves
superseded warnings, and creates deduplicated notifications. Web/API/Android read
stored forecasts; opening weather does not fetch the provider. Missing coordinates,
absent snapshots, stale cache and current forecast are distinct states.

## Scheduling date limits

Shared `TaskRules.ScheduledTasks` uses integer day numbers to avoid overflowing
at maximum planting/watering dates. Worker and the Web agenda share this rule.
The dated [PR48 checks](pr48-review.md) preserve regression evidence; this review
did not rerun those application tests.

## Enablement is separate from implementation

Checked-in Worker `appsettings.json` now sets `Hangfire:Enabled=true`,
`WorkerCount=1`, `InitializeSchema=false`, `RunJobsOnStartup=false`, and OpenMeteo.
With no overrides, startup requires both application and separate jobs connection
strings. To run intentionally idle, explicitly set `Hangfire__Enabled=false`.
Do not start the default Worker assuming it is inert.

Jobs storage rejects system/application database names. Schema preparation is
possible only via explicit `InitializeSchema=true`; heavy migrations are disabled.
Prefer separately reviewed provisioning. An enabled startup registers recurring
jobs and queues a heartbeat immediately; `RunJobsOnStartup` also queues farm jobs.
The Worker is headless with no HTTP listener/dashboard. Verify execution through
logs and the read-only jobs SQL query, not a nonexistent dashboard.

Farms need coordinates for weather and valid timezones for automation. Invalid
zones are logged/skipped. No live provider, jobs database or deployment was
exercised by this documentation update.

## Source navigation

[Scheduler](../../../../src/SmartFarm.Worker/Scheduling/WorkerJobScheduler.cs),
[automation](../../../../src/SmartFarm.Worker/Services/FarmAutomationService.cs),
[configuration](../../../../src/SmartFarm.Worker/appsettings.json),
[storage safeguards](../../../../src/SmartFarm.Worker/BackgroundProcessingRegistration.cs),
[provider](../../../../src/SmartFarm.Infrastructure/Integrations/Weather/OpenMeteoAdapter.cs),
[weather reads](../../../../src/SmartFarm.Infrastructure/Services/Weather/FarmWeatherService.cs).

## See Also

[Features](features.md), [Architecture](architecture.md), [Known issues](known-issues.md),
[Development](development.md), [Android](android.md).
