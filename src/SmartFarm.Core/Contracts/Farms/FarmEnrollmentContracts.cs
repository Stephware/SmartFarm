using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Core.Contracts.Farms;

public sealed record FarmJoinRequestDetails(Guid Id, Guid FarmId, string FarmName,
    Guid UserId, string FullName, string Email, string Status, DateTime RequestedAtUtc,
    DateTime? ReviewedAtUtc);

public sealed record FarmEnrollmentOverview(string JoinCode, IReadOnlyList<FarmJoinRequestDetails> Requests,
    IReadOnlyList<FarmJoinRequestDetails>? History = null);

public sealed class FarmCodeRegenerationInput
{
    [Required, StringLength(12, MinimumLength = 12)]
    public string CurrentCode { get; set; } = "";
}

public sealed class FarmJoinRequestInput
{
    [Required, StringLength(64)]
    public string FarmCode { get; set; } = "";
}

public sealed class FarmJoinDecisionInput
{
    [Required]
    public bool? Approve { get; set; }
}
