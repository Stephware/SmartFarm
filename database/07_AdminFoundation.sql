/* Add the permanent Super Admin audit table without changing farm records.
   Run against SmartFarmDB after backup; safe to rerun. No roles are seeded. */
SET XACT_ABORT ON;
SET NOCOUNT ON;
IF DB_NAME() <> N'SmartFarmDB'
    THROW 50070, 'Connect to SmartFarmDB before running 07_AdminFoundation.sql.', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.AdminAuditLog', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.AdminAuditLog (
            Id UNIQUEIDENTIFIER NOT NULL,
            ActorUserId UNIQUEIDENTIFIER NULL,
            ActorSource NVARCHAR(40) NOT NULL,
            Action NVARCHAR(100) NOT NULL,
            TargetType NVARCHAR(80) NOT NULL,
            TargetId UNIQUEIDENTIFIER NULL,
            Reason NVARCHAR(300) NULL,
            OccurredAtUtc DATETIME2(3) NOT NULL
                CONSTRAINT DF_AdminAuditLog_OccurredAt DEFAULT (SYSUTCDATETIME()),
            CONSTRAINT PK_AdminAuditLog PRIMARY KEY (Id),
            CONSTRAINT FK_AdminAuditLog_Actor FOREIGN KEY (ActorUserId)
                REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION
        );
    END;
    IF NOT EXISTS (SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.AdminAuditLog') AND name = N'IX_AdminAuditLog_OccurredAt')
        CREATE INDEX IX_AdminAuditLog_OccurredAt ON dbo.AdminAuditLog(OccurredAtUtc);
    IF NOT EXISTS (SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.AdminAuditLog') AND name = N'IX_AdminAuditLog_Actor_Date')
        CREATE INDEX IX_AdminAuditLog_Actor_Date ON dbo.AdminAuditLog(ActorUserId, OccurredAtUtc);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
