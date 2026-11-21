using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Metadata;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Persistence;
using Xunit;

namespace SmartFarm.SchemaVerification;

public sealed class SupportManagementSchemaTests
{
    [Fact]
    public void TicketsAndRepliesHaveConstrainedFieldsAndNoCascadeDelete()
    {
        using var db = new SmartFarmDbContextFactory().CreateDbContext([]);
        var model = db.GetService<IDesignTimeModel>().Model;
        var ticket = model.FindEntityType(typeof(SupportTicket))!;
        var message = model.FindEntityType(typeof(SupportTicketMessage))!;
        Assert.Equal("dbo", ticket.GetSchema());
        Assert.Equal("SupportTicket", ticket.GetTableName());
        Assert.Equal("SupportTicketMessage", message.GetTableName());
        Assert.Equal(12, ticket.GetProperties().Count());
        Assert.Equal(6, message.GetProperties().Count());
        Assert.Equal(2, ticket.GetForeignKeys().Count());
        Assert.Equal(2, message.GetForeignKeys().Count());
        Assert.All(ticket.GetForeignKeys().Concat(message.GetForeignKeys()),
            fk => Assert.Equal(DeleteBehavior.NoAction, fk.DeleteBehavior));
        Assert.Equal(2000, ticket.FindProperty(nameof(SupportTicket.Description))!.GetMaxLength());
        Assert.Equal(1200, message.FindProperty(nameof(SupportTicketMessage.Body))!.GetMaxLength());
        Assert.True(ticket.FindProperty(nameof(SupportTicket.RowVersion))!.IsConcurrencyToken);
        Assert.Equal(3, ticket.GetCheckConstraints().Count());
        Assert.Contains(ticket.GetCheckConstraints(),
            check => check.Name == "CK_SupportTicket_Status" && check.Sql.Contains("Resolved"));
    }

    [Fact]
    public void UpgradeCreatesSupportTablesOnlyAndIsSafeToRerun()
    {
        var sql = File.ReadAllText(Path.Combine(AppContext.BaseDirectory,
            "Database", "08_SupportManagement.sql"));
        Assert.Contains("OBJECT_ID(N'dbo.SupportTicket'", sql);
        Assert.Contains("OBJECT_ID(N'dbo.SupportTicketMessage'", sql);
        Assert.Contains("IF NOT EXISTS (SELECT 1 FROM sys.indexes", sql);
        Assert.Contains("BEGIN TRANSACTION", sql);
        Assert.Contains("ROLLBACK TRANSACTION", sql);
        Assert.DoesNotContain("DROP TABLE", sql);
        Assert.DoesNotContain("DELETE FROM", sql);
        Assert.DoesNotContain("INSERT INTO dbo.AspNetUsers", sql);
    }
}
