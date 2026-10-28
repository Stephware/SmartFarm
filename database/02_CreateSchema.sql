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
        CONSTRAINT CK_FarmJoinRequest_Status CHECK (Status IN (N'Pending', N'Approved', N'Rejected', N'Canceled')),
        CONSTRAINT CK_FarmJoinRequest_Review CHECK (
            (Status = N'Pending' AND ReviewedByUserId IS NULL AND ReviewedAtUtc IS NULL)
            OR (Status IN (N'Approved', N'Rejected') AND ReviewedByUserId IS NOT NULL AND ReviewedAtUtc IS NOT NULL)
            OR (Status = N'Canceled' AND ReviewedByUserId = UserId AND ReviewedAtUtc IS NOT NULL)
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
        CONSTRAINT CK_Notification_Type CHECK (Type IN (1,2,3,4))
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
