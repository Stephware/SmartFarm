/* AniGo Phase 3: manual farm cash ledger. Back up SQL Server before applying.
   Safe to rerun. No existing table or inventory record is changed. */
SET NOCOUNT ON;
SET XACT_ABORT ON;
IF DB_NAME() <> N'SmartFarmDB'
    THROW 50060, 'Select SmartFarmDB before applying the farm finance upgrade.', 1;
IF OBJECT_ID(N'dbo.Farm', N'U') IS NULL OR OBJECT_ID(N'dbo.CropCycle', N'U') IS NULL
    THROW 50061, 'Apply the existing SmartFarm schema before this upgrade.', 1;
IF OBJECT_ID(N'dbo.FarmFinanceEntry', N'U') IS NULL
BEGIN
    BEGIN TRANSACTION;
    CREATE TABLE dbo.FarmFinanceEntry (
        Id uniqueidentifier NOT NULL CONSTRAINT PK_FarmFinanceEntry PRIMARY KEY,
        FarmId uniqueidentifier NOT NULL,
        CropCycleId uniqueidentifier NULL,
        RecordedByUserId uniqueidentifier NOT NULL,
        EntryDate date NOT NULL,
        Kind nvarchar(10) NOT NULL,
        Category nvarchar(60) NOT NULL,
        AmountPhp decimal(14,2) NOT NULL,
        Notes nvarchar(500) NULL,
        RecordedAtUtc datetime2(3) NOT NULL CONSTRAINT DF_FarmFinanceEntry_RecordedAt DEFAULT SYSUTCDATETIME(),
        VoidedAtUtc datetime2(3) NULL,
        VoidedByUserId uniqueidentifier NULL,
        VoidReason nvarchar(300) NULL,
        CONSTRAINT CK_FarmFinanceEntry_Kind CHECK (Kind IN (N'Expense', N'Income')),
        CONSTRAINT CK_FarmFinanceEntry_Amount CHECK (AmountPhp > 0),
        CONSTRAINT CK_FarmFinanceEntry_Void CHECK (
            (VoidedAtUtc IS NULL AND VoidedByUserId IS NULL AND VoidReason IS NULL) OR
            (VoidedAtUtc IS NOT NULL AND VoidedByUserId IS NOT NULL AND VoidReason IS NOT NULL)),
        CONSTRAINT FK_FarmFinanceEntry_Farm FOREIGN KEY(FarmId) REFERENCES dbo.Farm(Id),
        CONSTRAINT FK_FarmFinanceEntry_CropCycle FOREIGN KEY(CropCycleId) REFERENCES dbo.CropCycle(Id),
        CONSTRAINT FK_FarmFinanceEntry_RecordedBy FOREIGN KEY(RecordedByUserId) REFERENCES dbo.AspNetUsers(Id),
        CONSTRAINT FK_FarmFinanceEntry_VoidedBy FOREIGN KEY(VoidedByUserId) REFERENCES dbo.AspNetUsers(Id)
    );
    CREATE INDEX IX_FarmFinanceEntry_Farm_Date ON dbo.FarmFinanceEntry(FarmId, EntryDate);
    CREATE INDEX IX_FarmFinanceEntry_Cycle ON dbo.FarmFinanceEntry(CropCycleId);
    CREATE INDEX IX_FarmFinanceEntry_RecordedBy ON dbo.FarmFinanceEntry(RecordedByUserId);
    COMMIT TRANSACTION;
END;
