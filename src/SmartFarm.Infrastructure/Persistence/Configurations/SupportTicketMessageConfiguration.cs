using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using SmartFarm.Core.Entities;
using SmartFarm.Infrastructure.Identity;

namespace SmartFarm.Infrastructure.Persistence.Configurations;

internal sealed class SupportTicketMessageConfiguration : IEntityTypeConfiguration<SupportTicketMessage>
{
    public void Configure(EntityTypeBuilder<SupportTicketMessage> builder)
    {
        builder.ToTable("SupportTicketMessage", "dbo");
        builder.HasKey(x => x.Id).HasName("PK_SupportTicketMessage");
        builder.Property(x => x.Id).HasColumnType("uniqueidentifier").ValueGeneratedNever();
        builder.Property(x => x.TicketId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.AuthorUserId).HasColumnType("uniqueidentifier");
        builder.Property(x => x.Body).HasColumnType("nvarchar(1200)").HasMaxLength(1200).IsRequired();
        builder.Property(x => x.IsInternal).HasColumnType("bit");
        builder.Property(x => x.CreatedAtUtc).HasColumnType("datetime2(3)");
        builder.HasOne<SupportTicket>().WithMany().HasForeignKey(x => x.TicketId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_SupportTicketMessage_Ticket");
        builder.HasOne<SmartFarmUser>().WithMany().HasForeignKey(x => x.AuthorUserId)
            .OnDelete(DeleteBehavior.NoAction).HasConstraintName("FK_SupportTicketMessage_Author");
        builder.HasIndex(x => new { x.TicketId, x.CreatedAtUtc }).HasDatabaseName("IX_SupportTicketMessage_Ticket_Created");
    }
}
