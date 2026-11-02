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
