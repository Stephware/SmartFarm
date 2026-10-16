# SmartFarm database setup — macOS

Uses **Docker Desktop + SQL Server 2022 + DBeaver**. Run commands from the repository's `database/` directory.

## 1. Install prerequisites

Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) and [DBeaver Community](https://dbeaver.io/download/). Launch Docker Desktop.

Open Terminal and run:

```bash
uname -m
docker --version
docker compose version
```

`x86_64` identifies an Intel Mac; `arm64` identifies Apple Silicon. **Microsoft does not support SQL Server's x86-64 Linux container on emulated Apple Silicon.** The supplied Compose configuration asks Docker for `linux/amd64`; it may work through emulation but may also fail or crash. For a supported deployment, use an x86-64 SQL Server host (or connect to one remotely). See [Microsoft SQL Server container guidance](https://learn.microsoft.com/en-us/sql/linux/containers/deploy?view=sql-server-ver16).

## 2. Configure Docker credentials

```bash
cd database
cp .env.example .env
nano .env
```

Replace the password placeholder with a strong password that meets SQL Server requirements. Keep the local `.env` secret and do not commit it. `compose.yaml` uses `MSSQL_SA_PASSWORD` from this file.

## 3. Start SQL Server

```bash
docker compose config --quiet
docker compose up -d
docker compose ps
docker compose logs --tail=60 sqlserver
```

Check that the container is running **and** the logs report SQL Server ready for connections. The host port is `14333`, mapped to container port `1433`. The named volume persists the data.

## 4. Connect using DBeaver

Create a **Microsoft SQL Server** connection and enter:

| Setting | Value |
| --- | --- |
| Host | `127.0.0.1` |
| Port | `14333` |
| Database | `master` initially |
| Authentication | SQL Server authentication |
| User | `sa` |
| Password | Your password in `.env` |
| Trust Server Certificate | Enabled for this local development setup |

Click **Test Connection**.

## 5. Initialize and verify the schema

1. In a SQL editor connected to `master`, execute all of `01_CreateDatabase.sql`.
2. Switch the SQL editor's active database/connection context to **`SmartFarmDB`**. Check with `SELECT DB_NAME() AS CurrentDatabase;`.
3. **Only for a new, empty database**, execute the complete `02_CreateSchema.sql` as a SQL script.
4. Run `03_VerifySchema.sql` while connected to `SmartFarmDB`. Expect **23 dbo tables** (7 Identity + 16 SmartFarm) and the associated foreign keys.

If you get **50001**, you have the wrong database selected. If you get **50002**, dbo tables already exist; verify their contents instead of rerunning the schema or deleting anything.

## 6. Everyday container commands

```bash
docker compose stop           # Stop, preserve data
docker compose start          # Start, preserve data
docker compose ps             # Show container state
docker compose logs sqlserver # Troubleshoot
docker compose down           # Remove container, preserve named volume
```

**Do not run `docker compose down -v`** unless you deliberately intend to delete the persistent development database.

## Important for both developers

The SQL files are shared through GitHub, but each developer has separate records and connection settings. Do not commit `.env`, database backups, or live credentials. The manual SQL schema must be reconciled with EF Core's model and migration history before running a generated initial EF Core migration.

[Back to database setup index](README.md)
