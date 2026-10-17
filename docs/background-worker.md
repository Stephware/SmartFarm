# Background worker

`SmartFarm.Worker` hosts Hangfire 1.8 with SQL Server storage. It starts idle by default (`Hangfire:Enabled=false`). No jobs are registered.

Before enabling, obtain approval to provision a separate `SmartFarmJobsDB` and install the Hangfire schema from the matching package's `tools/Install.sql`. Use a dedicated account with runtime permissions on that schema. The worker never installs or upgrades schema; do not point it at `SmartFarmDB`.

Configure `ConnectionStrings:Hangfire` in **Worker** user secrets (Development) or `ConnectionStrings__Hangfire` in the environment. Use `Server=127.0.0.1,14333;Database=SmartFarmJobsDB;...` for macOS Docker, or your Windows SQL Server instance with `Database=SmartFarmJobsDB` and Windows authentication. Keep credentials outside the repository and use trusted TLS certificates in deployed environments.

After provisioning approval and schema verification, set `Hangfire__Enabled=true` and run `dotnet run --project src/SmartFarm.Worker`. `Hangfire__WorkerCount` defaults to `1` (allowed: 1–64). Startup validates configuration; SQL availability and runtime permissions are required when enabled. Normal Hangfire startup writes processing metadata to the jobs database.

Hangfire uses host logging, dependency injection, the `default` queue, and graceful shutdown. `IBackgroundJobClient` and `IRecurringJobManager` are available when enabled for future scheduling. This foundation registers no farm jobs.

The worker is headless and exposes no dashboard or HTTP listener. Any future dashboard must require authenticated, authorized access before it is exposed.

See [SQL Server storage](https://docs.hangfire.io/en/latest/configuration/using-sql-server.html) and [dashboard authorization](https://docs.hangfire.io/en/latest/configuration/using-dashboard.html#configuring-authorization).
