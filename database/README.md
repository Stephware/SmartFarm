# SmartFarm database setup

SmartFarm uses **Microsoft SQL Server**. Each developer runs a separate local `SmartFarmDB` using the **same version-controlled SQL scripts**. Local database records are not shared through Git.

Choose your setup:

- **[macOS: Docker Desktop + DBeaver](README_macOS.md)**
- **[Windows: SQL Server + SSMS or DBeaver](README_Windows.md)** — no Docker required

## Files

| File | Purpose |
| --- | --- |
| `01_CreateDatabase.sql` | Creates `SmartFarmDB` if it does not exist |
| `02_CreateSchema.sql` | Creates 7 ASP.NET Core Identity + 17 SmartFarm business tables in a **fresh, empty** database |
| `03_VerifySchema.sql` | Checks the database and lists tables and foreign keys |
| `04_FarmerRegistrationApproval.sql` | Non-destructive, rerunnable farm-code/request upgrade for existing databases |
| `compose.yaml` | Local SQL Server 2022 container for macOS development |
| `.env.example` | Safe Docker password configuration template |

For a **new database**, execute `01_CreateDatabase.sql`, `02_CreateSchema.sql`, then `03_VerifySchema.sql` **in order** against the same SQL Server instance. Create the database while connected to `master`; switch the SQL editor to `SmartFarmDB` before running the schema and verification scripts. The schema is **not repeatable**: error 50002 means the database already contains dbo tables. Inspect them before doing anything destructive. Error 50001 means the SQL editor is using the wrong database.

**Migration warning:** This is a manually created SQL-first baseline. It does not populate `__EFMigrationsHistory`. Before using EF Core Code First migrations, ensure the EF Core model exactly matches this schema and establish a verified migration baseline. Do **not** run an ordinary generated `InitialCreate` migration against these already existing tables.

Never commit `.env`, passwords, private connection strings, local database data or backups. Coordinate all future schema changes through reviewed scripts or EF Core migrations. Keep the development databases independent; never point one developer's experiments at the other's local instance.

## Upgrade an existing database for farmer approval

Back up the intended database and review `04_FarmerRegistrationApproval.sql`.
The database owner applies it explicitly with the SQL editor connected to
`SmartFarmDB`, then runs `03_VerifySchema.sql`. For example, with an already
configured authenticated SQL client:

```sh
sqlcmd -S <server> -d SmartFarmDB -b -i database/04_FarmerRegistrationApproval.sql
sqlcmd -S <server> -d SmartFarmDB -b -i database/03_VerifySchema.sql
```

Never rerun `02_CreateSchema.sql` against existing data. The upgrade preserves
accounts, ownership and membership, fills only missing codes with cryptographic
random values, adds request history and its constraints, and retains codes on
rerun. It locks the farm table during the backfill, so schedule it before deploying
the updated clients. No upgrade runs at application startup.

The upgrade writes no EF history. If EF migration history has already been
adopted, the corresponding `FarmerRegistrationApproval` migration executes the
same rerunnable SQL; generate and review the migration script before applying
it. An unadopted SQL-first database can continue using the explicit SQL script.
Downgrade is deliberately blocked to preserve shared codes and decision history.
