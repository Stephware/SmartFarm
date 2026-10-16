# SmartFarm SQL Server setup

Each developer should follow these steps on their own machine to create a local
`SmartFarmDB`. The original SQL scripts create **16 SmartFarm business tables and
7 ASP.NET Core Identity tables**, together with their original keys, indexes,
constraints and relationships. Docker starts SQL Server; it does not run the SQL
scripts automatically.

## 1. Install Docker Desktop

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) for your
operating system and processor, then open it. On Windows, use Linux containers.
Verify the CLI and Compose are installed:

```bash
docker --version
docker compose version
```

## 2. Check Mac architecture

On each Mac, run:

```bash
uname -m
```

- `x86_64`: Intel Mac.
- `arm64`: Apple Silicon.

## 3. Understand the Apple Silicon limitation

SQL Server 2022 Linux container images require x86-64. Microsoft supports these
images on Linux hosts with Intel or AMD x86-64 processors; emulation through
Rosetta 2 or QEMU is not tested or supported. See
[Microsoft's container support statement](https://learn.microsoft.com/en-us/sql/linux/containers/deploy?view=sql-server-ver16).

The supplied Compose file requests `linux/amd64`. This allows Docker Desktop on
Apple Silicon to attempt emulation, but SQL Server may fail to start or crash.
For a supported setup, use an x86-64 Linux Docker host. If local emulation fails,
connect DBeaver to a separately configured x86-64 SQL Server host using that
host's address and credentials instead of the local connection below.

## 4. Create your local environment file

From the repository root:

```bash
cd database
cp .env.example .env
```

Keep subsequent Docker commands in this `database/` directory. `.env` is ignored
by Git; `.env.example` contains only a placeholder and is safe to commit.

## 5. Set a strong SQL Server password

Edit `.env` and replace the `MSSQL_SA_PASSWORD` placeholder with your own unique,
strong password. Use at least 16 characters and a mix of uppercase letters,
lowercase letters, numbers and symbols. SQL Server requires at least 8 characters
and at least three of those four character categories; an invalid password can
prevent startup. See [Microsoft's password requirements](https://learn.microsoft.com/sql/linux/install-upgrade/quickstart-install-docker).

Keep the value single-quoted so characters such as `$` remain literal; see
[Docker's environment file syntax](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/#env-file-syntax).
If your password contains a single quote, follow Docker's escaping rules.
An exported shell variable of the same name overrides the value in `.env`.
Keep real passwords out of committed files, logs and screenshots.

The only required variable is `MSSQL_SA_PASSWORD`, used for the `sa` login.
`ACCEPT_EULA=Y` and `MSSQL_PID=Developer` are already set in `compose.yaml`.
This configuration is for local development.

## 6. Start SQL Server

Optionally validate the configuration without printing the resolved password:

```bash
docker compose config --quiet
```

Start the SQL Server 2022 container:

```bash
docker compose up -d
```

The service is `sqlserver` and the container is `smartfarm-sqlserver`. The
configuration binds host `127.0.0.1:14333` to container port `1433` and mounts the
named volume `smartfarm_sql_data` at `/var/opt/mssql` for persistent storage.
Compose normally prefixes the volume name with its project name. If the
container name or port is already in use, check the existing setup before
starting another instance; do not remove an existing database container or volume.

## 7. Verify the container is running

```bash
docker compose ps
docker compose logs --tail=60 sqlserver
```

Confirm the service is running and wait for the log message
`SQL Server is now ready for client connections`. A running container alone does
not mean SQL Server is ready. If it exits, inspect the logs for password,
architecture or resource errors.

## 8. Connect with DBeaver

Install [DBeaver Community](https://dbeaver.io/download/) and create a new
**Microsoft SQL Server** connection. Allow DBeaver to download its driver if asked.

| Setting | Local value |
| --- | --- |
| Host | `127.0.0.1` |
| Port | `14333` |
| Database | `master` initially |
| Authentication | SQL Server authentication |
| Username | `sa` |
| Password | Your local `MSSQL_SA_PASSWORD` value |
| Trust Server Certificate | Enabled for this local development connection |

Keep encryption enabled and trust the local container's certificate using
**Trust Server Certificate** (driver property `trustServerCertificate=true`).
Click **Test Connection**, then **Finish**. See the
[DBeaver SQL Server connection guide](https://dbeaver.com/docs/dbeaver/Database-driver-Microsoft-SQL-Server/).

## 9. Create SmartFarmDB if it does not exist

In DBeaver, open `01_CreateDatabase.sql` using the connection to `master`.
If `SmartFarmDB` is absent, execute the whole file with **SQL Editor > Execute
SQL Script**. It checks for the database before creating it. Refresh the database
list afterward. If the database already exists, leave it intact and check
whether it is empty before proceeding.

## 10. Create the schema only in a new, empty SmartFarmDB

Select **SmartFarmDB** as the SQL editor's active database, then open
`02_CreateSchema.sql`. Confirm the connection and active database before running
the file. Execute it once, as a complete script, using **SQL Editor > Execute
SQL Script**; do not execute only the statement at the cursor. See
[DBeaver's script execution instructions](https://dbeaver.com/docs/dbeaver/SQL-Execution/).

**Run this script only against a new, empty `SmartFarmDB`.** It checks the database
name and refuses to proceed if `dbo` tables already exist. It creates the schema
inside a transaction. Do not run it against a populated or partially initialized
database, remove tables to bypass its checks, or rerun it on an established
database. These scripts do not seed user accounts or farm data.

## 11. Verify the schema

With **SmartFarmDB** still selected, execute all of `03_VerifySchema.sql`.
Confirm:

- `ConnectedDatabase` is `SmartFarmDB`.
- `TotalTables` is **23**: 7 Identity tables and 16 business tables.
- The table list includes `AspNetUsers`, `AspNetRoles`, `AspNetUserClaims`,
  `AspNetRoleClaims`, `AspNetUserLogins`, `AspNetUserRoles` and `AspNetUserTokens`,
  plus `Farm`, `FarmMember`, `Plot`, `CropType`, `CropCycle`, `FarmTask`,
  `CropActivity`, `CropObservation`, `ObservationPhoto`, `InventoryItem`,
  `InventoryTransaction`, `Harvest`, `WeatherForecast`, `WeatherAdvisory`,
  `Notification` and `SyncOperation`.
- The final result lists the foreign keys and their child/parent tables.

The verification script lists tables and foreign keys; it does not compare every
column, index or constraint. Keep the original schema script unchanged as the
reference for those definitions.

## 12. Stop and restart without deleting data

From `database/`, stop the container and later start the same container:

```bash
docker compose stop
docker compose start
```

Use `docker compose restart` to restart immediately. The named volume retains
database data across these operations. `docker compose down` removes the
container and network while retaining the named volume; `docker compose up -d`
recreates the container using that volume. Keep the same Compose project name
and directory so Compose reuses the same volume.

**Do not use `docker compose down -v`, delete the named volume, or prune it if you
want to retain the database.** Volume deletion permanently removes its data.
See [Microsoft's persistence documentation](https://learn.microsoft.com/en-us/sql/linux/sql-server-linux-docker-container-configure).

Changing `.env` does not change the `sa` password already stored in an existing
volume. For an existing database, continue using its current password until it
is deliberately changed in SQL Server.

## Reconcile the manual schema with EF Core before an initial migration

This schema was created manually with SQL and does not establish an EF Core
`__EFMigrationsHistory` baseline. **Reconcile this schema with EF Core Code First
models, configuration and migrations before running an initial migration.**
A generated `InitialCreate` migration can attempt to create tables that already
exist.

Both developers must agree on a verified migration baseline for existing
databases, or use matching Code First migrations to create separate fresh
databases. Verify every schema definition before baselining migration history.
Do not invent migration history or apply an initial migration blindly to this
manually initialized database.
