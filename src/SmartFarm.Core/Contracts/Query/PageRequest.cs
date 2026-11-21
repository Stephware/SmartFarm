using System.ComponentModel.DataAnnotations;

namespace SmartFarm.Core.Contracts.Query;

public sealed class PageRequest
{
    [Range(1, 100000)] public int Page { get; set; } = 1;
    [Range(1, 100)] public int PageSize { get; set; } = 25;
    [StringLength(120)] public string? Search { get; set; }
    public DateOnly? From { get; set; }
    public DateOnly? To { get; set; }
    public Guid? CropCycleId { get; set; }
    public Guid? ItemId { get; set; }
    public Guid? AssignedToUserId { get; set; }
    public byte? Type { get; set; }
    public byte? Status { get; set; }
    public string? EnrollmentStatus { get; set; }
    public bool? Unread { get; set; }
    public bool? LowStock { get; set; }
    public bool? Voided { get; set; }
    public string Sort { get; set; } = "desc";
    public void Validate()
    {
        if (Page is < 1 or > 100000 || PageSize is < 1 or > 100 || Search?.Length > 120 ||
            Sort is not ("asc" or "desc") || From > To ||
            EnrollmentStatus is not (null or "Pending" or "Approved" or "Rejected" or "Canceled"))
            throw new ArgumentException("Invalid paging or filter parameters.");
    }
}

public sealed record PageResult<T>(IReadOnlyList<T> Items, int Page, int PageSize, int TotalCount)
{
    public bool HasMore => (long)Page * PageSize < TotalCount;
}
