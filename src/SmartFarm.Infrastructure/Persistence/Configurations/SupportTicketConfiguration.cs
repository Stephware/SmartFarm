using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class SupportTicketConfiguration : IEntityTypeConfiguration<SupportTicket>
{
    public void Configure(EntityTypeBuilder<SupportTicket> builder)
    {
        builder.ToTable("SupportTicket", "dbo", t =>
        {
            t.HasCheckConstraint("CK_SupportTicket_Status", "Status IN (N'Open', N'InProgress', N'Resolved', N'Closed')");
            t.HasCheckConstraint("CK_SupportTicket_Priority", "Priority IN (N'Normal', N'High')");
            t.HasCheckConstraint("CK_SupportTicket_Category", "Category IN (N'Account', N'Farm', N'Mobile', N'Other')");
        });
        builder.HasKey(x => x.Id).HasName("PK_SupportTicket");
        builder.Property(x => x.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(x => x.UserId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.FarmId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.Title).HasColumnType("nvarchar(140)").HasMaxLength(140).IsRequired();
        builder.Property(x => x.Description).HasColumnType("nvarchar(2000)").HasMaxLength(2000).IsRequired();
        builder.Property(x => x.Category).HasColumnType("nvarchar(20)").HasMaxLength(20).IsRequired();
        builder.Property(x => x.Status).HasColumnType("nvarchar(16)").HasMaxLength(16).IsRequired();
        builder.Property(x => x.Priority).HasColumnType("nvarchar(8)").HasMaxLength(8).IsRequired();
        builder.Property(x => x.CreatedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.UpdatedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.ResolvedAtUtc).HasColumnType("datetime2(3)");
        builder.Property(x => x.RowVersion).HasColumnType("rowversion").IsRowVersion();
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(x => x.UserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_SupportTicket_User");
        builder.HasOne<Farm>().WithMany().HasForeignKey(x => x.FarmId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_SupportTicket_Farm");
        builder.HasIndex(x => new { x.Status, x.UpdatedAtUtc }).HasDatabaseName("IX_SupportTicket_Status_Updated");
        builder.HasIndex(x => new { x.UserId, x.CreatedAtUtc }).HasDatabaseName("IX_SupportTicket_User_Created");
        builder.HasIndex(x => x.FarmId).HasDatabaseName("IX_SupportTicket_Farm");
    }
}
