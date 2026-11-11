using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace SmartFarm.Infrastructure.Persistence.Migrations;

[DbContext(typeof(SmartFarmDbContext))]
[Migration("20261111031500_CropIssueFollowUps")]
public sealed class CropIssueFollowUps : Migration
{
    protected override void Up(MigrationBuilder migrationBuilder)
    {
        migrationBuilder.AddColumn<byte>(
            name: "IssueCategory", schema: "dbo", table: "CropObservation",
            type: "tinyint", nullable: false, defaultValue: (byte)0);
        migrationBuilder.AddColumn<byte>(
            name: "IssueSeverity", schema: "dbo", table: "CropObservation",
            type: "tinyint", nullable: false, defaultValue: (byte)0);
        migrationBuilder.AddColumn<byte>(
            name: "FollowUpStatus", schema: "dbo", table: "CropObservation",
            type: "tinyint", nullable: false, defaultValue: (byte)0);
        migrationBuilder.AddColumn<Guid>(
            name: "FollowUpTaskId", schema: "dbo", table: "CropObservation",
            type: "uniqueidentifier", nullable: true);
        migrationBuilder.AddColumn<byte[]>(
            name: "RowVersion", schema: "dbo", table: "CropObservation",
            type: "rowversion", rowVersion: true, nullable: false);

        migrationBuilder.AddCheckConstraint(
            name: "CK_CropObservation_Issue", schema: "dbo", table: "CropObservation",
            sql: "(IssueCategory = 0 AND IssueSeverity = 0 AND FollowUpStatus = 0 AND FollowUpTaskId IS NULL) OR " +
                 "(IssueCategory BETWEEN 1 AND 5 AND IssueSeverity BETWEEN 1 AND 3 AND " +
                 "FollowUpStatus BETWEEN 1 AND 2 AND HealthStatus IN (2,3))");
        migrationBuilder.CreateIndex(
            name: "IX_CropObservation_FollowUpTask", schema: "dbo",
            table: "CropObservation", column: "FollowUpTaskId");
        migrationBuilder.AddForeignKey(
            name: "FK_CropObservation_FollowUpTask", schema: "dbo",
            table: "CropObservation", column: "FollowUpTaskId",
            principalSchema: "dbo", principalTable: "FarmTask", principalColumn: "Id",
            onDelete: ReferentialAction.NoAction);
    }

    protected override void Down(MigrationBuilder migrationBuilder)
    {
        // Removing follow-up links and issue assessments would discard farm records.
        throw new NotSupportedException(
            "Crop issue and resolution history must be preserved. Use a reviewed forward migration.");
    }
}
