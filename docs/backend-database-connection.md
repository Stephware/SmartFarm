# Backend database connection

The Web host registers EF Core 10's SQL Server provider and a scoped `SmartFarmDbContext` from Infrastructure. The context maps the current SQL-first schema (base plus optional finance); see [schema alignment and migration adoption](efcore-schema-alignment.md). Startup and the development health check never create databases or tables, apply migrations, or change migration history.

## Configure your existing database

Use the existing [database setup guides](../database/README.md) for instance details. Supply `ConnectionStrings:SmartFarm` through Web user secrets or the `ConnectionStrings__SmartFarm` environment variable. No connection string belongs in committed appsettings files. The Web host uses ASP.NET Core's default configuration sources; environment variables override user secrets, which load only in Development.

From the repository root, store a local development connection using one of these templates. Replace placeholders with your own values before running the command.

**macOS Docker SQL Server**, using the existing host port `14333`:

```bash
dotnet user-secrets set 'ConnectionStrings:SmartFarm' 'Server=127.0.0.1,14333;Database=SmartFarmDB;User ID=sa;Password=<local-password>;Encrypt=True;TrustServerCertificate=True;Connect Timeout=5;' --project src/SmartFarm.Web
```

**Windows local SQL Server**, using Windows Authentication (PowerShell):

```powershell
dotnet user-secrets set 'ConnectionStrings:SmartFarm' 'Server=localhost;Database=SmartFarmDB;Integrated Security=True;Encrypt=True;TrustServerCertificate=True;Connect Timeout=5;' --project src/SmartFarm.Web
```

For a named SQL Server Express instance, use `Server=.\SQLEXPRESS`; for separately installed LocalDB, use `Server=(localdb)\MSSQLLocalDB`. Adjust the server to match the installed instance. SQL authentication is also supported through a connection string stored outside Git. `TrustServerCertificate=True` is for local development; deployments should validate the server certificate. User secrets are local development storage, not encrypted credential storage, and commands containing passwords may remain in shell history.

Alternatively, set the environment variable for your shell or hosting process:

```bash
# macOS / bash / zsh: replace the placeholder with the complete connection string.
export ConnectionStrings__SmartFarm='<connection-string>'
```

```powershell
# Windows PowerShell: replace the placeholder with the complete connection string.
$env:ConnectionStrings__SmartFarm = '<connection-string>'
```

The Docker `.env` file is used by Compose and is not automatically loaded by the Web host. An enabled Worker uses the application connection plus a separate Hangfire jobs
connection; checked-in processing is enabled. See [Worker setup](background-worker.md).

## Run and verify

```bash
dotnet restore SmartFarm.slnx
dotnet build SmartFarm.slnx --no-restore
dotnet run --project src/SmartFarm.Web
```

The default launch profile sets Development and listens at `http://localhost:5080`. The existing home page remains available even if connection settings are missing or SQL Server is offline.

```bash
curl -i http://localhost:5080/health/database
```

In Development, the endpoint returns `200 Healthy` only when EF Core can open the configured existing database, and `503 Unhealthy` for missing, malformed, or inaccessible settings. It has a five-second health-check timeout and passes cancellation to EF Core. Response bodies exclude provider errors and connection details. Connectivity does not establish schema compatibility or verify application table permissions.

The endpoint and its database health-check registration are absent outside
Development. Production startup additionally requires private photo storage and
HTTPS configuration; the older bare Production command is not a sufficient check.
API is a separate host on port 5081. It can reuse only Web’s DB secret in Development
when no explicit API connection exists; otherwise configure each host separately.
API health also probes Users/Farms/FarmMembers, still not every schema object.

Migration baseline adoption requires a separate approved database operation. Do not run `EnsureCreated()`, `EnsureDeleted()`, or an initial migration against the existing database.

Reviewed 2026-11-11 against ac624be. Follow the full [database rollout guide](../database/README.md)
for cancellation/workflow, optional finance and issue-field compatibility. No live
connection or upgrade was performed by this documentation synchronization.
