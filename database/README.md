# SmartFarm database setup

SmartFarm uses **Microsoft SQL Server**. Each developer runs a separate local `SmartFarmDB` using the **same version-controlled SQL scripts**. Local database records are not shared through Git.

Choose your setup:

- **[macOS: Docker Desktop + DBeaver](README_macOS.md)**
- **[Windows: SQL Server + SSMS or DBeaver](README_Windows.md)** — no Docker required

## Files

| File | Purpose |
| --- | --- |
| `01_CreateDatabase.sql` | Creates `SmartFarmDB` if it does not exist |
| `02_CreateSchema.sql` | Creates 7 ASP.NET Core Identity + 16 SmartFarm business tables in a **fresh, empty** database |
| `03_VerifySchema.sql` | Checks the database and lists tables and foreign keys |
| `compose.yaml` | Local SQL Server 2022 container for macOS development |
| `.env.example` | Safe Docker password configuration template |

Execute the SQL scripts **in order** against the same SQL Server instance. Create the database while connected to `master`; switch the SQL editor to `SmartFarmDB` before running the schema and verification scripts. The schema is **not repeatable**: error 50002 means the database already contains dbo tables. Inspect them before doing anything destructive. Error 50001 means the SQL editor is using the wrong database.

**Migration warning:** This is a manually created SQL-first baseline. It does not populate `__EFMigrationsHistory`. Before using EF Core Code First migrations, ensure the EF Core model exactly matches this schema and establish a verified migration baseline. Do **not** run an ordinary generated `InitialCreate` migration against these already existing tables.

Never commit `.env`, passwords, private connection strings, local database data or backups. Coordinate all future schema changes through reviewed scripts or EF Core migrations. Keep the development databases independent; never point one developer's experiments at the other's local instance.
