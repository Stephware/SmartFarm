/* SmartFarm Hangfire storage — run on master with DBeaver or sqlcmd.
   Does not touch SmartFarmDB or any existing jobs database.
   Apply once before starting the Worker. Hangfire installs its own schema
   on the first run only when Hangfire__InitializeSchema=true. */
IF DB_ID(N'SmartFarmJobsDB') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [SmartFarmJobsDB]');
END;

SELECT name, state_desc
FROM sys.databases
WHERE name = N'SmartFarmJobsDB';
