using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Core.Contracts.Farms;

public sealed class FarmInput
{
    [Required, StringLength(120, MinimumLength = 2)]
    public string Name { get; set; } = "";

    [Required, StringLength(250, MinimumLength = 2)]
    public string LocationText { get; set; } = "";

    [StringLength(64)]
    public string TimeZoneId { get; set; } = "Asia/Manila";

    public decimal? Latitude { get; set; }
    public decimal? Longitude { get; set; }

    // Required for updates and archiving, not creation.
    public string? RowVersion { get; set; }
}

public sealed class PlotInput
{
    [Required, StringLength(100, MinimumLength = 2)]
    public string Name { get; set; } = "";

    [Range(typeof(decimal), "0.01", "9999999999.99")]
    public decimal AreaSqMeters { get; set; }

    public string? RowVersion { get; set; }
}

public sealed class RowVersionInput
{
    [Required]
    public string RowVersion { get; set; } = "";
}

public sealed class InviteMemberInput
{
    [Required, EmailAddress, StringLength(256)]
    public string Email { get; set; } = "";

    [Range(1, 2)]
    public byte Role { get; set; }
}

public sealed class UpdateMemberInput
{
    [Range(1, 2)]
    public byte Role { get; set; }
}

public sealed class InvitationAcceptance
{
    [Required]
    public string Token { get; set; } = "";
}

public sealed record FarmSummary(
    Guid Id, string Name, string LocationText, string Role, string TimeZoneId);

public sealed record FarmDetails(
    Guid Id, string Name, string LocationText, string TimeZoneId,
    decimal? Latitude, decimal? Longitude, string Role, string RowVersion);

public sealed record PlotDetails(Guid Id, Guid FarmId, string Name,
    decimal AreaSqMeters, string RowVersion);

public sealed record FarmMemberDetails(Guid Id, Guid UserId, string FullName,
    string Email, string Role);

public sealed record InvitationDetails(string Token, DateTimeOffset ExpiresAtUtc,
    string Email, string Role);

