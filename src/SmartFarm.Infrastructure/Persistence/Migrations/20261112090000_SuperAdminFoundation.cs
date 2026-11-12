using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFarm.Infrastructure.Persistence.Migrations;

[DbContext(typeof(SmartFarmDbContext))]
[Migration("20261112090000_SuperAdminFoundation")]
public sealed class SuperAdminFoundation : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        // Compatible with the SQL-first script and with existing EF installations.
        migrationBuilder.Sql("""
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
            """);
    }

    protected override void Down(MigrationBuilder migrationBuilder) =>
        throw new NotSupportedException(
            "Administrative security audit history must be preserved. Use a reviewed forward migration.");
}
