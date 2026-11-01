/* Read-only Hangfire verification: connect to SmartFarmJobsDB first.
   Run this after the Worker has started and processed the startup check.
   For task/weather execution, run the Worker temporarily with
   Hangfire__RunJobsOnStartup=true, then run this script again. */
IF DB_NAME() <> N'SmartFarmJobsDB'
    THROW 51010, 'Connect to SmartFarmJobsDB, not SmartFarmDB.', 1;

IF OBJECT_ID(N'HangFire.Job', N'U') IS NULL OR
   OBJECT_ID(N'HangFire.Server', N'U') IS NULL OR
   OBJECT_ID(N'HangFire.[Set]', N'U') IS NULL
    THROW 51011, 'Hangfire schema missing. Initialize it before verification.', 1;

-- Three recurring schedules must exist, including task/weather scans.
SELECT [Value] AS RecurringJobId
FROM [HangFire].[Set]
WHERE [Key] = N'recurring-jobs'
  AND [Value] IN (N'smartfarm-worker-check',
                  N'smartfarm-tasks-and-reminders',
                  N'smartfarm-weather-check')
ORDER BY [Value];

-- A running server should have a recent heartbeat.
SELECT [Id], [LastHeartbeat]
FROM [HangFire].[Server]
ORDER BY [LastHeartbeat] DESC;

-- A Succeeded state proves the worker actually executed that job.
SELECT TOP (30) [Id], [CreatedAt], [StateName],
       JSON_VALUE([InvocationData], '$.Type') AS JobType
FROM [HangFire].[Job]
WHERE [InvocationData] LIKE N'%SmartFarm.Worker.Jobs.%'
ORDER BY [Id] DESC;
