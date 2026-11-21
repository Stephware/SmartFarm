# SmartFarm database source snapshot

> Source: Local SmartFarm repository at 850bfc7dab94675d5a70c13a95a9d00c481c1784
> Collected: 2026-10-27
> Published: Unknown

Read-only capture of repository file contents; not proof of runtime behavior.


## Repository file: database/01_CreateDatabase.sql

/* SmartFarm — Step 1: Execute in DBeaver against the master database.
   Creates the database only if it does not exist.
   SQL Server 2022+. Do not run against a database with existing application data.
*/
IF DB_ID(N'SmartFarmDB') IS NULL
BEGIN
    EXEC(N'CREATE DATABASE [SmartFarmDB]');
END;

SELECT name, state_desc FROM sys.databases WHERE name = N'SmartFarmDB';


## Repository file: database/02_CreateSchema.sql

/*
  SmartFarm — Initial SQL Server schema (manual SQL-first initialization)
  Run ONLY on a new, empty SmartFarmDB database in DBeaver.
  Includes 7 ASP.NET Core Identity tables + 17 SmartFarm business tables.

  IMPORTANT FOR EF CORE:
  This SQL does not insert __EFMigrationsHistory entries. Do not later run a
  generated EF Core InitialCreate migration on the same database: it will try
  to create tables that already exist. Either adopt SQL-first / reverse-map
  the schema, or generate an EXACTLY MATCHING EF migration and carefully
  baseline this database before applying later migrations. Never fabricate
  migration history without verifying that its schema matches.

  IDs: GUID / UNIQUEIDENTIFIER (compatible with client-generated UUIDs).
  Conventions: datetime2(3) UTC, date for farm calendar dates,
  decimal(18,3) for recorded quantities, restricted FK deletion.
*/

-- Filtered indexes require these SET options, regardless of the SQL client.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50001, 'Connect to SmartFarmDB before executing 02_CreateSchema.sql.', 1;

-- Refuse to initialize any database that already has user tables, in any schema.
IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)
    THROW 50002, 'This schema script is intended for a fresh database. Existing tables found; no changes made.', 1;

