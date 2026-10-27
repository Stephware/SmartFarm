# SmartFarm database setup — Windows

Uses **locally installed Microsoft SQL Server + SQL Server Management Studio (SSMS) or DBeaver**. **Docker is not required.** Use the same SQL scripts as the macOS developer.

## 1. Install and start SQL Server

1. Install [SQL Server Developer Edition](https://www.microsoft.com/en-us/sql-server/sql-server-downloads) for development/testing, or **SQL Server Express** if its limits suit your machine. Developer Edition is not licensed for production use.
2. Install [SQL Server Management Studio (SSMS)](https://learn.microsoft.com/en-us/ssms/install/install) or [DBeaver](https://dbeaver.io/download/).
3. During SQL Server installation, note the **instance name** and authentication mode. Windows Authentication is easiest for local development when available.
4. Open **SQL Server Configuration Manager** or Windows **Services**, and confirm the **SQL Server (instance name)** service is running. If connecting remotely or through DBeaver over TCP, ensure the relevant TCP/IP connection settings are enabled.

## 2. Connect to the correct instance

In SSMS choose **Database Engine**, or create a **Microsoft SQL Server** connection in DBeaver.

| Installation | Typical server name |
| --- | --- |
| Default SQL Server instance | `localhost` or your computer name |
| SQL Server Express named instance | `.\\SQLEXPRESS` (SSMS server-name syntax) |
| SQL Server LocalDB, if separately installed | `(localdb)\\MSSQLLocalDB` |

These are examples; your actual installed instance name may differ. **LocalDB is a separate local development option, not the SQL Server service**. For DBeaver, a standard SQL Server service reachable over TCP is usually simpler than LocalDB.

Prefer **Windows Authentication** when supported. For SQL Server Authentication, use a login permitted by your SQL Server configuration and **never put its password in Git**. Connect to `master` initially. If connection fails, verify the instance name, running service, authentication, TCP/IP settings and port.

## 3. Create SmartFarmDB

Open `database/01_CreateDatabase.sql` in a new query editor, using the intended SQL Server instance. Execute it while connected to `master`. Refresh the database navigator and confirm `SmartFarmDB` appears.

## 4. Create the schema

1. Select **`SmartFarmDB`** as the active database in the SSMS toolbar or DBeaver SQL editor.
2. Check the connection context with:

```sql
SELECT DB_NAME() AS CurrentDatabase;
```

3. On a **new, empty** `SmartFarmDB`, open `database/02_CreateSchema.sql` and execute the **whole script**, not just a selected statement.
4. Run `database/03_VerifySchema.sql`. Expect **24 dbo tables** (7 ASP.NET Core Identity + 17 SmartFarm tables), plus their foreign keys.

Error **50001** means the SQL editor is not using `SmartFarmDB`. Error **50002** means the database contains dbo tables already—inspect and verify them before considering any change. Do **not** drop or reset an existing database without checking whether it contains important data.

## 5. Connection strings for later ASP.NET Core integration

Use your actual SQL Server instance name (these examples are **templates**, not application configuration to commit).

**Default local SQL Server instance with Windows Authentication:**

```text
Server=localhost;Database=SmartFarmDB;Trusted_Connection=True;TrustServerCertificate=True;
```

**SQL Server Express named instance with Windows Authentication:**

```text
Server=.\\SQLEXPRESS;Database=SmartFarmDB;Trusted_Connection=True;TrustServerCertificate=True;
```

For SQL Server Authentication, replace integrated authentication with your own user ID and password, and store the connection string in **user secrets or environment variables**, not source control. `TrustServerCertificate=True` is meant only for a trusted local development environment; use proper certificate validation when deploying.

## Important for both developers

Your Windows installation and your teammate's macOS Docker container are **separate database instances**. GitHub shares the SQL schema, **not live records**. Both developers should run the same scripts on their own instances.

**EF Core warning:** The SQL scripts manually created the schema and did not populate `__EFMigrationsHistory`. Before adding EF Core Code First migrations, compare the entity mappings to the actual database and establish a verified migration baseline. Do not apply a normal generated `InitialCreate` migration on top of already existing tables.

[Back to database setup index](README.md)
