using System.Text.RegularExpressions;
using Xunit;

namespace SmartFarm.SchemaVerification;

// These tests only read scripts. They never connect to or change a database.
public sealed class SqlInitializationTests
{
    private static string ReadSql(string filename) =>
        File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "Database", filename));

    [Fact]
    public void FreshSchemaUsesRequiredSqlServerIndexSettings()
    {
        var script = ReadSql("02_CreateSchema.sql");
        var firstTable = script.IndexOf("CREATE TABLE dbo.AspNetUsers", StringComparison.Ordinal);
        Assert.True(firstTable > 0);

        foreach (var setting in new[]
        {
            "SET ANSI_NULLS ON;",
            "SET ANSI_PADDING ON;",
            "SET ANSI_WARNINGS ON;",
            "SET ARITHABORT ON;",
            "SET CONCAT_NULL_YIELDS_NULL ON;",
            "SET QUOTED_IDENTIFIER ON;",
            "SET NUMERIC_ROUNDABORT OFF;"
        })
        {
            var position = script.IndexOf(setting, StringComparison.OrdinalIgnoreCase);
            Assert.True(position >= 0 && position < firstTable,
                $"{setting} must run before the schema is created.");
        }
    }

    [Fact]
    public void SchemaCreationRefusesExistingTablesAndRollsBackOnErrors()
    {
        var creation = ReadSql("01_CreateDatabase.sql");
        var schema = ReadSql("02_CreateSchema.sql");

        Assert.Contains("IF DB_ID(N'SmartFarmDB') IS NULL", creation);
        Assert.Contains("IF DB_NAME() <> N'SmartFarmDB'", schema);
        Assert.Contains("IF EXISTS (SELECT 1 FROM sys.tables WHERE is_ms_shipped = 0)", schema);
        Assert.True(schema.IndexOf("IF EXISTS (SELECT 1 FROM sys.tables",
            StringComparison.Ordinal) < schema.IndexOf("BEGIN TRANSACTION",
            StringComparison.Ordinal));
        Assert.Contains("SET XACT_ABORT ON;", schema);
        Assert.Contains("BEGIN CATCH", schema);
        Assert.Contains("ROLLBACK TRANSACTION", schema);

        Assert.Equal(23, Regex.Matches(schema, @"\bCREATE TABLE dbo\.",
            RegexOptions.IgnoreCase).Count);
        Assert.Equal(45, Regex.Matches(schema, @"\bCREATE (?:UNIQUE )?INDEX\b",
            RegexOptions.IgnoreCase).Count);
    }

    [Fact]
    public void SchemaVerificationChecksStructureWithoutChangingData()
    {
        var script = ReadSql("03_VerifySchema.sql");

        Assert.Contains("IF DB_NAME() <> N'SmartFarmDB'", script);
        Assert.Contains("Expected 23 SmartFarm and Identity tables.", script);
        Assert.Contains("Expected 45 application indexes.", script);
        Assert.Contains("Expected 39 foreign keys.", script);
        Assert.Contains("Expected 36 check constraints.", script);
        Assert.Contains("Expected five SQL rowversion columns.", script);
        Assert.Contains("UX_FarmTask_Dedupe", script);
        Assert.Contains("UX_Notification_User_Dedupe", script);
        Assert.Contains("UX_CropCycle_OneActivePerPlot", script);
        Assert.Contains("THROW 50017", script);
        Assert.False(Regex.IsMatch(script,
            @"(?mi)^\s*(CREATE|ALTER|DROP|TRUNCATE|INSERT|UPDATE|DELETE|MERGE|EXEC)\b"),
            "Schema verification must be read-only.");
    }
}
