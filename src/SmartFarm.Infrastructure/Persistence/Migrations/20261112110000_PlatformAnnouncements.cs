using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFarm.Infrastructure.Persistence.Migrations;

[DbContext(typeof(SmartFarmDbContext))]
[Migration("20261112110000_PlatformAnnouncements")]
public sealed class PlatformAnnouncements : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        // Mirrors database/09_PlatformAnnouncements.sql for existing SQL-first installations.
        migrationBuilder.Sql("""
            IF OBJECT_ID(N'dbo.PlatformAnnouncement', N'U') IS NULL
            BEGIN
                CREATE TABLE dbo.PlatformAnnouncement (
                    Id UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_PlatformAnnouncement PRIMARY KEY,
                    CreatedByUserId UNIQUEIDENTIFIER NOT NULL,
                    Title NVARCHAR(140) NOT NULL,
                    Body NVARCHAR(2000) NOT NULL,
                    Category NVARCHAR(20) NOT NULL,
                    State NVARCHAR(16) NOT NULL,
                    CreatedAtUtc DATETIME2(3) NOT NULL,
                    UpdatedAtUtc DATETIME2(3) NOT NULL,
                    PublishedAtUtc DATETIME2(3) NULL,
                    ExpiresAtUtc DATETIME2(3) NULL,
                    RowVersion ROWVERSION NOT NULL,
                    CONSTRAINT FK_PlatformAnnouncement_Creator FOREIGN KEY (CreatedByUserId)
                        REFERENCES dbo.AspNetUsers(Id) ON DELETE NO ACTION,
                    CONSTRAINT CK_PlatformAnnouncement_State CHECK (State IN (N'Draft', N'Published', N'Archived')),
                    CONSTRAINT CK_PlatformAnnouncement_Category CHECK (Category IN (N'General', N'Maintenance', N'Update')),
                    CONSTRAINT CK_PlatformAnnouncement_Published CHECK (
                        (State = N'Draft' AND PublishedAtUtc IS NULL) OR
                        (State IN (N'Published', N'Archived') AND PublishedAtUtc IS NOT NULL))
                );
            END;
            IF NOT EXISTS (SELECT 1 FROM sys.indexes
                WHERE object_id = OBJECT_ID(N'dbo.PlatformAnnouncement') AND name = N'IX_PlatformAnnouncement_State_Published')
                CREATE INDEX IX_PlatformAnnouncement_State_Published ON dbo.PlatformAnnouncement(State, PublishedAtUtc);
            IF NOT EXISTS (SELECT 1 FROM sys.indexes
                WHERE object_id = OBJECT_ID(N'dbo.PlatformAnnouncement') AND name = N'IX_PlatformAnnouncement_Creator')
                CREATE INDEX IX_PlatformAnnouncement_Creator ON dbo.PlatformAnnouncement(CreatedByUserId);
            """);
    }

    protected override void Down(MigrationBuilder migrationBuilder) =>
        throw new NotSupportedException(
            "Published announcements and audit history must be preserved; use a reviewed forward migration.");
}
