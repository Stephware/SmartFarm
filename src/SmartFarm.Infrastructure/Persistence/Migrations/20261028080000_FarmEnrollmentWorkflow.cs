using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFarm.Infrastructure.Persistence.Migrations;

[DbContext(typeof(SmartFarmDbContext))]
[Migration("20261028080000_FarmEnrollmentWorkflow")]
public sealed class FarmEnrollmentWorkflow : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder) => migrationBuilder.Sql("""
        /* Forward-only, rerunnable upgrade. Review and back up the existing database first.
           Expands checks only; preserves accounts, farm codes, memberships and history. */
        SET NOCOUNT ON;
        SET XACT_ABORT ON;
        IF DB_NAME() <> N'SmartFarmDB'
            THROW 50040, 'Select SmartFarmDB before applying the enrollment workflow upgrade.', 1;
        IF OBJECT_ID(N'dbo.FarmJoinRequest', N'U') IS NULL OR OBJECT_ID(N'dbo.Notification', N'U') IS NULL
            THROW 50041, 'Apply the existing enrollment schema before this upgrade.', 1;
        
        BEGIN TRY
            BEGIN TRANSACTION;
            ALTER TABLE dbo.FarmJoinRequest DROP CONSTRAINT CK_FarmJoinRequest_Status;
            ALTER TABLE dbo.FarmJoinRequest DROP CONSTRAINT CK_FarmJoinRequest_Review;
            ALTER TABLE dbo.FarmJoinRequest WITH CHECK ADD CONSTRAINT CK_FarmJoinRequest_Status
                CHECK (Status IN (N'Pending', N'Approved', N'Rejected', N'Canceled'));
            ALTER TABLE dbo.FarmJoinRequest WITH CHECK ADD CONSTRAINT CK_FarmJoinRequest_Review
                CHECK ((Status = N'Pending' AND ReviewedByUserId IS NULL AND ReviewedAtUtc IS NULL) OR (Status IN (N'Approved', N'Rejected') AND ReviewedByUserId IS NOT NULL AND ReviewedAtUtc IS NOT NULL) OR (Status = N'Canceled' AND ReviewedByUserId = UserId AND ReviewedAtUtc IS NOT NULL));
            ALTER TABLE dbo.Notification DROP CONSTRAINT CK_Notification_Type;
            ALTER TABLE dbo.Notification WITH CHECK ADD CONSTRAINT CK_Notification_Type CHECK (Type IN (1,2,3,4));
            COMMIT TRANSACTION;
        END TRY
        BEGIN CATCH
            IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
            THROW;
        END CATCH;
        """);

    protected override void Down(MigrationBuilder migrationBuilder) =>
        throw new NotSupportedException("Cancellation history and enrollment notifications must be preserved; use a reviewed forward migration.");
}
