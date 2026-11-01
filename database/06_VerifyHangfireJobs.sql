/* Read-only verification. Connect to SmartFarmJobsDB (never SmartFarmDB).
   Start the Worker. To test all three job types immediately, start it once
   with Hangfire__RunJobsOnStartup=true, then rerun this script. */
IF DB_NAME() <> N'SmartFarmJobsDB'
    THROW 51010, 'Connect to SmartFarmJobsDB, not SmartFarmDB.', 1;

IF OBJECT_ID(N'HangFire.Job', N'U') IS NULL OR
   OBJECT_ID(N'HangFire.Server', N'U') IS NULL OR
   OBJECT_ID(N'HangFire.[Set]', N'U') IS NULL
    THROW 51011, 'Hangfire schema missing. Initialize it before verification.', 1;

-- Every recurring job should show REGISTERED.
SELECT expected.JobId,
       CASE WHEN EXISTS (
           SELECT 1 FROM [HangFire].[Set] scheduled
           WHERE scheduled.[Key] = N'recurring-jobs'
             AND scheduled.[Value] = expected.JobId
       ) THEN 'REGISTERED' ELSE 'MISSING' END AS ScheduleStatus
FROM (VALUES
    (N'smartfarm-worker-check'),
    (N'smartfarm-tasks-and-reminders'),
    (N'smartfarm-weather-check')
) expected(JobId);

-- A running worker should report a recent heartbeat.
SELECT [Id], [LastHeartbeat]
FROM [HangFire].[Server]
ORDER BY [LastHeartbeat] DESC;

-- All three should show SUCCEEDED after the one-time startup test.
SELECT expected.JobType,
       CASE WHEN EXISTS (
           SELECT 1 FROM [HangFire].[Job] executed
           WHERE executed.[StateName] = N'Succeeded'
             AND executed.[InvocationData] LIKE N'%' + expected.JobType + N'%'
       ) THEN 'SUCCEEDED' ELSE 'NOT VERIFIED' END AS ExecutionStatus
FROM (VALUES
    (N'WorkerHeartbeatJob'),
    (N'TaskReminderJob'),
    (N'WeatherCheckJob')
) expected(JobType);

-- Details for unsuccessful or recent jobs.
SELECT TOP (30) [Id], [CreatedAt], [StateName],
       JSON_VALUE([InvocationData], '$.Type') AS JobType
FROM [HangFire].[Job]
WHERE [InvocationData] LIKE N'%SmartFarm.Worker.Jobs.%'
ORDER BY [Id] DESC;