BEGIN TRY
    BEGIN TRANSACTION;

    /* 1–7: ASP.NET CORE IDENTITY (configured as IdentityUser<Guid>/IdentityRole<Guid>) */
    CREATE TABLE dbo.AspNetUsers (
        Id                  UNIQUEIDENTIFIER NOT NULL,
        UserName            NVARCHAR(256) NULL,
        NormalizedUserName  NVARCHAR(256) NULL,
        Email               NVARCHAR(256) NULL,
        NormalizedEmail     NVARCHAR(256) NULL,
        EmailConfirmed      BIT NOT NULL CONSTRAINT DF_AspNetUsers_EmailConfirmed DEFAULT (0),
        PasswordHash        NVARCHAR(MAX) NULL,
        SecurityStamp       NVARCHAR(MAX) NULL,
        ConcurrencyStamp    NVARCHAR(MAX) NULL,
        PhoneNumber         NVARCHAR(MAX) NULL,
        PhoneNumberConfirmed BIT NOT NULL CONSTRAINT DF_AspNetUsers_PhoneNumberConfirmed DEFAULT (0),
        TwoFactorEnabled    BIT NOT NULL CONSTRAINT DF_AspNetUsers_TwoFactorEnabled DEFAULT (0),
        LockoutEnd          DATETIMEOFFSET(7) NULL,
        LockoutEnabled      BIT NOT NULL CONSTRAINT DF_AspNetUsers_LockoutEnabled DEFAULT (0),
        AccessFailedCount   INT NOT NULL CONSTRAINT DF_AspNetUsers_AccessFailedCount DEFAULT (0),
        FullName            NVARCHAR(120) NOT NULL,
        DisabledAtUtc       DATETIME2(3) NULL,
        CONSTRAINT PK_AspNetUsers PRIMARY KEY (Id)
    );

    CREATE TABLE dbo.AspNetRoles (
        Id UNIQUEIDENTIFIER NOT NULL,
        Name NVARCHAR(256) NULL,
        NormalizedName NVARCHAR(256) NULL,
        ConcurrencyStamp NVARCHAR(MAX) NULL,
        CONSTRAINT PK_AspNetRoles PRIMARY KEY (Id)
    );

    CREATE TABLE dbo.AspNetUserClaims (
        Id INT IDENTITY(1,1) NOT NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        ClaimType NVARCHAR(MAX) NULL,
        ClaimValue NVARCHAR(MAX) NULL,
        CONSTRAINT PK_AspNetUserClaims PRIMARY KEY (Id),
        CONSTRAINT FK_AspNetUserClaims_AspNetUsers_UserId FOREIGN KEY (UserId)
            REFERENCES dbo.AspNetUsers(Id) ON DELETE CASCADE
    );

    CREATE TABLE dbo.AspNetRoleClaims (
        Id INT IDENTITY(1,1) NOT NULL,
        RoleId UNIQUEIDENTIFIER NOT NULL,
        ClaimType NVARCHAR(MAX) NULL,
        ClaimValue NVARCHAR(MAX) NULL,
        CONSTRAINT PK_AspNetRoleClaims PRIMARY KEY (Id),
        CONSTRAINT FK_AspNetRoleClaims_AspNetRoles_RoleId FOREIGN KEY (RoleId)
            REFERENCES dbo.AspNetRoles(Id) ON DELETE CASCADE
    );

    CREATE TABLE dbo.AspNetUserLogins (
        LoginProvider NVARCHAR(128) NOT NULL,
        ProviderKey NVARCHAR(128) NOT NULL,
        ProviderDisplayName NVARCHAR(MAX) NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        CONSTRAINT PK_AspNetUserLogins PRIMARY KEY (LoginProvider, ProviderKey),
        CONSTRAINT FK_AspNetUserLogins_AspNetUsers_UserId FOREIGN KEY (UserId)
            REFERENCES dbo.AspNetUsers(Id) ON DELETE CASCADE
    );

    CREATE TABLE dbo.AspNetUserRoles (
        UserId UNIQUEIDENTIFIER NOT NULL,
        RoleId UNIQUEIDENTIFIER NOT NULL,
        CONSTRAINT PK_AspNetUserRoles PRIMARY KEY (UserId, RoleId),
        CONSTRAINT FK_AspNetUserRoles_AspNetUsers_UserId FOREIGN KEY (UserId)
            REFERENCES dbo.AspNetUsers(Id) ON DELETE CASCADE,
        CONSTRAINT FK_AspNetUserRoles_AspNetRoles_RoleId FOREIGN KEY (RoleId)
            REFERENCES dbo.AspNetRoles(Id) ON DELETE CASCADE
    );

    CREATE TABLE dbo.AspNetUserTokens (
        UserId UNIQUEIDENTIFIER NOT NULL,
        LoginProvider NVARCHAR(128) NOT NULL,
        Name NVARCHAR(128) NOT NULL,
        Value NVARCHAR(MAX) NULL,
        CONSTRAINT PK_AspNetUserTokens PRIMARY KEY (UserId, LoginProvider, Name),
        CONSTRAINT FK_AspNetUserTokens_AspNetUsers_UserId FOREIGN KEY (UserId)
            REFERENCES dbo.AspNetUsers(Id) ON DELETE CASCADE
    );

    /* 1: FARM */
    CREATE TABLE dbo.Farm (
        Id UNIQUEIDENTIFIER NOT NULL,
        OwnerUserId UNIQUEIDENTIFIER NOT NULL,
        JoinCode NVARCHAR(12) NOT NULL,
        Name NVARCHAR(120) NOT NULL,
        LocationText NVARCHAR(250) NOT NULL,
        Latitude DECIMAL(9,6) NULL,
        Longitude DECIMAL(9,6) NULL,
        TimeZoneId NVARCHAR(64) NOT NULL CONSTRAINT DF_Farm_TimeZone DEFAULT (N'Asia/Manila'),
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_Farm_CreatedAt DEFAULT (SYSUTCDATETIME()),
        ArchivedAtUtc DATETIME2(3) NULL,
        RowVersion ROWVERSION NOT NULL,
        CONSTRAINT PK_Farm PRIMARY KEY (Id),
        CONSTRAINT FK_Farm_OwnerUser FOREIGN KEY (OwnerUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_Farm_Coordinates CHECK (
            (Latitude IS NULL AND Longitude IS NULL) OR
            (Latitude IS NOT NULL AND Longitude IS NOT NULL
             AND Latitude BETWEEN -90 AND 90 AND Longitude BETWEEN -180 AND 180)
        )
    );

    CREATE TABLE dbo.FarmJoinRequest (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        Status NVARCHAR(8) NOT NULL CONSTRAINT DF_FarmJoinRequest_Status DEFAULT (N'Pending'),
        RequestedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_FarmJoinRequest_RequestedAt DEFAULT (SYSUTCDATETIME()),
        ReviewedByUserId UNIQUEIDENTIFIER NULL,
        ReviewedAtUtc DATETIME2(3) NULL,
        CONSTRAINT PK_FarmJoinRequest PRIMARY KEY (Id),
        CONSTRAINT FK_FarmJoinRequest_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmJoinRequest_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmJoinRequest_Reviewer FOREIGN KEY (ReviewedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_FarmJoinRequest_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected')),
        CONSTRAINT CK_FarmJoinRequest_Review CHECK (
            (Status = N'Pending' AND ReviewedByUserId IS NULL AND ReviewedAtUtc IS NULL)
            OR (Status IN (N'Approved', N'Rejected') AND ReviewedByUserId IS NOT NULL AND ReviewedAtUtc IS NOT NULL)
        )
    );

    /* 2: FARM MEMBERS (owner is identified through Farm.OwnerUserId) */
    CREATE TABLE dbo.FarmMember (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        Role TINYINT NOT NULL, -- 1 Manager, 2 FieldWorker
        JoinedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_FarmMember_JoinedAt DEFAULT (SYSUTCDATETIME()),
        RemovedAtUtc DATETIME2(3) NULL,
        CONSTRAINT PK_FarmMember PRIMARY KEY (Id),
        CONSTRAINT FK_FarmMember_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmMember_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_FarmMember_Role CHECK (Role IN (1,2))
    );

    /* 3: PLOT */
    CREATE TABLE dbo.Plot (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        Name NVARCHAR(100) NOT NULL,
        AreaSqMeters DECIMAL(12,2) NOT NULL,
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_Plot_CreatedAt DEFAULT (SYSUTCDATETIME()),
        ArchivedAtUtc DATETIME2(3) NULL,
        RowVersion ROWVERSION NOT NULL,
        CONSTRAINT PK_Plot PRIMARY KEY (Id),
        CONSTRAINT FK_Plot_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_Plot_Area CHECK (AreaSqMeters > 0)
    );

    /* 4: CROP TYPE */
    CREATE TABLE dbo.CropType (
        Id UNIQUEIDENTIFIER NOT NULL,
        Name NVARCHAR(100) NOT NULL,
        DefaultGrowthDays SMALLINT NULL,
        DefaultWateringIntervalDays SMALLINT NULL,
        DefaultHarvestUnit NVARCHAR(20) NOT NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_CropType_IsActive DEFAULT (1),
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_CropType_CreatedAt DEFAULT (SYSUTCDATETIME()),
        CONSTRAINT PK_CropType PRIMARY KEY (Id),
        CONSTRAINT CK_CropType_GrowthDays CHECK (DefaultGrowthDays IS NULL OR DefaultGrowthDays > 0),
        CONSTRAINT CK_CropType_WateringDays CHECK (DefaultWateringIntervalDays IS NULL OR DefaultWateringIntervalDays > 0)
    );

    /* 5: CROP CYCLE */
    CREATE TABLE dbo.CropCycle (
        Id UNIQUEIDENTIFIER NOT NULL,
        PlotId UNIQUEIDENTIFIER NOT NULL,
        CropTypeId UNIQUEIDENTIFIER NOT NULL,
        PlannedPlantingDate DATE NOT NULL,
        PlantedDate DATE NULL,
        ExpectedHarvestDate DATE NULL,
        HarvestUnit NVARCHAR(20) NOT NULL,
        WateringIntervalDays SMALLINT NULL,
        Status TINYINT NOT NULL CONSTRAINT DF_CropCycle_Status DEFAULT (0),
        CompletedAtUtc DATETIME2(3) NULL,
        TerminationReason NVARCHAR(300) NULL,
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_CropCycle_CreatedAt DEFAULT (SYSUTCDATETIME()),
        RowVersion ROWVERSION NOT NULL,
        CONSTRAINT PK_CropCycle PRIMARY KEY (Id),
        CONSTRAINT FK_CropCycle_Plot FOREIGN KEY (PlotId) REFERENCES dbo.Plot(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_CropCycle_CropType FOREIGN KEY (CropTypeId) REFERENCES dbo.CropType(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_CropCycle_Status CHECK (Status IN (0,1,2,3,4)), -- Planned, Active, Completed, Failed, Cancelled
        CONSTRAINT CK_CropCycle_Watering CHECK (WateringIntervalDays IS NULL OR WateringIntervalDays > 0),
        CONSTRAINT CK_CropCycle_ActivePlanting CHECK (Status NOT IN (1,2,3) OR PlantedDate IS NOT NULL),
        CONSTRAINT CK_CropCycle_CompletedTime CHECK (Status <> 2 OR CompletedAtUtc IS NOT NULL),
        CONSTRAINT CK_CropCycle_HarvestDate CHECK (ExpectedHarvestDate IS NULL OR PlantedDate IS NULL OR ExpectedHarvestDate >= PlantedDate)
    );

    /* 6: FARM TASK (created before activities, due to optional reference) */
    CREATE TABLE dbo.FarmTask (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        CropCycleId UNIQUEIDENTIFIER NULL,
        AssignedToUserId UNIQUEIDENTIFIER NULL,
        TaskType TINYINT NOT NULL, -- 1 Planting, 2 Watering, 3 Fertilizing, 4 Inspection, 5 Harvest, 6 Other
        DueDate DATE NOT NULL,
        Status TINYINT NOT NULL CONSTRAINT DF_FarmTask_Status DEFAULT (0), -- Pending, Completed, Skipped, Cancelled
        Description NVARCHAR(500) NULL,
        DedupeKey NVARCHAR(160) NULL,
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_FarmTask_CreatedAt DEFAULT (SYSUTCDATETIME()),
        ClosedAtUtc DATETIME2(3) NULL,
        ClosedByUserId UNIQUEIDENTIFIER NULL,
        RowVersion ROWVERSION NOT NULL,
        CONSTRAINT PK_FarmTask PRIMARY KEY (Id),
        CONSTRAINT FK_FarmTask_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmTask_CropCycle FOREIGN KEY (CropCycleId) REFERENCES dbo.CropCycle(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmTask_AssignedUser FOREIGN KEY (AssignedToUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmTask_ClosedUser FOREIGN KEY (ClosedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_FarmTask_Type CHECK (TaskType BETWEEN 1 AND 6),
        CONSTRAINT CK_FarmTask_Status CHECK (Status BETWEEN 0 AND 3),
        CONSTRAINT CK_FarmTask_ClosedAt CHECK (Status = 0 OR ClosedAtUtc IS NOT NULL)
    );

    /* 7: CROP ACTIVITY */
    CREATE TABLE dbo.CropActivity (
        Id UNIQUEIDENTIFIER NOT NULL,
        CropCycleId UNIQUEIDENTIFIER NOT NULL,
        FarmTaskId UNIQUEIDENTIFIER NULL,
        ActivityType TINYINT NOT NULL, -- 1 Watering, 2 Fertilizing, 3 Pesticide, 4 Weeding, 5 Other
        OccurredAtUtc DATETIME2(3) NOT NULL,
        RecordedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_CropActivity_RecordedAt DEFAULT (SYSUTCDATETIME()),
        RecordedByUserId UNIQUEIDENTIFIER NOT NULL,
        Notes NVARCHAR(1000) NULL,
        CONSTRAINT PK_CropActivity PRIMARY KEY (Id),
        CONSTRAINT FK_CropActivity_CropCycle FOREIGN KEY (CropCycleId) REFERENCES dbo.CropCycle(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_CropActivity_FarmTask FOREIGN KEY (FarmTaskId) REFERENCES dbo.FarmTask(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_CropActivity_User FOREIGN KEY (RecordedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_CropActivity_Type CHECK (ActivityType BETWEEN 1 AND 5)
    );

    /* 8: CROP OBSERVATION */
    CREATE TABLE dbo.CropObservation (
        Id UNIQUEIDENTIFIER NOT NULL,
        CropCycleId UNIQUEIDENTIFIER NOT NULL,
        HealthStatus TINYINT NOT NULL, -- 0 NotAssessed, 1 Healthy, 2 NeedsAttention, 3 Critical
        GrowthStage NVARCHAR(40) NULL,
        ObservedAtUtc DATETIME2(3) NOT NULL,
        RecordedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_CropObservation_RecordedAt DEFAULT (SYSUTCDATETIME()),
        RecordedByUserId UNIQUEIDENTIFIER NOT NULL,
        Notes NVARCHAR(1000) NULL,
        CONSTRAINT PK_CropObservation PRIMARY KEY (Id),
        CONSTRAINT FK_CropObservation_CropCycle FOREIGN KEY (CropCycleId) REFERENCES dbo.CropCycle(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_CropObservation_User FOREIGN KEY (RecordedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_CropObservation_Health CHECK (HealthStatus BETWEEN 0 AND 3)
    );

    /* 9: OBSERVATION PHOTO */
    CREATE TABLE dbo.ObservationPhoto (
        Id UNIQUEIDENTIFIER NOT NULL,
        CropObservationId UNIQUEIDENTIFIER NOT NULL,
        StorageKey NVARCHAR(450) NOT NULL,
        ContentType NVARCHAR(100) NOT NULL,
        FileSizeBytes BIGINT NOT NULL,
        UploadedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_ObservationPhoto_UploadedAt DEFAULT (SYSUTCDATETIME()),
        UploadedByUserId UNIQUEIDENTIFIER NOT NULL,
        CONSTRAINT PK_ObservationPhoto PRIMARY KEY (Id),
        CONSTRAINT FK_ObservationPhoto_Observation FOREIGN KEY (CropObservationId) REFERENCES dbo.CropObservation(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_ObservationPhoto_User FOREIGN KEY (UploadedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_ObservationPhoto_Size CHECK (FileSizeBytes > 0)
    );

    /* 10: INVENTORY ITEM */
    CREATE TABLE dbo.InventoryItem (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        Name NVARCHAR(120) NOT NULL,
        Category TINYINT NOT NULL, -- 1 Seeds, 2 Fertilizers, 3 Pesticides, 4 Other
        Unit NVARCHAR(20) NOT NULL,
        Description NVARCHAR(500) NULL,
        QuantityOnHand DECIMAL(18,3) NOT NULL CONSTRAINT DF_InventoryItem_Quantity DEFAULT (0),
        ReorderLevel DECIMAL(18,3) NULL,
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_InventoryItem_CreatedAt DEFAULT (SYSUTCDATETIME()),
        ArchivedAtUtc DATETIME2(3) NULL,
        MetadataVersion UNIQUEIDENTIFIER NOT NULL CONSTRAINT DF_InventoryItem_MetadataVersion DEFAULT (NEWID()),
        RowVersion ROWVERSION NOT NULL,
        CONSTRAINT PK_InventoryItem PRIMARY KEY (Id),
        CONSTRAINT FK_InventoryItem_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_InventoryItem_Category CHECK (Category BETWEEN 1 AND 4),
        CONSTRAINT CK_InventoryItem_Stock CHECK (QuantityOnHand >= 0),
        CONSTRAINT CK_InventoryItem_Reorder CHECK (ReorderLevel IS NULL OR ReorderLevel >= 0)
    );

    /* 11: INVENTORY TRANSACTION */
    CREATE TABLE dbo.InventoryTransaction (
        Id UNIQUEIDENTIFIER NOT NULL,
        InventoryItemId UNIQUEIDENTIFIER NOT NULL,
        CropActivityId UNIQUEIDENTIFIER NULL,
        ReversesTransactionId UNIQUEIDENTIFIER NULL,
        TransactionType TINYINT NOT NULL, -- 1 Opening, 2 Receipt, 3 Consumption, 4 Adjustment, 5 Reversal
        QuantityChange DECIMAL(18,3) NOT NULL,
        OccurredAtUtc DATETIME2(3) NOT NULL,
        RecordedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_InventoryTransaction_RecordedAt DEFAULT (SYSUTCDATETIME()),
        RecordedByUserId UNIQUEIDENTIFIER NOT NULL,
        Reason NVARCHAR(300) NULL,
        CONSTRAINT PK_InventoryTransaction PRIMARY KEY (Id),
        CONSTRAINT FK_InventoryTransaction_Item FOREIGN KEY (InventoryItemId) REFERENCES dbo.InventoryItem(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_InventoryTransaction_Activity FOREIGN KEY (CropActivityId) REFERENCES dbo.CropActivity(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_InventoryTransaction_Reverses FOREIGN KEY (ReversesTransactionId) REFERENCES dbo.InventoryTransaction(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_InventoryTransaction_User FOREIGN KEY (RecordedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_InventoryTransaction_Quantity CHECK (QuantityChange <> 0),
        CONSTRAINT CK_InventoryTransaction_Type CHECK (TransactionType BETWEEN 1 AND 5),
        CONSTRAINT CK_InventoryTransaction_Sign CHECK (
             (TransactionType IN (1,2) AND QuantityChange > 0)
          OR (TransactionType = 3 AND QuantityChange < 0)
          OR (TransactionType IN (4,5))
        ),
        CONSTRAINT CK_InventoryTransaction_ConsumptionActivity CHECK (TransactionType <> 3 OR CropActivityId IS NOT NULL),
        CONSTRAINT CK_InventoryTransaction_Reversal CHECK (
             (TransactionType = 5 AND ReversesTransactionId IS NOT NULL)
          OR (TransactionType <> 5 AND ReversesTransactionId IS NULL)
        )
    );

    /* 12: HARVEST (multiple per crop cycle; cycle completion is a separate online operation) */
    CREATE TABLE dbo.Harvest (
        Id UNIQUEIDENTIFIER NOT NULL,
        CropCycleId UNIQUEIDENTIFIER NOT NULL,
        HarvestedAtUtc DATETIME2(3) NOT NULL,
        Quantity DECIMAL(18,3) NOT NULL,
        Unit NVARCHAR(20) NOT NULL,
        RecordedByUserId UNIQUEIDENTIFIER NOT NULL,
        RecordedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_Harvest_RecordedAt DEFAULT (SYSUTCDATETIME()),
        Notes NVARCHAR(1000) NULL,
        VoidedAtUtc DATETIME2(3) NULL,
        VoidedByUserId UNIQUEIDENTIFIER NULL,
        VoidReason NVARCHAR(300) NULL,
        CONSTRAINT PK_Harvest PRIMARY KEY (Id),
        CONSTRAINT FK_Harvest_CropCycle FOREIGN KEY (CropCycleId) REFERENCES dbo.CropCycle(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_Harvest_RecordedBy FOREIGN KEY (RecordedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_Harvest_VoidedBy FOREIGN KEY (VoidedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_Harvest_Quantity CHECK (Quantity > 0),
        CONSTRAINT CK_Harvest_Void CHECK (
          (VoidedAtUtc IS NULL AND VoidedByUserId IS NULL AND VoidReason IS NULL)
          OR (VoidedAtUtc IS NOT NULL AND VoidedByUserId IS NOT NULL AND VoidReason IS NOT NULL)
        )
    );

    /* 13: WEATHER FORECAST */
    CREATE TABLE dbo.WeatherForecast (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        ForecastDate DATE NOT NULL,
        RetrievedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_WeatherForecast_RetrievedAt DEFAULT (SYSUTCDATETIME()),
        MinTemperatureC DECIMAL(5,2) NULL,
        MaxTemperatureC DECIMAL(5,2) NULL,
        RainfallMm DECIMAL(8,2) NULL,
        MaxPrecipitationProbability DECIMAL(5,2) NULL,
        MaxWindKph DECIMAL(7,2) NULL,
        Provider NVARCHAR(32) NOT NULL,
        CONSTRAINT PK_WeatherForecast PRIMARY KEY (Id),
        CONSTRAINT FK_WeatherForecast_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_WeatherForecast_Temperature CHECK (
          MinTemperatureC IS NULL OR MaxTemperatureC IS NULL OR MinTemperatureC <= MaxTemperatureC
        ),
        CONSTRAINT CK_WeatherForecast_Rain CHECK (RainfallMm IS NULL OR RainfallMm >= 0),
        CONSTRAINT CK_WeatherForecast_Probability CHECK (
          MaxPrecipitationProbability IS NULL OR MaxPrecipitationProbability BETWEEN 0 AND 100
        ),
        CONSTRAINT CK_WeatherForecast_Wind CHECK (MaxWindKph IS NULL OR MaxWindKph >= 0)
    );

    /* 14: WEATHER ADVISORY */
    CREATE TABLE dbo.WeatherAdvisory (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        WeatherForecastId UNIQUEIDENTIFIER NULL,
        AdvisoryDate DATE NOT NULL,
        AdvisoryType TINYINT NOT NULL, -- 1 Rain, 2 Wind, 3 Heat, 4 Other
        Severity TINYINT NOT NULL, -- 1 Info, 2 Warning, 3 Critical
        Status TINYINT NOT NULL CONSTRAINT DF_WeatherAdvisory_Status DEFAULT (0), -- 0 Active, 1 Resolved
        Message NVARCHAR(500) NOT NULL,
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_WeatherAdvisory_CreatedAt DEFAULT (SYSUTCDATETIME()),
        ResolvedAtUtc DATETIME2(3) NULL,
        CONSTRAINT PK_WeatherAdvisory PRIMARY KEY (Id),
        CONSTRAINT FK_WeatherAdvisory_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_WeatherAdvisory_Forecast FOREIGN KEY (WeatherForecastId) REFERENCES dbo.WeatherForecast(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_WeatherAdvisory_Type CHECK (AdvisoryType BETWEEN 1 AND 4),
        CONSTRAINT CK_WeatherAdvisory_Severity CHECK (Severity BETWEEN 1 AND 3),
        CONSTRAINT CK_WeatherAdvisory_Status CHECK (Status IN (0,1)),
        CONSTRAINT CK_WeatherAdvisory_Resolved CHECK (Status <> 1 OR ResolvedAtUtc IS NOT NULL)
    );

    /* 15: NOTIFICATION */
    CREATE TABLE dbo.Notification (
        Id UNIQUEIDENTIFIER NOT NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        FarmTaskId UNIQUEIDENTIFIER NULL,
        WeatherAdvisoryId UNIQUEIDENTIFIER NULL,
        Type TINYINT NOT NULL, -- 1 Task, 2 Weather, 3 System
        DeduplicationKey NVARCHAR(180) NOT NULL,
        Title NVARCHAR(150) NOT NULL,
        Message NVARCHAR(500) NOT NULL,
        CreatedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_Notification_CreatedAt DEFAULT (SYSUTCDATETIME()),
        ReadAtUtc DATETIME2(3) NULL,
        CONSTRAINT PK_Notification PRIMARY KEY (Id),
        CONSTRAINT FK_Notification_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_Notification_Task FOREIGN KEY (FarmTaskId) REFERENCES dbo.FarmTask(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_Notification_Advisory FOREIGN KEY (WeatherAdvisoryId) REFERENCES dbo.WeatherAdvisory(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_Notification_Type CHECK (Type IN (1,2,3))
    );

    /* 16: SYNC OPERATION (server-side idempotency ledger) */
    CREATE TABLE dbo.SyncOperation (
        OperationId UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        DeviceId UNIQUEIDENTIFIER NULL,
        OperationType NVARCHAR(50) NOT NULL,
        TargetEntityId UNIQUEIDENTIFIER NULL,
        RequestHash VARBINARY(32) NOT NULL,
        Status TINYINT NOT NULL, -- 1 Completed, 2 Rejected
        ResultJson NVARCHAR(MAX) NULL,
        ErrorCode NVARCHAR(60) NULL,
        ReceivedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_SyncOperation_ReceivedAt DEFAULT (SYSUTCDATETIME()),
        CompletedAtUtc DATETIME2(3) NULL,
        CONSTRAINT PK_SyncOperation PRIMARY KEY (OperationId),
        CONSTRAINT FK_SyncOperation_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_SyncOperation_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_SyncOperation_Status CHECK (Status IN (1,2))
    );

    /* IDENTITY INDEXES */
    CREATE UNIQUE INDEX RoleNameIndex ON dbo.AspNetRoles(NormalizedName) WHERE NormalizedName IS NOT NULL;
    CREATE INDEX IX_AspNetRoleClaims_RoleId ON dbo.AspNetRoleClaims(RoleId);
    CREATE INDEX IX_AspNetUserClaims_UserId ON dbo.AspNetUserClaims(UserId);
    CREATE INDEX IX_AspNetUserLogins_UserId ON dbo.AspNetUserLogins(UserId);
    CREATE INDEX IX_AspNetUserRoles_RoleId ON dbo.AspNetUserRoles(RoleId);
    CREATE INDEX EmailIndex ON dbo.AspNetUsers(NormalizedEmail);
    CREATE UNIQUE INDEX UserNameIndex ON dbo.AspNetUsers(NormalizedUserName) WHERE NormalizedUserName IS NOT NULL;

    /* BUSINESS UNIQUE INDEXES */
    CREATE UNIQUE INDEX UX_Farm_JoinCode ON dbo.Farm(JoinCode);
    CREATE UNIQUE INDEX UX_FarmJoinRequest_Pending ON dbo.FarmJoinRequest(FarmId, UserId) WHERE Status = N'Pending';
    CREATE UNIQUE INDEX UX_FarmMember_Active ON dbo.FarmMember(FarmId, UserId) WHERE RemovedAtUtc IS NULL;
    CREATE UNIQUE INDEX UX_Plot_Farm_Name_Active ON dbo.Plot(FarmId, Name) WHERE ArchivedAtUtc IS NULL;
    CREATE UNIQUE INDEX UX_CropType_Name ON dbo.CropType(Name);
    CREATE UNIQUE INDEX UX_CropCycle_OneActivePerPlot ON dbo.CropCycle(PlotId) WHERE Status = 1;
    CREATE UNIQUE INDEX UX_FarmTask_Dedupe ON dbo.FarmTask(FarmId, DedupeKey) WHERE DedupeKey IS NOT NULL;
    CREATE UNIQUE INDEX UX_Notification_User_Dedupe ON dbo.Notification(UserId, DeduplicationKey);
    CREATE UNIQUE INDEX UX_WeatherAdvisory_Daily ON dbo.WeatherAdvisory(FarmId, AdvisoryType, AdvisoryDate);
    CREATE UNIQUE INDEX UX_InventoryTransaction_Reverses ON dbo.InventoryTransaction(ReversesTransactionId) WHERE ReversesTransactionId IS NOT NULL;

    /* LOOKUP & FOREIGN KEY INDEXES */
    CREATE INDEX IX_FarmJoinRequest_User ON dbo.FarmJoinRequest(UserId);
    CREATE INDEX IX_Farm_Owner ON dbo.Farm(OwnerUserId);
    CREATE INDEX IX_FarmMember_User ON dbo.FarmMember(UserId);
    CREATE INDEX IX_Plot_Farm ON dbo.Plot(FarmId);
    CREATE INDEX IX_CropCycle_CropType ON dbo.CropCycle(CropTypeId);
    CREATE INDEX IX_CropCycle_Plot_Status ON dbo.CropCycle(PlotId, Status);
    CREATE INDEX IX_CropActivity_Cycle_Time ON dbo.CropActivity(CropCycleId, OccurredAtUtc);
    CREATE INDEX IX_CropActivity_Task ON dbo.CropActivity(FarmTaskId) WHERE FarmTaskId IS NOT NULL;
    CREATE INDEX IX_CropActivity_User ON dbo.CropActivity(RecordedByUserId);
    CREATE INDEX IX_CropObservation_Cycle_Time ON dbo.CropObservation(CropCycleId, ObservedAtUtc);
    CREATE INDEX IX_CropObservation_User ON dbo.CropObservation(RecordedByUserId);
    CREATE INDEX IX_ObservationPhoto_Observation ON dbo.ObservationPhoto(CropObservationId);
    CREATE INDEX IX_ObservationPhoto_User ON dbo.ObservationPhoto(UploadedByUserId);
    CREATE INDEX IX_InventoryItem_Farm_Archived ON dbo.InventoryItem(FarmId, ArchivedAtUtc);
    CREATE INDEX IX_InventoryTransaction_Item_Time ON dbo.InventoryTransaction(InventoryItemId, OccurredAtUtc);
    CREATE INDEX IX_InventoryTransaction_Activity ON dbo.InventoryTransaction(CropActivityId) WHERE CropActivityId IS NOT NULL;
    CREATE INDEX IX_InventoryTransaction_User ON dbo.InventoryTransaction(RecordedByUserId);
    CREATE INDEX IX_Harvest_Cycle_Time ON dbo.Harvest(CropCycleId, HarvestedAtUtc);
    CREATE INDEX IX_Harvest_RecordedBy ON dbo.Harvest(RecordedByUserId);
    CREATE INDEX IX_Harvest_VoidedBy ON dbo.Harvest(VoidedByUserId) WHERE VoidedByUserId IS NOT NULL;
    CREATE INDEX IX_FarmTask_Farm_Status_Due ON dbo.FarmTask(FarmId, Status, DueDate);
    CREATE INDEX IX_FarmTask_CropCycle ON dbo.FarmTask(CropCycleId) WHERE CropCycleId IS NOT NULL;
    CREATE INDEX IX_FarmTask_Assigned ON dbo.FarmTask(AssignedToUserId) WHERE AssignedToUserId IS NOT NULL;
    CREATE INDEX IX_FarmTask_ClosedBy ON dbo.FarmTask(ClosedByUserId) WHERE ClosedByUserId IS NOT NULL;
    CREATE INDEX IX_WeatherForecast_Farm_Date_Retrieved ON dbo.WeatherForecast(FarmId, ForecastDate, RetrievedAtUtc);
    CREATE INDEX IX_WeatherAdvisory_Forecast ON dbo.WeatherAdvisory(WeatherForecastId) WHERE WeatherForecastId IS NOT NULL;
    CREATE INDEX IX_Notification_User_Unread ON dbo.Notification(UserId, ReadAtUtc, CreatedAtUtc);
    CREATE INDEX IX_Notification_Task ON dbo.Notification(FarmTaskId) WHERE FarmTaskId IS NOT NULL;
    CREATE INDEX IX_Notification_Advisory ON dbo.Notification(WeatherAdvisoryId) WHERE WeatherAdvisoryId IS NOT NULL;
    CREATE INDEX IX_SyncOperation_Farm_Received ON dbo.SyncOperation(FarmId, ReceivedAtUtc);
    CREATE INDEX IX_SyncOperation_User ON dbo.SyncOperation(UserId);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

SELECT N'SmartFarm schema created' AS Message,
       COUNT(*) AS TableCount
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo');
-- Expected: 24 tables (7 Identity + 17 SmartFarm).


## Repository file: database/03_VerifySchema.sql

/* Read-only SmartFarmDB verification. Optional EF baseline history is ignored. */
SET NOCOUNT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50010, 'Select SmartFarmDB before checking its schema.', 1;

-- Check all SmartFarm and Identity objects without editing existing data.
IF EXISTS (
    SELECT 1 FROM sys.tables
    WHERE is_ms_shipped = 0 AND schema_id <> SCHEMA_ID(N'dbo')
)
    THROW 50011, 'Unexpected user tables outside dbo.', 1;

IF (SELECT COUNT(*) FROM sys.tables
    WHERE schema_id = SCHEMA_ID(N'dbo') AND name <> N'__EFMigrationsHistory') <> 24
    THROW 50012, 'Expected 24 SmartFarm and Identity tables.', 1;

IF (SELECT COUNT(*) FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND i.is_primary_key = 0 AND i.name IS NOT NULL) <> 48
    THROW 50013, 'Expected 48 application indexes.', 1;

IF (SELECT COUNT(*) FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 42
    THROW 50014, 'Expected 42 foreign keys.', 1;

IF (SELECT COUNT(*) FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory') <> 38
    THROW 50015, 'Expected 38 check constraints.', 1;

IF (SELECT COUNT(*) FROM sys.columns AS c
    JOIN sys.tables AS t ON t.object_id = c.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo') AND t.name <> N'__EFMigrationsHistory'
        AND c.system_type_id = TYPE_ID(N'timestamp')) <> 5
    THROW 50016, 'Expected five SQL rowversion columns.', 1;

-- Disabled or untrusted constraints can look present while not protecting data.
IF EXISTS (
    SELECT 1 FROM sys.foreign_keys AS fk
    JOIN sys.tables AS t ON t.object_id = fk.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND (fk.is_disabled = 1 OR fk.is_not_trusted = 1)
)
    THROW 50018, 'Foreign key checks are disabled or untrusted.', 1;

IF EXISTS (
    SELECT 1 FROM sys.check_constraints AS ck
    JOIN sys.tables AS t ON t.object_id = ck.parent_object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND (ck.is_disabled = 1 OR ck.is_not_trusted = 1)
)
    THROW 50019, 'Check constraints are disabled or untrusted.', 1;

IF EXISTS (
    SELECT 1 FROM sys.indexes AS i
    JOIN sys.tables AS t ON t.object_id = i.object_id
    WHERE t.schema_id = SCHEMA_ID(N'dbo')
        AND i.name IS NOT NULL AND i.is_disabled = 1
)
    THROW 50020, 'An application index is disabled.', 1;

-- These unique indexes protect important business rules and retry safety.
IF EXISTS (
    SELECT 1
    FROM (VALUES
        (N'Farm', N'UX_Farm_JoinCode', 0),
        (N'FarmJoinRequest', N'UX_FarmJoinRequest_Pending', 1),
        (N'FarmMember', N'UX_FarmMember_Active', 1),
        (N'CropCycle', N'UX_CropCycle_OneActivePerPlot', 1),
        (N'FarmTask', N'UX_FarmTask_Dedupe', 1),
        (N'InventoryTransaction', N'UX_InventoryTransaction_Reverses', 1),
        (N'Notification', N'UX_Notification_User_Dedupe', 0),
        (N'WeatherAdvisory', N'UX_WeatherAdvisory_Daily', 0)
    ) AS expected(TableName, IndexName, MustBeFiltered)
    LEFT JOIN sys.tables AS t ON t.schema_id = SCHEMA_ID(N'dbo')
        AND t.name = expected.TableName
    LEFT JOIN sys.indexes AS i ON i.object_id = t.object_id
        AND i.name = expected.IndexName
    WHERE i.index_id IS NULL OR i.is_unique <> 1
        OR (expected.MustBeFiltered = 1 AND i.has_filter <> 1)
)
    THROW 50017, 'A required unique business index is missing or incorrectly configured.', 1;

SELECT N'PASS' AS SchemaCheck, DB_NAME() AS ConnectedDatabase,
       24 AS AppAndIdentityTables, 48 AS AppIndexes,
       42 AS ForeignKeys, 38 AS CheckConstraints, 5 AS RowVersionColumns;

SELECT name AS TableName
FROM sys.tables
WHERE schema_id = SCHEMA_ID(N'dbo')
ORDER BY name;

SELECT fk.name AS ForeignKeyName,
       OBJECT_NAME(fk.parent_object_id) AS ChildTable,
       OBJECT_NAME(fk.referenced_object_id) AS ParentTable
FROM sys.foreign_keys AS fk
WHERE OBJECT_SCHEMA_NAME(fk.parent_object_id) = N'dbo'
ORDER BY ChildTable, ForeignKeyName;


## Repository file: database/04_FarmerRegistrationApproval.sql

/* Non-destructive SQL-first upgrade for populated SmartFarmDB; safe to rerun.
   Obtain a backup and review before the database owner applies this script.
   Does not change existing accounts, membership, ownership, or EF history. */
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF DB_NAME() <> N'SmartFarmDB'
    THROW 50030, 'Select SmartFarmDB before applying the enrollment upgrade.', 1;
IF OBJECT_ID(N'dbo.Farm', N'U') IS NULL OR OBJECT_ID(N'dbo.AspNetUsers', N'U') IS NULL
    THROW 50031, 'The existing SQL-first schema is required before this upgrade.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    -- Keep farm creation and simultaneous upgrade runs outside the backfill.
    DECLARE @FarmCount BIGINT;
    SELECT @FarmCount = COUNT_BIG(*) FROM dbo.Farm WITH (TABLOCKX, HOLDLOCK);
    IF COL_LENGTH(N'dbo.Farm', N'JoinCode') IS NULL
        ALTER TABLE dbo.Farm ADD JoinCode NVARCHAR(12) NULL;

    -- Compile after ADD COLUMN so the same batch works on the original schema.
    EXEC sys.sp_executesql N'
        DECLARE @FarmId UNIQUEIDENTIFIER, @Code NVARCHAR(12), @Attempts INT;
        WHILE EXISTS (SELECT 1 FROM dbo.Farm WHERE JoinCode IS NULL)
        BEGIN
            SELECT TOP (1) @FarmId = Id FROM dbo.Farm WHERE JoinCode IS NULL;
            SET @Attempts = 0;
            WHILE 1 = 1
            BEGIN
                SET @Code = CONVERT(VARCHAR(12), CRYPT_GEN_RANDOM(6), 2);
                IF NOT EXISTS (SELECT 1 FROM dbo.Farm WHERE JoinCode = @Code) BREAK;
                SET @Attempts += 1;
                IF @Attempts >= 100 THROW 50032, ''Unable to allocate a unique farm code; retry the upgrade.'', 1;
            END;
            UPDATE dbo.Farm SET JoinCode = @Code WHERE Id = @FarmId;
        END;
        ALTER TABLE dbo.Farm ALTER COLUMN JoinCode NVARCHAR(12) NOT NULL;
        IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N''dbo.Farm'') AND name = N''UX_Farm_JoinCode'')
            CREATE UNIQUE INDEX UX_Farm_JoinCode ON dbo.Farm(JoinCode);';

    IF OBJECT_ID(N'dbo.FarmJoinRequest', N'U') IS NULL
    BEGIN
    CREATE TABLE dbo.FarmJoinRequest (
        Id UNIQUEIDENTIFIER NOT NULL,
        FarmId UNIQUEIDENTIFIER NOT NULL,
        UserId UNIQUEIDENTIFIER NOT NULL,
        Status NVARCHAR(8) NOT NULL CONSTRAINT DF_FarmJoinRequest_Status DEFAULT (N'Pending'),
        RequestedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_FarmJoinRequest_RequestedAt DEFAULT (SYSUTCDATETIME()),
        ReviewedByUserId UNIQUEIDENTIFIER NULL,
        ReviewedAtUtc DATETIME2(3) NULL,
        CONSTRAINT PK_FarmJoinRequest PRIMARY KEY (Id),
        CONSTRAINT FK_FarmJoinRequest_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmJoinRequest_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT FK_FarmJoinRequest_Reviewer FOREIGN KEY (ReviewedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
        CONSTRAINT CK_FarmJoinRequest_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected')),
        CONSTRAINT CK_FarmJoinRequest_Review CHECK (
            (Status = N'Pending' AND ReviewedByUserId IS NULL AND ReviewedAtUtc IS NULL)
            OR (Status IN (N'Approved', N'Rejected') AND ReviewedByUserId IS NOT NULL AND ReviewedAtUtc IS NOT NULL)
        )
    );

    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.FarmJoinRequest') AND name = N'UX_FarmJoinRequest_Pending')
        CREATE UNIQUE INDEX UX_FarmJoinRequest_Pending ON dbo.FarmJoinRequest(FarmId, UserId) WHERE Status = N'Pending';
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.FarmJoinRequest') AND name = N'IX_FarmJoinRequest_User')
        CREATE INDEX IX_FarmJoinRequest_User ON dbo.FarmJoinRequest(UserId);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;


## Repository file: database/README.md

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


## Repository file: database/compose.yaml

# Developer-only SQL Server; test on an x86-64 host first.
# Apple Silicon (ARM64) with x86 emulation is not officially supported.
services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2022-latest
    platform: linux/amd64
    container_name: smartfarm-sqlserver
    restart: unless-stopped
    environment:
      ACCEPT_EULA: "Y"
      MSSQL_PID: "Developer"
      MSSQL_SA_PASSWORD: ${MSSQL_SA_PASSWORD:?Set MSSQL_SA_PASSWORD in your local .env file}
    ports:
      - "127.0.0.1:14333:1433"
    volumes:
      - smartfarm_sql_data:/var/opt/mssql
volumes:
  smartfarm_sql_data:


## Repository file: database/verification/README.md

# Read-only schema verification

`verify_schema.py` compares the reviewed SQL-first contract with EF Core's
`GenerateCreateScript()` output, a live SQL Server metadata export, or both.
It uses Python 3's standard library, reads local files, and never connects to
the database or executes SQL. Keep generated SQL and catalog exports outside
the repository.

From the repository root:

```sh
python3 database/verification/verify_schema.py \
  --source database/02_CreateSchema.sql \
  --generated /path/to/ef-generated.sql \
  --live-schema /path/to/live-schema.json

python3 -m unittest discover -s database/verification -v
```

Supply at least one of `--generated` and `--live-schema`. To obtain the live
metadata file, run `read_catalog.sql` through your existing SQL connection to
`SmartFarmDB` and export its single JSON result as UTF-8. This query reads
system catalogs only; it does not read application records or credentials.
The account must be able to see the complete database schema. Truncated or
incomplete metadata is unsuitable for baseline approval.

The report lists differences by object. Exit codes are `0` for matching
contracts, `1` for schema differences, and `2` for invalid input or unsupported
syntax. A successful report for the current contract has 24 tables, 201
columns, 24 primary keys, 42 foreign keys, 38 checks, and 48 secondary indexes.

Comparison includes column types, lengths, precision/scale, nullability,
identity seed/increment, named defaults, named keys and foreign keys, deletion
rules, unique indexes, key order, filters, and check predicates. Live metadata
also verifies trusted/enabled constraints, index storage flags, absence of
triggers, and column collations inherited from the database default.

Predicate comparison preserves parentheses and operator precedence. It
normalizes SQL Server's `IN` and `BETWEEN` expansions, including string states and associative ordering
of `AND`/`OR`. It never compares checks by deleting parentheses. Supported
normalizations also include `rowversion`/`timestamp`, an omitted `ON DELETE
NO ACTION`, and exact constant casts such as `CAST(1 AS bit)` for a bit column.
Physical column order is ignored because it does not affect EF's explicit
column mappings; primary/index key order remains significant.

This is a deliberately scoped parser for the SmartFarm CREATE contract,
rather than a general SQL interpreter. Unsupported schema definitions,
column clauses, default functions, or predicates fail verification. It rejects
additional CREATE object kinds, ALTER/DROP scripts, non-dbo objects, included
or descending index keys, computed columns, temporal tables, and memory
optimized tables. Extend the parser and its regression tests before adopting
such schema features. SQL batch/transaction wrappers are not executed or
semantically interpreted.

Matching files are evidence for model review; they do not authorize database
changes or migration-history adoption. Re-export current metadata and obtain
explicit approval before applying any migration or writing migration history.


## Repository file: src/SmartFarm.Infrastructure/Persistence/SmartFarmDbContext.cs

using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Conventions;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence;

public sealed class SmartFarmDbContext(DbContextOptions<SmartFarmDbContext> options)
    : IdentityDbContext<SmartFarmUser, SmartFarmRole, Guid>(options)
{
    public DbSet<Farm> Farms => Set<Farm>();
    public DbSet<FarmJoinRequest> FarmJoinRequests => Set<FarmJoinRequest>();
    public DbSet<FarmMember> FarmMembers => Set<FarmMember>();
    public DbSet<Plot> Plots => Set<Plot>();
    public DbSet<CropType> CropTypes => Set<CropType>();
    public DbSet<CropCycle> CropCycles => Set<CropCycle>();
    public DbSet<FarmTask> FarmTasks => Set<FarmTask>();
    public DbSet<CropActivity> CropActivities => Set<CropActivity>();
    public DbSet<CropObservation> CropObservations => Set<CropObservation>();
    public DbSet<ObservationPhoto> ObservationPhotos => Set<ObservationPhoto>();
    public DbSet<InventoryItem> InventoryItems => Set<InventoryItem>();
    public DbSet<InventoryTransaction> InventoryTransactions => Set<InventoryTransaction>();
    public DbSet<Harvest> Harvests => Set<Harvest>();
    public DbSet<WeatherForecast> WeatherForecasts => Set<WeatherForecast>();
    public DbSet<WeatherAdvisory> WeatherAdvisories => Set<WeatherAdvisory>();
    public DbSet<Notification> Notifications => Set<Notification>();
    public DbSet<SyncOperation> SyncOperations => Set<SyncOperation>();

    // The existing seven-table Identity schema predates passkeys (schema v3).
    protected override Version SchemaVersion => IdentitySchemaVersions.Version1;

    protected override void ConfigureConventions(ModelConfigurationBuilder configurationBuilder)
    {
        base.ConfigureConventions(configurationBuilder);
        // SQL-first schema defines exactly which FK indexes exist.
        configurationBuilder.Conventions.Remove(typeof(ForeignKeyIndexConvention));
    }

    protected override void OnModelCreating(ModelBuilder builder)
    {
        base.OnModelCreating(builder);
        builder.HasDefaultSchema("dbo");
        builder.ApplyConfigurationsFromAssembly(typeof(SmartFarmDbContext).Assembly);
    }
}


## Repository file: src/SmartFarm.Infrastructure/Persistence/Migrations/20261017105514_ExistingSchemaBaseline.cs

using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFarm.Infrastructure.Persistence.Migrations
{
    /// <summary>
    /// Adoption marker for the verified, manually initialized SQL-first schema.
    /// Never creates or removes existing tables. History adoption requires review.
    /// </summary>
    public partial class ExistingSchemaBaseline : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // Existing database only: the SQL schema is managed by 02_CreateSchema.sql.
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            // Removing an adoption marker must not remove existing schema or data.
        }
    }
}


## Repository file: src/SmartFarm.Infrastructure/Persistence/Migrations/20261027022450_FarmerRegistrationApproval.cs

using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFarm.Infrastructure.Persistence.Migrations
{
    public partial class FarmerRegistrationApproval : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql("""
                /* Non-destructive SQL-first upgrade for populated SmartFarmDB; safe to rerun.
                   Obtain a backup and review before the database owner applies this script.
                   Does not change existing accounts, membership, ownership, or EF history. */
                SET ANSI_NULLS ON;
                SET ANSI_PADDING ON;
                SET ANSI_WARNINGS ON;
                SET ARITHABORT ON;
                SET CONCAT_NULL_YIELDS_NULL ON;
                SET QUOTED_IDENTIFIER ON;
                SET NUMERIC_ROUNDABORT OFF;
                SET NOCOUNT ON;
                SET XACT_ABORT ON;

                IF DB_NAME() <> N'SmartFarmDB'
                    THROW 50030, 'Select SmartFarmDB before applying the enrollment upgrade.', 1;
                IF OBJECT_ID(N'dbo.Farm', N'U') IS NULL OR OBJECT_ID(N'dbo.AspNetUsers', N'U') IS NULL
                    THROW 50031, 'The existing SQL-first schema is required before this upgrade.', 1;

                BEGIN TRY
                    BEGIN TRANSACTION;
                    -- Keep farm creation and simultaneous upgrade runs outside the backfill.
                    DECLARE @FarmCount BIGINT;
                    SELECT @FarmCount = COUNT_BIG(*) FROM dbo.Farm WITH (TABLOCKX, HOLDLOCK);
                    IF COL_LENGTH(N'dbo.Farm', N'JoinCode') IS NULL
                        ALTER TABLE dbo.Farm ADD JoinCode NVARCHAR(12) NULL;

                    -- Compile after ADD COLUMN so the same batch works on the original schema.
                    EXEC sys.sp_executesql N'
                        DECLARE @FarmId UNIQUEIDENTIFIER, @Code NVARCHAR(12), @Attempts INT;
                        WHILE EXISTS (SELECT 1 FROM dbo.Farm WHERE JoinCode IS NULL)
                        BEGIN
                            SELECT TOP (1) @FarmId = Id FROM dbo.Farm WHERE JoinCode IS NULL;
                            SET @Attempts = 0;
                            WHILE 1 = 1
                            BEGIN
                                SET @Code = CONVERT(VARCHAR(12), CRYPT_GEN_RANDOM(6), 2);
                                IF NOT EXISTS (SELECT 1 FROM dbo.Farm WHERE JoinCode = @Code) BREAK;
                                SET @Attempts += 1;
                                IF @Attempts >= 100 THROW 50032, ''Unable to allocate a unique farm code; retry the upgrade.'', 1;
                            END;
                            UPDATE dbo.Farm SET JoinCode = @Code WHERE Id = @FarmId;
                        END;
                        ALTER TABLE dbo.Farm ALTER COLUMN JoinCode NVARCHAR(12) NOT NULL;
                        IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N''dbo.Farm'') AND name = N''UX_Farm_JoinCode'')
                            CREATE UNIQUE INDEX UX_Farm_JoinCode ON dbo.Farm(JoinCode);';

                    IF OBJECT_ID(N'dbo.FarmJoinRequest', N'U') IS NULL
                    BEGIN
                    CREATE TABLE dbo.FarmJoinRequest (
                        Id UNIQUEIDENTIFIER NOT NULL,
                        FarmId UNIQUEIDENTIFIER NOT NULL,
                        UserId UNIQUEIDENTIFIER NOT NULL,
                        Status NVARCHAR(8) NOT NULL CONSTRAINT DF_FarmJoinRequest_Status DEFAULT (N'Pending'),
                        RequestedAtUtc DATETIME2(3) NOT NULL CONSTRAINT DF_FarmJoinRequest_RequestedAt DEFAULT (SYSUTCDATETIME()),
                        ReviewedByUserId UNIQUEIDENTIFIER NULL,
                        ReviewedAtUtc DATETIME2(3) NULL,
                        CONSTRAINT PK_FarmJoinRequest PRIMARY KEY (Id),
                        CONSTRAINT FK_FarmJoinRequest_Farm FOREIGN KEY (FarmId) REFERENCES dbo.Farm(Id) ON DELETE NO ACTION,
                        CONSTRAINT FK_FarmJoinRequest_User FOREIGN KEY (UserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
                        CONSTRAINT FK_FarmJoinRequest_Reviewer FOREIGN KEY (ReviewedByUserId) REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
                        CONSTRAINT CK_FarmJoinRequest_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected')),
                        CONSTRAINT CK_FarmJoinRequest_Review CHECK (
                            (Status = N'Pending' AND ReviewedByUserId IS NULL AND ReviewedAtUtc IS NULL)
                            OR (Status IN (N'Approved', N'Rejected') AND ReviewedByUserId IS NOT NULL AND ReviewedAtUtc IS NOT NULL)
                        )
                    );

                    END;
                    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.FarmJoinRequest') AND name = N'UX_FarmJoinRequest_Pending')
                        CREATE UNIQUE INDEX UX_FarmJoinRequest_Pending ON dbo.FarmJoinRequest(FarmId, UserId) WHERE Status = N'Pending';
                    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.FarmJoinRequest') AND name = N'IX_FarmJoinRequest_User')
                        CREATE INDEX IX_FarmJoinRequest_User ON dbo.FarmJoinRequest(UserId);
                    COMMIT TRANSACTION;
                END TRY
                BEGIN CATCH
                    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
                    THROW;
                END CATCH;
                """);
        }

        protected override void Down(MigrationBuilder migrationBuilder) =>
            throw new NotSupportedException("Enrollment history and shared farm codes must be preserved; use a reviewed forward migration.");
    }
}


## Repository file: tests/SmartFarm.SchemaVerification/SchemaModelTests.cs

using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.EntityFrameworkCore.Migrations.Operations;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

// Offline checks: no connection string, database calls, or DDL execution.
public sealed class SchemaModelTests
{
    [Fact]
    public void EnrollmentPersistsUniqueCodesAndPendingRequestsWithDecisionIntegrity()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var model = context.GetService<IDesignTimeModel>().Model;
        var farm = model.FindEntityType(typeof(Farm))!;
        var code = farm.FindProperty("JoinCode");
        Assert.NotNull(code);
        Assert.False(code.IsNullable);
        Assert.Equal(12, code.GetMaxLength());
        Assert.Contains(farm.GetIndexes(), index => index.IsUnique && index.Properties.Count == 1 && index.Properties[0].Name == "JoinCode");
        var request = model.GetEntityTypes().Single(entity => entity.GetTableName() == "FarmJoinRequest");
        Assert.Equal(7, request.GetProperties().Count());
        Assert.Equal(3, request.GetForeignKeys().Count());
        Assert.All(request.GetForeignKeys(), key => Assert.Equal(DeleteBehavior.NoAction, key.DeleteBehavior));
        var pending = request.GetIndexes().Single(index => index.IsUnique);
        Assert.Equal(new[] { "FarmId", "UserId" }, pending.Properties.Select(property => property.Name));
        Assert.Equal("Status = N'Pending'", pending.GetFilter());
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Status");
        Assert.Contains(request.GetCheckConstraints(), constraint => constraint.Name == "CK_FarmJoinRequest_Review");
        var codes = Enumerable.Range(0, 100).Select(_ => new Farm().JoinCode).ToArray();
        Assert.All(codes, value => Assert.Matches("^[0-9A-F]{12}$", value));
        Assert.Equal(codes.Length, codes.Distinct().Count());
    }

    [Fact]
    public void SqlFirstModelContainsOnlyExpectedTablesColumnsAndConstraints()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.GetService<IDesignTimeModel>().Model.GetEntityTypes().ToArray();
        Assert.Equal(24, entities.Length); // Includes seven Identity tables; excludes passkeys.
        Assert.Equal(201, entities.Sum(entity => entity.GetProperties().Count()));
        Assert.Equal(42, entities.Sum(entity => entity.GetForeignKeys().Count()));
        Assert.Equal(48, entities.Sum(entity => entity.GetIndexes().Count()));
        Assert.Equal(38, entities.Sum(entity => entity.GetCheckConstraints().Count()));
        Assert.Equal(30, entities.SelectMany(entity => entity.GetProperties()).Count(property =>
            property.GetDefaultValueSql() is not null || property.TryGetDefaultValue(out _)));
        Assert.All(entities, entity => Assert.Equal("dbo", entity.GetSchema()));
    }

    [Fact]
    public void GuidKeysAreClientAssignedAndSqlRowVersionsProvideConcurrency()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var entities = context.Model.GetEntityTypes().ToArray();
        var guidKeys = entities.SelectMany(entity => entity.FindPrimaryKey()!.Properties)
            .Where(property => property.ClrType == typeof(Guid));
        Assert.All(guidKeys, property => Assert.Equal(ValueGenerated.Never, property.ValueGenerated));
        var rowVersions = entities.SelectMany(entity => entity.GetProperties())
            .Where(property => property.Name == "RowVersion").ToArray();
        Assert.Equal(5, rowVersions.Length);
        Assert.All(rowVersions, property =>
        {
            Assert.True(property.IsConcurrencyToken);
            Assert.Equal(ValueGenerated.OnAddOrUpdate, property.ValueGenerated);
        });
        Assert.False(context.Model.FindEntityType(typeof(InventoryItem))!
            .FindProperty(nameof(InventoryItem.MetadataVersion))!.IsConcurrencyToken);
        Assert.NotEqual(Guid.Empty, new Farm().Id);
        Assert.NotEqual(Guid.Empty, new SyncOperation().OperationId);
    }

    [Fact]
    public void ExplicitFalseCropTypeValueDoesNotUseTrueDatabaseDefault()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        var inactiveCrop = new CropType { IsActive = false };
        context.Add(inactiveCrop);
        var entry = context.Entry(inactiveCrop).Property(crop => crop.IsActive);
        Assert.False(entry.IsTemporary);
        Assert.Equal(true, entry.Metadata.Sentinel);
        Assert.NotEqual(entry.CurrentValue, entry.Metadata.Sentinel);
        Assert.True(new CropType().IsActive);
        Assert.Equal("Asia/Manila", new Farm().TimeZoneId);
    }

    [Fact]
    public void AdoptionBaselineContainsNoDdlAndSnapshotHasNoDrift()
    {
        using var context = new SmartFarmDbContextFactory().CreateDbContext([]);
        Assert.False(context.Database.HasPendingModelChanges());
        var assembly = context.GetService<IMigrationsAssembly>();
        Assert.Equal(2, assembly.Migrations.Count);
        var migrationType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_ExistingSchemaBaseline", StringComparison.Ordinal)).Value;
        var migration = assembly.CreateMigration(migrationType, context.Database.ProviderName!);
        Assert.Empty(migration.UpOperations);
        Assert.Empty(migration.DownOperations);
        var enrollmentType = assembly.Migrations.Single(entry => entry.Key.EndsWith("_FarmerRegistrationApproval", StringComparison.Ordinal)).Value;
        var enrollment = assembly.CreateMigration(enrollmentType, context.Database.ProviderName!);
        var upgrade = Assert.IsType<SqlOperation>(Assert.Single(enrollment.UpOperations));
        var script = File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Database", "04_FarmerRegistrationApproval.sql"));
        Assert.Equal(script.Trim(), upgrade.Sql.Trim());
        Assert.Throws<NotSupportedException>(() => enrollment.DownOperations);
    }
}
