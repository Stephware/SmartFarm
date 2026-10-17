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
